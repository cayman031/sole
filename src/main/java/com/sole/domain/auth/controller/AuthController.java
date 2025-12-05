package com.sole.domain.auth.controller;

import com.sole.domain.auth.dto.LoginRequest;
import com.sole.domain.auth.dto.LoginResponse;
import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.auth.dto.SignUpResponse;
import com.sole.domain.auth.service.AuthService;
import com.sole.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        Long userId = authService.signUp(request);
        return ApiResponse.success(new SignUpResponse(userId));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login (
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginResponse response = authService.login(request, httpRequest);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ApiResponse.success();
    }
}
