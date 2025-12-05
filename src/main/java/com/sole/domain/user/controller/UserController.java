package com.sole.domain.user.controller;

import com.sole.domain.user.dto.ChangePasswordRequest;
import com.sole.domain.user.dto.UpdateUserProfileRequest;
import com.sole.domain.user.dto.UserProfileResponse;
import com.sole.domain.user.service.UserPrincipal;
import com.sole.domain.user.service.UserService;
import com.sole.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        // 세션 인증으로부터 현재 로그인한 유저 ID 사용
        UserProfileResponse response = userService.getMe(principal.getId());
        return ApiResponse.success(response);
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfileResponse response = userService.updateProfile(principal.getId(), request);
        return ApiResponse.success(response);
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(principal.getId(), request);
        return ApiResponse.success();
    }
}
