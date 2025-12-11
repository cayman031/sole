package com.sole.domain.crew.repository;

import com.sole.domain.crew.dto.CrewSummaryResponse;
import com.sole.domain.crew.entity.RunningCrew;
import com.sole.domain.user.entity.PreferredLevel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RunningCrewRepository extends JpaRepository<RunningCrew,
        Long> {

    @EntityGraph(attributePaths = {"host", "region", "members",
            "members.user"})
    Optional<RunningCrew> findWithHostRegionMembersById(Long id);

    @Query("""
          select new com.sole.domain.crew.dto.CrewSummaryResponse(
              c.id,
              c.title,
              r.id,
              r.city,
              r.district,
              c.meetingTime,
              c.place,
              c.latitude,
              c.longitude,
              c.maxParticipants,
              (select count(cm) from CrewMember cm where cm.crew = c),
              c.level
          )
          from RunningCrew c
          join c.region r
          where (:regionId is null or r.id = :regionId)
            and (:level is null or c.level = :level)
            and (:start is null or c.meetingTime >= :start)
            and (:end is null or c.meetingTime < :end)
          """)
    Page<CrewSummaryResponse> search(
            @Param("regionId") Long regionId,
            @Param("level") PreferredLevel level,
            @Param("start") LocalDateTime startDateTime,
            @Param("end") LocalDateTime endExclusive,
            Pageable pageable
    );

    @Query("""
            select new com.sole.domain.crew.dto.CrewSummaryResponse(
                c.id,
                c.title,
                r.id,
                r.city,
                r.district,
                c.meetingTime,
                c.place,
                c.latitude,
                c.longitude,
                c.maxParticipants,
                (select count(cm) from CrewMember cm where cm.crew = c),
                c.level
            )
            from RunningCrew c
            join c.region r
            where c.latitude between :minLat and :maxLat
              and c.longitude between :minLng and :maxLng
              and (:level is null or c.level = :level)
              and (:start is null or c.meetingTime >= :start)
              and (:end is null or c.meetingTime < :end)
            """)
    List<CrewSummaryResponse> searchWithinBoundingBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("level") PreferredLevel level,
            @Param("start") LocalDateTime startDateTime,
            @Param("end") LocalDateTime endExclusive
    );
}
