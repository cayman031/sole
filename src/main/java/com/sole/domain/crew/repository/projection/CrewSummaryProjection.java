package com.sole.domain.crew.repository.projection;

import com.sole.domain.user.entity.PreferredLevel;
import java.time.LocalDateTime;

/**
 * 조회 전용 프로젝션(쿼리 DTO)
 * Repository가 반환하는 "조회 결과 형태"를 고정하기 위한 타입
 * API 응답 DTO(...Response)와 분리해서 계층 결합을 줄인다
 */
public record CrewSummaryProjection(
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
}