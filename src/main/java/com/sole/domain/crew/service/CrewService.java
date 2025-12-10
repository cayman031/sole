package com.sole.domain.crew.service;

import com.sole.domain.crew.dto.*;
import com.sole.domain.crew.entity.CrewMember;
import com.sole.domain.crew.entity.CrewRole;
import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.crew.repository.CrewMemberRepository;
import com.sole.domain.crew.repository.RunningCrewRepository;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrewService {

    private final RunningCrewRepository runningCrewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public Long createCrew(Long hostUserId, CrewCreateRequest request) {
        User host = userRepository.findById(hostUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));

        RunningCrew crew = RunningCrew.builder()
                .title(request.title())
                .description(request.description())
                .host(host)
                .region(region)
                .meetingTime(request.meetingTime())
                .place(request.place())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .maxParticipants(request.maxParticipants())
                .level(request.level())
                .build();

        CrewMember hostMember = CrewMember.builder()
                .user(host)
                .role(CrewRole.HOST)
                .joinedAt(LocalDateTime.now())
                .build();
        crew.addMember(hostMember);

        runningCrewRepository.save(crew);
        return crew.getId();
    }

    @Transactional(readOnly = true)
    public CrewDetailResponse getCrewDetail(Long crewId) {
        RunningCrew crew =
                runningCrewRepository.findWithHostRegionMembersById(crewId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        return CrewDetailResponse.from(crew);
    }

    @Transactional(readOnly = true)
    public Page<CrewSummaryResponse> getCrews(CrewSearchCondition condition, Pageable pageable) {
        return runningCrewRepository.search(
                condition.regionId(),
                condition.level(),
                condition.startDateTime(),
                condition.endDateTimeExclusive(),
                pageable
        );
    }

    @Transactional
    public void updateCrew(Long crewId, Long requesterId, CrewUpdateRequest request) {
        RunningCrew crew = loadCrewAndValidateHost(crewId, requesterId);

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));

        crew.update(
                request.title(),
                request.description(),
                region,
                request.meetingTime(),
                request.place(),
                request.latitude(),
                request.longitude(),
                request.maxParticipants(),
                request.level()
        );
    }

    @Transactional
    public void deleteCrew(Long crewId, Long requesterId) {
        RunningCrew crew = loadCrewAndValidateHost(crewId, requesterId);
        runningCrewRepository.delete(crew); //members는 cascade로 함께 삭제
    }

    @Transactional
    public void joinCrew(Long crewId, Long userId) {
        RunningCrew crew = runningCrewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        crewMemberRepository.findByCrewAndUser(crew, user).ifPresent(cm -> {
            throw new BusinessException(ErrorCode.CREW_MEMBER_ALREADY_JOINED);
        });

        int currentCount = crewMemberRepository.countByCrew(crew);
        crew.validateCapacity(currentCount); // 정원 초과 시 예외

        CrewMember member = CrewMember.builder()
                .user(user)
                .role(CrewRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        crew.addMember(member);
        try {
            crewMemberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException ex) {
            throw new
                    BusinessException(ErrorCode.CREW_MEMBER_ALREADY_JOINED);
        }
    }

    @Transactional
    public void leaveCrew(Long crewId, Long userId) {
        RunningCrew crew = runningCrewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CrewMember member = crewMemberRepository.findByCrewAndUser(crew, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_MEMBER_NOT_FOUND));

        // 호스트는 모임을 유지해야 하므로 탈퇴불가 -> 호스트를 넘겨주고 탈퇴하는 방식 고려
        if (member.getRole() ==  CrewRole.HOST) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        crewMemberRepository.delete(member);
        crew.getMembers().remove(member); // 컬렉션 캐시 정합성 유지
    }

    private RunningCrew loadCrewAndValidateHost(Long crewId, Long requesterId) {
        RunningCrew crew = runningCrewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));

        if (!crew.getHost().getId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return crew;
    }
}