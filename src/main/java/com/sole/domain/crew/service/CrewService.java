package com.sole.domain.crew.service;

import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.dto.CrewDetailResponse;
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
                .orElseThrow(() -> new
                        BusinessException(ErrorCode.USER_NOT_FOUND));

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new
                        BusinessException(ErrorCode.REGION_NOT_FOUND));

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

        // 호스트를 자동 참여자로 등록
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
                        .orElseThrow(() -> new
                                BusinessException(ErrorCode.CREW_NOT_FOUND));
        return CrewDetailResponse.from(crew);
    }

    private RunningCrew loadCrewAndValidateHost(Long crewId, Long
            requesterId) {
        RunningCrew crew = runningCrewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));

        if (!crew.getHost().getId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return crew;
    }
}