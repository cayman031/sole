package com.sole.domain.crew;

import static org.assertj.core.api.Assertions.assertThat;

import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.auth.service.AuthService;
import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.dto.CrewDetailResponse;
import com.sole.domain.crew.entity.CrewRole;
import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.crew.repository.RunningCrewRepository;
import com.sole.domain.crew.service.CrewService;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CrewServiceTest {

    @Autowired
    private CrewService crewService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private RunningCrewRepository runningCrewRepository;

    @Test
    void createCrew_registers_host_as_member() {
        Region region = regionRepository.save(new Region("서울", "강남구"));
        Long hostId = authService.signUp(new SignUpRequest(
                "host@example.com",
                "password123",
                "host-user",
                region.getId(),
                PreferredLevel.INTERMEDIATE
        ));

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
        Long hostId = authService.signUp(new SignUpRequest(
                "detail@example.com",
                "password123",
                "detail-user",
                region.getId(),
                PreferredLevel.BEGINNER
        ));

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
}
