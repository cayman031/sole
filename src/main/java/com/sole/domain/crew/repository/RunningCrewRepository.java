package com.sole.domain.crew.repository;

import com.sole.domain.crew.entity.RunningCrew;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunningCrewRepository extends JpaRepository<RunningCrew, Long> {

    // 상세 조회 시 호스트/지역/멤버들을 함께 로딩해 N+1을 줄인다.
    @EntityGraph(attributePaths = {"host", "region", "members", "members.user"})
    Optional<RunningCrew> findWithHostRegionMembersById(Long id);
}