package com.sole.domain.auth.dto;

import com.sole.domain.user.entity.PreferredLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @Email
    @NotBlank
    @Size(max = 255)
    String email,

    @NotBlank
    @Size(min = 8, max = 100)
    String password,

    @NotBlank
    @Size(max = 50)
    String nickname,

    Long regionId,

    PreferredLevel preferredLevel
) {
}
