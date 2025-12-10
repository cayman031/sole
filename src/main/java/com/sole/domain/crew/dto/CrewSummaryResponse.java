package com.sole.domain.crew.dto;

import com.sole.domain.user.entity.PreferredLevel;
import java.time.LocalDateTime;

/**
 * 모임 목록 한 건 요약 DTO.
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
) {}
