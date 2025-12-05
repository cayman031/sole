package com.sole.domain.auth.service;

import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegionRepository regionRepository;

    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
    }

    @Test
    void 회원가입_시_비밀번호는_BCrypt로_암호화된다() {
        // given
        SignUpRequest req = new SignUpRequest("a@b.com", "P@ssw0rd123", "nick", null, null);
        given(userRepository.existsByEmail(req.email())).willReturn(false);
        given(userRepository.save(any())).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        // when
        Long userId = authService.signUp(req);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(userId).isEqualTo(1L);
        assertThat(saved.getEmail()).isEqualTo(req.email());
        assertThat(saved.getNickname()).isEqualTo(req.nickname());
        assertThat(saved.getPassword()).isNotEqualTo(req.password());
        assertThat(passwordEncoder.matches(req.password(), saved.getPassword())).isTrue();
    }

    @Test
    void 중복_이메일이면_예외() {
        SignUpRequest req = new SignUpRequest("dup@b.com", "P@ssw0rd123", "nick", null, null);
        given(userRepository.existsByEmail(req.email())).willReturn(true);

        assertThatThrownBy(() -> authService.signUp(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 이메일");
    }

    @Test
    void 지역_ID가_있으면_조회에_실패시_예외() {
        SignUpRequest req = new SignUpRequest("a@b.com", "P@ssw0rd123", "nick", 99L, null);
        given(userRepository.existsByEmail(req.email())).willReturn(false);
        given(regionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signUp(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("존재하지 않는 지역");
    }

    @Test
    void 지역_ID가_있으면_연결된다() {
        Region region = new Region("서울시", "강남구");
        SignUpRequest req = new SignUpRequest("a@b.com", "P@ssw0rd123", "nick", 1L, null);
        given(userRepository.existsByEmail(req.email())).willReturn(false);
        given(regionRepository.findById(1L)).willReturn(Optional.of(region));
        given(userRepository.save(any())).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        authService.signUp(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRegion()).isEqualTo(region);
    }
}
