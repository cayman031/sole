package com.sole.domain.crew.service;

import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.dto.CrewDetailResponse;
import com.sole.domain.crew.dto.CrewSearchCondition;
import com.sole.domain.crew.dto.CrewSummaryResponse;
import com.sole.domain.crew.dto.CrewUpdateRequest;
import com.sole.domain.crew.dto.NearbyCrewRequest;
import com.sole.domain.crew.dto.NearbyCrewResponse;
import com.sole.domain.crew.entity.CrewMember;
import com.sole.domain.crew.entity.CrewRole;
import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.crew.repository.CrewMemberRepository;
import com.sole.domain.crew.repository.RunningCrewRepository;
import com.sole.domain.crew.repository.projection.CrewSummaryProjection;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import com.sole.global.util.DistanceCalculator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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
                )
                .map(CrewSummaryResponse::from);
    }

    @Transactional
    public void updateCrew(Long crewId, Long requesterId, CrewUpdateRequest request) {
        RunningCrew crew = loadCrewAndValidateHost(crewId, requesterId);

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new
                        BusinessException(ErrorCode.REGION_NOT_FOUND));

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
        crewMemberRepository.saveAndFlush(member); // 중복 레이스는 DB 제약으로 막고 전역 핸들러에서 매핑
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

    @Transactional(readOnly = true)
    public List<NearbyCrewResponse> getNearbyCrews(NearbyCrewRequest request)
    {
        BoundingBox box = calculateBoundingBox(
                request.latitude(),
                request.longitude(),
                request.radiusKm()
        );

        List<CrewSummaryProjection> candidates =
                runningCrewRepository.searchWithinBoundingBox(
                        box.minLat(), box.maxLat(), box.minLng(), box.maxLng(),
                        request.level(),
                        request.startDateTime(),
                        request.endDateTimeExclusive()
                );

        return candidates.stream()
                .map(summary -> {
                    double distanceKm = DistanceCalculator.haversineKm(
                            request.latitude(), request.longitude(),
                            summary.latitude(), summary.longitude()
                    );
                    return NearbyCrewResponse.of(summary, distanceKm);
                })
                .filter(resp -> resp.distanceKm() <= request.radiusKm())
                .sorted(Comparator.comparingDouble(NearbyCrewResponse::distanceKm))
                .collect(Collectors.toList());
    }

    private BoundingBox calculateBoundingBox(double lat, double lng, double radiusKm) {
        double earthRadiusKm = DistanceCalculator.earthRadiusKm();
        double latDelta = Math.toDegrees(radiusKm / earthRadiusKm);
        double lngDelta = Math.toDegrees(radiusKm / earthRadiusKm / Math.cos(Math.toRadians(lat)));

        double minLat = clamp(lat - latDelta, -90, 90);
        double maxLat = clamp(lat + latDelta, -90, 90);
        double minLng = clamp(lng - lngDelta, -180, 180);
        double maxLng = clamp(lng + lngDelta, -180, 180);
        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {}

    private RunningCrew loadCrewAndValidateHost(Long crewId, Long requesterId) {
        RunningCrew crew = runningCrewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));

        if (!crew.getHost().getId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return crew;
    }
}
