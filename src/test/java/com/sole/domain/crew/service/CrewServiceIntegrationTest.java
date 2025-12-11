package com.sole.domain.crew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.dto.NearbyCrewRequest;
import com.sole.domain.crew.dto.NearbyCrewResponse;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import com.sole.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class CrewServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CrewService crewService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RegionRepository regionRepository;

    @Test
    @Transactional
    @DisplayName("MySQL 환경에서 모임 인원 제한을 적용한다")
    void joinCrewCapacityWithMySql() {
        Region region = regionRepository.save(new Region("서울", "관악구"));
        User host = userRepository.save(User.builder()
                .email("host@example.com")
                .password("pw")
                .nickname("호스트")
                .region(region)
                .preferredLevel(PreferredLevel.BEGINNER)
                .build());
        User member = userRepository.save(User.builder()
                .email("member@example.com")
                .password("pw")
                .nickname("참여자")
                .region(region)
                .preferredLevel(PreferredLevel.BEGINNER)
                .build());

        Long crewId = crewService.createCrew(host.getId(), new CrewCreateRequest(
                "정원 1명",
                "호스트만 참여",
                region.getId(),
                LocalDateTime.now().plusDays(1),
                "어딘가",
                1.0,
                2.0,
                1, // 호스트 포함 1명
                PreferredLevel.BEGINNER
        ));

        assertThatThrownBy(() -> crewService.joinCrew(crewId, member.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREW_MEMBER_LIMIT_EXCEEDED);
    }

    @Test
    @Transactional
    @DisplayName("중복 가입 시 제약조건 예외를 비즈니스 예외로 변환한다")
    void duplicateJoinWithMySql() {
        Region region = regionRepository.save(new Region("서울", "노원구"));
        User host = userRepository.save(User.builder()
                .email("host2@example.com")
                .password("pw")
                .nickname("호스트2")
                .region(region)
                .preferredLevel(PreferredLevel.BEGINNER)
                .build());
        User member = userRepository.save(User.builder()
                .email("member2@example.com")
                .password("pw")
                .nickname("참여자2")
                .region(region)
                .preferredLevel(PreferredLevel.BEGINNER)
                .build());

        Long crewId = crewService.createCrew(host.getId(), new CrewCreateRequest(
                "중복 체크",
                "두 명 허용",
                region.getId(),
                LocalDateTime.now().plusDays(1),
                "공원",
                1.0,
                2.0,
                3,
                PreferredLevel.BEGINNER
        ));
        crewService.joinCrew(crewId, member.getId());

        assertThatThrownBy(() -> crewService.joinCrew(crewId, member.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREW_MEMBER_ALREADY_JOINED);
    }

    @Test
    @Transactional
    @DisplayName("반경 내 모임을 거리 오름차순으로 조회한다")
    void getNearbyCrewsSortedByDistance() {
        Region region = regionRepository.save(new Region("서울", "중구"));
        User host = userRepository.save(User.builder()
                .email("host3@example.com")
                .password("pw")
                .nickname("호스트3")
                .region(region)
                .preferredLevel(PreferredLevel.BEGINNER)
                .build());

        double baseLat = 37.5665;
        double baseLng = 126.9780;
        LocalDateTime meeting = LocalDateTime.now().plusDays(2);

        Long veryCloseId = createCrew(host, region, "0.2km", baseLat + 0.002, baseLng, meeting);
        Long closeId = createCrew(host, region, "1km", baseLat + 0.009, baseLng, meeting);
        createCrew(host, region, "5km", baseLat + 0.05, baseLng, meeting);

        NearbyCrewRequest request2km = new NearbyCrewRequest(
                baseLat, baseLng, 2.0, null, null, null
        );
        List<NearbyCrewResponse> within2km = crewService.getNearbyCrews(request2km);

        assertThat(within2km).extracting(NearbyCrewResponse::crewId)
                .containsExactly(veryCloseId, closeId);
        assertThat(within2km)
                .isSortedAccordingTo(Comparator.comparingDouble(NearbyCrewResponse::distanceKm));
        assertThat(within2km.get(within2km.size() - 1).distanceKm()).isLessThanOrEqualTo(2.0);

        NearbyCrewRequest requestHalfKm = new NearbyCrewRequest(
                baseLat, baseLng, 0.3, null, null, null
        );
        List<NearbyCrewResponse> withinHalfKm = crewService.getNearbyCrews(requestHalfKm);
        assertThat(withinHalfKm).extracting(NearbyCrewResponse::crewId)
                .containsExactly(veryCloseId);
    }

    private Long createCrew(User host, Region region, String title,
                            double latitude, double longitude, LocalDateTime meetingTime) {
        return crewService.createCrew(host.getId(), new CrewCreateRequest(
                title,
                "설명",
                region.getId(),
                meetingTime,
                "장소",
                latitude,
                longitude,
                10,
                PreferredLevel.BEGINNER
        ));
    }
}
