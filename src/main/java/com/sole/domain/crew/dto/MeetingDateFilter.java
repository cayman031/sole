package com.sole.domain.crew.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 모임 일시 필터: 오늘 / 이번 주 / 앞으로(이후) / 전체.
 */
public enum MeetingDateFilter {
    TODAY,
    THIS_WEEK,
    UPCOMING,   // 지금 이후 전체
    ALL;        // 필터 없음

    public LocalDateTime start() {
        return switch (this) {
            case TODAY, THIS_WEEK -> LocalDate.now().atStartOfDay();
            case UPCOMING -> LocalDateTime.now();
            case ALL -> null;
        };
    }

    public LocalDateTime endExclusive() {
        return switch (this) {
            case TODAY -> LocalDate.now().plusDays(1).atStartOfDay();
            case THIS_WEEK -> {
                // 다음 주 월요일 00:00까지 포함
                LocalDate nextMonday =
                        LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1);
                yield nextMonday.atStartOfDay();
            }
            case UPCOMING -> null;
            case ALL -> null;
        };
    }
}