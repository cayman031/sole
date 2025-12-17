package com.sole.domain.crew.dto;

import com.sole.domain.crew.repository.projection.CrewSummaryProjection;
import com.sole.domain.user.entity.PreferredLevel;
import java.time.LocalDateTime;

/**
 * 모임 목록 한 건 요약 DTO (API 응답).
 */
public record CrewSummaryResponse(
        Long id,
        String title,
        Long regionId,
        String regionCity,
        String regionDistrict,
        LocalDateTime meetingTime,
        String place,
        double latitude,
        double longitude,
        int maxParticipants,
        long currentParticipants,
        PreferredLevel level
) {
    public static CrewSummaryResponse from(CrewSummaryProjection projection)
    {
        return new CrewSummaryResponse(
                projection.id(),
                projection.title(),
                projection.regionId(),
                projection.regionCity(),
                projection.regionDistrict(),
                projection.meetingTime(),
                projection.place(),
                projection.latitude(),
                projection.longitude(),
                projection.maxParticipants(),
                projection.currentParticipants(),
                projection.level()
        );
    }
}