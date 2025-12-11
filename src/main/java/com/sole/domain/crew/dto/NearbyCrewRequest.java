package com.sole.domain.crew.dto;

import com.sole.domain.user.entity.PreferredLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record NearbyCrewRequest(
        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        Double latitude,
        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        Double longitude,
        @NotNull @Positive
        Double radiusKm,
        PreferredLevel level,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {
    public LocalDateTime endDateTimeExclusive() {
        return endDateTime;
    }
}
