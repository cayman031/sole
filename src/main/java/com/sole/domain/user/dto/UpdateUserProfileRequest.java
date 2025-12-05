package com.sole.domain.user.dto;

import com.sole.domain.user.entity.PreferredLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank
        @Size(max = 50)
        String nickname,
        Long regionId,                          // null 허용
        PreferredLevel preferredLevel           // null 허용
) {
}
