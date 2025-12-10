package com.sole.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.dto.ChangePasswordRequest;
import com.sole.domain.user.dto.UpdateUserProfileRequest;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.domain.user.service.UserPrincipal;
import com.sole.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("/users/me 프로필 조회")
    void getMe_returns_profile() throws Exception {
        Region region = regionRepository.save(new Region("서울", "동작구"));
        User user = saveUser("me@example.com", "me", region, PreferredLevel.BEGINNER);
        MockHttpSession session = login(user, "password123");

        mockMvc.perform(get("/api/v1/users/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("me@example.com"))
                .andExpect(jsonPath("$.data.region.city").value("서울"));
    }

    @Test
    @DisplayName("/users/me 업데이트 및 비밀번호 변경")
    void update_profile_and_change_password() throws Exception {
        Region region = regionRepository.save(new Region("서울", "성동구"));
        Region newRegion = regionRepository.save(new Region("서울", "중구"));
        User user = saveUser("update@example.com", "before", region, PreferredLevel.BEGINNER);
        MockHttpSession session = login(user, "password123");

        UpdateUserProfileRequest update = new UpdateUserProfileRequest(
                "after",
                newRegion.getId(),
                PreferredLevel.ADVANCED
        );

        mockMvc.perform(put("/api/v1/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("after"))
                .andExpect(jsonPath("$.data.region.id").value(newRegion.getId()));

        ChangePasswordRequest changePassword = new ChangePasswordRequest(
                "password123",
                "newPass!234"
        );

        mockMvc.perform(put("/api/v1/users/me/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePassword)))
                .andExpect(status().isOk());

        User refreshed = userRepository.findById(user.getId()).orElseThrow();
        // 새 비밀번호가 반영되었는지 확인
        assertThat(passwordEncoder.matches("newPass!234", refreshed.getPassword())).isTrue();
    }

    private User saveUser(String email, String nickname, Region region, PreferredLevel level) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .nickname(nickname)
                .region(region)
                .preferredLevel(level)
                .build();
        return userRepository.save(user);
    }

    private MockHttpSession login(User user, String rawPassword) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String payload = """
                { "email": "%s", "password": "%s" }
                """.formatted(user.getEmail(), rawPassword);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .session(session))
                .andExpect(status().isOk());
        return session;
    }
}
