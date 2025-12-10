package com.sole.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.auth.service.AuthService;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.dto.ChangePasswordRequest;
import com.sole.domain.user.dto.UpdateUserProfileRequest;
import com.sole.domain.user.dto.UserProfileResponse;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.service.UserService;
import com.sole.global.common.ErrorCode;
import com.sole.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.sole.domain.user.repository.UserRepository userRepository;

    @Test
    void getMe_returns_profile_with_region() {
        Region region = regionRepository.save(new Region("서울", "광진구"));
        Long userId = signUp("me@example.com", "me-user", region, PreferredLevel.BEGINNER);

        UserProfileResponse profile = userService.getMe(userId);

        assertThat(profile.email()).isEqualTo("me@example.com");
        assertThat(profile.region().id()).isEqualTo(region.getId());
        assertThat(profile.region().city()).isEqualTo("서울");
        assertThat(profile.region().district()).isEqualTo("광진구");
    }

    @Test
    void updateProfile_changes_nickname_region_and_level() {
        Region region = regionRepository.save(new Region("서울", "용산구"));
        Region newRegion = regionRepository.save(new Region("서울", "종로구"));
        Long userId = signUp("update-user@example.com", "before", region, PreferredLevel.BEGINNER);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "after",
                newRegion.getId(),
                PreferredLevel.ADVANCED
        );

        UserProfileResponse updated = userService.updateProfile(userId, request);

        assertThat(updated.nickname()).isEqualTo("after");
        assertThat(updated.region().id()).isEqualTo(newRegion.getId());
        assertThat(updated.preferredLevel()).isEqualTo(PreferredLevel.ADVANCED);
    }

    @Test
    void changePassword_succeeds_when_current_matches() {
        Region region = regionRepository.save(new Region("서울", "마포구"));
        Long userId = signUp("pwd@example.com", "pwd-user", region, PreferredLevel.INTERMEDIATE);

        userService.changePassword(userId, new ChangePasswordRequest(
                "password123",
                "newPass!234"
        ));

        // 비밀번호가 새 값으로 변경되었는지 확인
        var user = userRepository.findById(userId).orElseThrow();
        assertThat(passwordEncoder.matches("newPass!234", user.getPassword())).isTrue();
    }

    @Test
    void changePassword_throws_when_current_mismatch() {
        Region region = regionRepository.save(new Region("서울", "서대문구"));
        Long userId = signUp("pwd-fail@example.com", "pwd-fail", region, PreferredLevel.BEGINNER);

        assertThatThrownBy(() -> userService.changePassword(userId, new ChangePasswordRequest(
                "wrong-current",
                "newPass!234"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_MISMATCH);
    }

    private Long signUp(String email, String nickname, Region region, PreferredLevel level) {
        return authService.signUp(new SignUpRequest(
                email,
                "password123",
                nickname,
                region.getId(),
                level
        ));
    }
}
