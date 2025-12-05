package com.sole.domain.region.repository;

import com.sole.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region,Long> {
    Optional<Region> findByCityAndDistrict(String city, String district);
}
