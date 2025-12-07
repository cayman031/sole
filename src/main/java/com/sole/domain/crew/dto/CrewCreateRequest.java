package com.sole.domain.crew.dto;

import com.sole.domain.user.entity.PreferredLevel;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CrewCreateRequest(
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