package com.sole.domain.crew.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sole.domain.crew.dto.CrewSummaryResponse;
import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RunningCrewRepositoryTest {

    @Autowired
    private RunningCrewRepository runningCrewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegionRepository regionRepository;

    private Region seoul;
    private User host;

    @BeforeEach
    void setUp() {
        seoul = regionRepository.save(new Region("서울", "강남구"));
        host = userRepository.save(User.builder()
                .email("host@example.com")
                .password("pw")
                .nickname("호스트")
                .region(seoul)
                .preferredLevel(PreferredLevel.INTERMEDIATE)
                .build());
    }

    @Test
    @DisplayName("검색 조건(지역/레벨/기간)에 따라 모임을 조회한다")
    void searchByCondition() {
        LocalDateTime now = LocalDateTime.now();
        RunningCrew crew1 = RunningCrew.builder()
                .title("이른 달리기")
                .description("아침 러닝")
                .host(host)
                .region(seoul)
                .meetingTime(now.plusDays(1))
                .place("한강")
                .latitude(37.0)
                .longitude(127.0)
                .maxParticipants(10)
                .level(PreferredLevel.BEGINNER)
                .build();
        RunningCrew crew2 = RunningCrew.builder()
                .title("저녁 달리기")
                .description("저녁 러닝")
                .host(host)
                .region(seoul)
                .meetingTime(now.plusDays(3))
                .place("올림픽공원")
                .latitude(37.5)
                .longitude(127.1)
                .maxParticipants(5)
                .level(PreferredLevel.INTERMEDIATE)
                .build();
        runningCrewRepository.save(crew1);
        runningCrewRepository.save(crew2);

        Page<CrewSummaryResponse> page = runningCrewRepository.search(
                seoul.getId(),
                PreferredLevel.INTERMEDIATE,
                now.plusDays(2),
                now.plusDays(4),
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent())
                .hasSize(1)
                .allSatisfy(summary -> {
                    assertThat(summary.title()).isEqualTo("저녁 달리기");
                    assertThat(summary.level()).isEqualTo(PreferredLevel.INTERMEDIATE);
                });
    }
}
