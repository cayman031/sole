package com.sole.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.auth.service.AuthService;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void signUp_persists_user_with_encoded_password_and_region() {
        Region region = regionRepository.save(new Region("서울", "송파구"));

        SignUpRequest request = new SignUpRequest(
                "signup@example.com",
                "password123",
                "runner",
                region.getId(),
                PreferredLevel.BEGINNER
        );

        Long userId = authService.signUp(request);

        User saved = userRepository.findById(userId).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo(request.email());
        assertThat(passwordEncoder.matches(request.password(), saved.getPassword())).isTrue();
        assertThat(saved.getRegion().getId()).isEqualTo(region.getId());
        assertThat(saved.getPreferredLevel()).isEqualTo(PreferredLevel.BEGINNER);
    }
}
