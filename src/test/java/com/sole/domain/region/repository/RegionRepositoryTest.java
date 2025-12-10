package com.sole.domain.region.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sole.domain.region.entity.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegionRepositoryTest {

    @Autowired
    private RegionRepository regionRepository;

    @Test
    @DisplayName("도시/구 조합으로 조회할 수 있다")
    void findByCityAndDistrict() {
        Region saved = regionRepository.save(new Region("서울", "강남구"));

        assertThat(regionRepository.findByCityAndDistrict("서울", "강남구"))
                .contains(saved);
        assertThat(regionRepository.findByCityAndDistrict("부산", "해운대구"))
                .isEmpty();
    }
}
