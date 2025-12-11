package com.sole.domain.crew.dto;

import com.sole.domain.user.entity.PreferredLevel;
import java.time.LocalDateTime;

public record NearbyCrewResponse(
        Long crewId,
        String title,
        Long regionId,
        String regionCity,
        String regionDistrict,
        LocalDateTime meetingTime,
        String place,
        double latitude,
        double longitude,
        double distanceKm,
        int maxParticipants,
        long currentParticipants,
        PreferredLevel level
) {
    public static NearbyCrewResponse of(CrewSummaryResponse summary, double distanceKm) {
        return new NearbyCrewResponse(
                summary.id(),
                summary.title(),
                summary.regionId(),
                summary.regionCity(),
                summary.regionDistrict(),
                summary.meetingTime(),
                summary.place(),
                summary.latitude(),
                summary.longitude(),
                distanceKm,
                summary.maxParticipants(),
                summary.currentParticipants(),
                summary.level()
        );
    }
}
