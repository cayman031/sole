package com.sole.domain.auth.dto;

public record LoginResponse(Long userId, String nickname, String email) {
}
