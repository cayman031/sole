package com.sole.domain.crew;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.auth.service.AuthService;
import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.dto.CrewDetailResponse;
import com.sole.domain.crew.dto.CrewSearchCondition;
import com.sole.domain.crew.dto.CrewSummaryResponse;
import com.sole.domain.crew.dto.CrewUpdateRequest;
import com.sole.domain.crew.dto.MeetingDateFilter;
import com.sole.domain.crew.entity.CrewRole;
import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.crew.repository.CrewMemberRepository;
import com.sole.domain.crew.repository.RunningCrewRepository;
import com.sole.domain.crew.service.CrewService;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class CrewServiceTest {

    @Autowired
    private CrewService crewService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private RunningCrewRepository runningCrewRepository;

    @Autowired
    private CrewMemberRepository crewMemberRepository;

    @Test
    void createCrew_registers_host_as_member() {
        Region region = regionRepository.save(new Region("서울", "강남구"));
        Long hostId = signUp("host@example.com", "host-user", region, PreferredLevel.INTERMEDIATE);

        CrewCreateRequest request = new CrewCreateRequest(
                "아침 러닝",
                "한강 러닝",
                region.getId(),
                LocalDateTime.now().plusDays(1),
                "잠원 한강공원",
                37.5101,
                127.0201,
                10,
                PreferredLevel.INTERMEDIATE
        );

        Long crewId = crewService.createCrew(hostId, request);

        RunningCrew crew = runningCrewRepository.findWithHostRegionMembersById(crewId).orElseThrow();
        assertThat(crew.getHost().getId()).isEqualTo(hostId);
        assertThat(crew.getMembers()).hasSize(1);
        assertThat(crew.getMembers().get(0).getRole()).isEqualTo(CrewRole.HOST);
        assertThat(crew.getMaxParticipants()).isEqualTo(10);
    }

    @Test
    void getCrewDetail_returns_host_region_and_members() {
        Region region = regionRepository.save(new Region("서울", "마포구"));
        Long hostId = signUp("detail@example.com", "detail-user", region, PreferredLevel.BEGINNER);

        Long crewId = crewService.createCrew(hostId, new CrewCreateRequest(
                "저녁 러닝",
                "상암 하늘공원 러닝",
                region.getId(),
                LocalDateTime.now().plusDays(2),
                "상암 하늘공원",
                37.5665,
                126.9779,
                20,
                PreferredLevel.BEGINNER
        ));

        CrewDetailResponse detail = crewService.getCrewDetail(crewId);

        assertThat(detail.hostId()).isEqualTo(hostId);
        assertThat(detail.regionId()).isEqualTo(region.getId());
        assertThat(detail.members()).hasSize(1);
        assertThat(detail.members().get(0).userId()).isEqualTo(hostId);
        assertThat(detail.level()).isEqualTo(PreferredLevel.BEGINNER);
    }

    @Test
    void getCrews_filters_by_region_level_and_date() {
        Region seoulGangnam = regionRepository.save(new Region("서울", "강남구"));
        Region busanHaeundae = regionRepository.save(new Region("부산", "해운대구"));
        Long hostId = signUp("list@example.com", "list-host", seoulGangnam, PreferredLevel.BEGINNER);
        Long otherHostId = signUp("other@example.com", "other-host", busanHaeundae, PreferredLevel.ADVANCED);

        // 강남 / INTERMEDIATE / 내일 -> 필터에 잡혀야 함
        crewService.createCrew(hostId, new CrewCreateRequest(
                "강남 내일 러닝", "desc", seoulGangnam.getId(),
                LocalDateTime.now().plusDays(1),
                "잠원", 37.5, 127.0, 10, PreferredLevel.INTERMEDIATE
        ));
        // 강남 / BEGINNER / 오늘 -> level 미스매치로 제외
        crewService.createCrew(hostId, new CrewCreateRequest(
                "강남 오늘 러닝", "desc", seoulGangnam.getId(),
                LocalDateTime.now().plusHours(1),
                "역삼", 37.5, 127.0, 10, PreferredLevel.BEGINNER
        ));
        // 부산 / INTERMEDIATE -> region 미스매치로 제외
        crewService.createCrew(otherHostId, new CrewCreateRequest(
                "부산 러닝", "desc", busanHaeundae.getId(),
                LocalDateTime.now().plusDays(1),
                "해운대", 35.1, 129.0, 10, PreferredLevel.INTERMEDIATE
        ));

        CrewSearchCondition condition = new CrewSearchCondition(
                seoulGangnam.getId(),
                PreferredLevel.INTERMEDIATE,
                MeetingDateFilter.UPCOMING
        );
        Page<CrewSummaryResponse> result = crewService.getCrews(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().title()).contains("강남 내일");
    }

    @Test
    void updateCrew_changes_fields_when_host() {
        String suffix = String.valueOf(System.nanoTime());
        Region region = regionRepository.save(new Region("서울" + suffix, "서초구" + suffix));
        Region newRegion = regionRepository.save(new Region("서울" + suffix, "송파구" + suffix));
        Long hostId = signUp("update@example.com", "update-host", region, PreferredLevel.INTERMEDIATE);
        Long crewId = crewService.createCrew(hostId, new CrewCreateRequest(
                "원본 제목", "원본 설명", region.getId(),
                LocalDateTime.now().plusDays(1),
                "잠원", 37.5, 127.0, 5, PreferredLevel.INTERMEDIATE
        ));

        CrewUpdateRequest update = new CrewUpdateRequest(
                "새 제목",
                "새 설명",
                newRegion.getId(),
                LocalDateTime.now().plusDays(2),
                "올림픽공원",
                37.51,
                127.12,
                15,
                PreferredLevel.ADVANCED
        );

        crewService.updateCrew(crewId, hostId, update);

        RunningCrew updated = runningCrewRepository.findById(crewId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("새 제목");
        assertThat(updated.getRegion().getId()).isEqualTo(newRegion.getId());
        assertThat(updated.getMaxParticipants()).isEqualTo(15);
        assertThat(updated.getLevel()).isEqualTo(PreferredLevel.ADVANCED);
    }

    @Test
    void updateCrew_denies_when_not_host() {
        Region region = regionRepository.save(new Region("서울", "영등포구"));
        Long hostId = signUp("host2@example.com", "host2", region, PreferredLevel.BEGINNER);
        Long otherUserId = signUp("user@example.com", "user", region, PreferredLevel.BEGINNER);
        Long crewId = crewService.createCrew(hostId, sampleRequest(region.getId()));

        CrewUpdateRequest update = new CrewUpdateRequest(
                "제목", "설명", region.getId(),
                LocalDateTime.now().plusDays(1),
                "장소", 37.0, 127.0, 5, PreferredLevel.BEGINNER
        );

        assertThatThrownBy(() -> crewService.updateCrew(crewId, otherUserId, update))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void deleteCrew_removes_entity_and_members() {
        Region region = regionRepository.save(new Region("서울", "종로구"));
        Long hostId = signUp("delete@example.com", "delete-host", region, PreferredLevel.BEGINNER);
        Long crewId = crewService.createCrew(hostId, sampleRequest(region.getId()));

        crewService.deleteCrew(crewId, hostId);

        assertThat(runningCrewRepository.findById(crewId)).isEmpty();
        assertThat(crewMemberRepository.findAll()).isEmpty();
    }

    @Test
    void joinCrew_prevents_duplicate_and_capacity_exceeded() {
        Region region = regionRepository.save(new Region("서울", "노원구"));
        Long hostId = signUp("cap-host@example.com", "cap-host", region, PreferredLevel.INTERMEDIATE);
        Long crewId = crewService.createCrew(hostId, new CrewCreateRequest(
                "정원 1 모임", "desc", region.getId(),
                LocalDateTime.now().plusDays(1),
                "공원", 37.6, 127.1, 1, PreferredLevel.INTERMEDIATE
        ));
        Long memberId = signUp("member@example.com", "member", region, PreferredLevel.INTERMEDIATE);

        // 정원 1 (호스트 포함) → 추가 참여 시 정원 초과
        assertThatThrownBy(() -> crewService.joinCrew(crewId, memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREW_MEMBER_LIMIT_EXCEEDED);

        // 정원이 넉넉한 새 모임에서 중복 참여 방지 확인
        Long roomyCrewId = crewService.createCrew(hostId, new CrewCreateRequest(
                "정원 3 모임", "desc", region.getId(),
                LocalDateTime.now().plusDays(1),
                "공원", 37.6, 127.1, 3, PreferredLevel.INTERMEDIATE
        ));
        crewService.joinCrew(roomyCrewId, memberId);
        assertThatThrownBy(() -> crewService.joinCrew(roomyCrewId, memberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREW_MEMBER_ALREADY_JOINED);
    }

    @Test
    void leaveCrew_removes_member_but_host_cannot_leave() {
        Region region = regionRepository.save(new Region("서울", "동작구"));
        Long hostId = signUp("leave-host@example.com", "leave-host", region, PreferredLevel.INTERMEDIATE);
        Long crewId = crewService.createCrew(hostId, sampleRequest(region.getId()));
        Long memberId = signUp("join@example.com", "join", region, PreferredLevel.INTERMEDIATE);

        crewService.joinCrew(crewId, memberId);
        crewService.leaveCrew(crewId, memberId);

        RunningCrew crew = runningCrewRepository.findWithHostRegionMembersById(crewId).orElseThrow();
        assertThat(crew.getMembers()).hasSize(1); // 호스트만 남음

        assertThatThrownBy(() -> crewService.leaveCrew(crewId, hostId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    private Long signUp(String email, String nickname, Region region, PreferredLevel level) {
        return authService.signUp(new SignUpRequest(
                email,
                "password123",
                nickname,
                region.getId(),
                level
        ));
    }

    private CrewCreateRequest sampleRequest(Long regionId) {
        return new CrewCreateRequest(
                "샘플 러닝",
                "샘플 설명",
                regionId,
                LocalDateTime.now().plusDays(1),
                "샘플 장소",
                37.5,
                127.0,
                10,
                PreferredLevel.INTERMEDIATE
        );
    }
}
