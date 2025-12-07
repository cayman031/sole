package com.sole.domain.crew.dto;

import com.sole.domain.user.entity.PreferredLevel;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 모임 수정 요청 DTO.
 * Week 5–6에서 서비스/컨트롤러 로직을 붙일 예정이며,
 * 지금은 권한 체크 연결용 뼈대만 준비한다.
 */
public record CrewUpdateRequest(
        @NotBlank @Size(max = 100)
        String title,
        String description,
        @NotNull
        Long regionId,
        @NotNull
        LocalDateTime meetingTime,
        @NotBlank @Size(max = 255)
        String place,
        @NotNull
        Double latitude,
        @NotNull
        Double longitude,
        @Positive
        Integer maxParticipants,
        @NotNull
        PreferredLevel level
) {}