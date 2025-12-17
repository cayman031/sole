package com.sole.domain.auth.controller;

import com.sole.domain.auth.dto.LoginRequest;
import com.sole.domain.auth.dto.LoginResponse;
import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.auth.dto.SignUpResponse;
import com.sole.domain.auth.service.AuthService;
import com.sole.domain.auth.service.AuthService.LoginResult;
import com.sole.global.common.ApiResponse;
import com.sole.global.security.SessionManager;
import jakarta.servlet.http.HttpServletRequest;
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
    private final SessionManager sessionManager;

    @PostMapping("/signup")
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody
                                              SignUpRequest request) {
        Long userId = authService.signUp(request);
        return ApiResponse.success(new SignUpResponse(userId));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest
    ) {
        LoginResult result = authService.login(request);
        sessionManager.storeAuthentication(httpRequest, result.authentication());
        return ApiResponse.success(new LoginResponse(result.userId(), result.nickname(), result.email()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        sessionManager.clearSession(request);
        authService.logout();
        return ApiResponse.success();
    }
}