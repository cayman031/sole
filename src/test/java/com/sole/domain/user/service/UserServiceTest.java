package com.sole.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.dto.ChangePasswordRequest;
import com.sole.domain.user.dto.UpdateUserProfileRequest;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("현재 비밀번호가 맞지 않으면 변경을 막는다")
    void changePasswordMismatch() {
        User user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "newPassword1!");

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("존재하지 않는 지역으로는 프로필을 수정할 수 없다")
    void updateProfileRegionNotFound() {
        User user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(regionRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "새닉네임",
                99L,
                PreferredLevel.ADVANCED
        );

        assertThatThrownBy(() -> userService.updateProfile(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REGION_NOT_FOUND);
    }

    private User sampleUser() {
        User user = User.builder()
                .email("user@example.com")
                .password("hashed")
                .nickname("기존닉")
                .region(new Region("서울", "영등포구"))
                .preferredLevel(PreferredLevel.INTERMEDIATE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
