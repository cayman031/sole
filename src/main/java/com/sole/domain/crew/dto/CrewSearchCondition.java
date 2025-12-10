package com.sole.domain.crew.dto;

import com.sole.domain.user.entity.PreferredLevel;
import java.time.LocalDateTime;

/**
 * GET /api/v1/crews 검색 조건.
 * @ModelAttribute로 바인딩되므로 필드는 참조 타입으로 두어 null 허용.
 */
public record CrewSearchCondition(
        Long regionId,
        PreferredLevel level,
        MeetingDateFilter dateFilter
) {
    public LocalDateTime startDateTime() {
        return dateFilter == null ? null : dateFilter.start();
    }

    public LocalDateTime endDateTimeExclusive() {
        return dateFilter == null ? null : dateFilter.endExclusive();
    }
}