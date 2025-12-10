package com.sole.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    @DisplayName("사용자와 연관된 지역을 함께 조회한다")
    void findWithRegionById() {
        Region region = regionRepository.save(new Region("서울", "마포구"));
        User user = userRepository.save(User.builder()
                .email("user@example.com")
                .password("pw")
                .nickname("사용자")
                .region(region)
                .preferredLevel(PreferredLevel.BEGINNER)
                .build());

        assertThat(userRepository.findWithRegionById(user.getId()))
                .get()
                .satisfies(found -> {
                    assertThat(found.getRegion()).isNotNull();
                    assertThat(found.getRegion().getDistrict()).isEqualTo("마포구");
                });
    }
}
