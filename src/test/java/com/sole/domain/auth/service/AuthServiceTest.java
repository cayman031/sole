package com.sole.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("중복 이메일이면 회원가입을 거부한다")
    void duplicateEmail() {
        SignUpRequest request = new SignUpRequest(
                "user@example.com",
                "password123",
                "닉네임",
                null,
                PreferredLevel.BEGINNER
        );
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATED_EMAIL);
    }

    @Test
    @DisplayName("회원가입 시 비밀번호를 암호화하고 저장한다")
    void signUpSuccess() {
        Region region = new Region("서울", "송파구");
        ReflectionTestUtils.setField(region, "id", 1L);
        SignUpRequest request = new SignUpRequest(
                "user@example.com",
                "password123",
                "닉네임",
                1L,
                PreferredLevel.INTERMEDIATE
        );
        when(regionRepository.findById(1L)).thenReturn(java.util.Optional.of(region));
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPw");

        User saved = User.builder()
                .email(request.email())
                .password("encodedPw")
                .nickname(request.nickname())
                .region(region)
                .preferredLevel(request.preferredLevel())
                .build();
        ReflectionTestUtils.setField(saved, "id", 10L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        Long userId = authService.signUp(request);

        assertThat(userId).isEqualTo(10L);
        verify(userRepository).save(any(User.class));
    }
}
