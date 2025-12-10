package com.sole.domain.crew;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.dto.CrewUpdateRequest;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.domain.user.service.UserPrincipal;
import com.sole.support.IntegrationTestBase;
import java.time.LocalDateTime;
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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.mock.web.MockHttpSession;

/**
 * REST API 계약을 MockMvc로 검증한다.
 * - 세션 기반 인증은 SecurityMockMvcRequestPostProcessors.user(UserPrincipal)로 시뮬레이션.
 * - DB는 Testcontainers MySQL을 사용.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CrewControllerIntegrationTest extends IntegrationTestBase {

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
    @DisplayName("모임 생성 후 목록 조회 필터가 적용된다")
    void create_and_list_crews() throws Exception {
        Region region = regionRepository.save(new Region("서울", "강남구"));
        User host = saveUser("host@example.com", "host", region, PreferredLevel.INTERMEDIATE);

        CrewCreateRequest request = new CrewCreateRequest(
                "아침 러닝",
                "한강에서 러닝",
                region.getId(),
                LocalDateTime.now().plusDays(1),
                "잠원 한강공원",
                37.5,
                127.0,
                10,
                PreferredLevel.INTERMEDIATE
        );

        MockHttpSession session = login(host, "password123");

        mockMvc.perform(post("/api/v1/crews")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNumber());

        mockMvc.perform(get("/api/v1/crews")
                        .param("regionId", region.getId().toString())
                        .param("level", PreferredLevel.INTERMEDIATE.name())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("아침 러닝"))
                .andExpect(jsonPath("$.data.content[0].regionCity").value("서울"));
    }

    @Test
    @DisplayName("호스트가 아니면 모임 수정 시 ACCESS_DENIED 반환")
    void update_denied_for_non_host() throws Exception {
        Region region = regionRepository.save(new Region("서울", "서초구"));
        User host = saveUser("host2@example.com", "host2", region, PreferredLevel.BEGINNER);
        User other = saveUser("other@example.com", "other", region, PreferredLevel.BEGINNER);

        Long crewId = createCrew(host, region, "호스트 모임");

        CrewUpdateRequest update = new CrewUpdateRequest(
                "수정 제목",
                "수정 설명",
                region.getId(),
                LocalDateTime.now().plusDays(1),
                "장소",
                37.4,
                127.1,
                20,
                PreferredLevel.ADVANCED
        );

        MockHttpSession otherSession = login(other, "password123");

        mockMvc.perform(put("/api/v1/crews/{id}", crewId)
                        .session(otherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("참여/취소 API가 성공 응답을 반환한다")
    void join_and_leave_flow() throws Exception {
        Region region = regionRepository.save(new Region("서울", "마포구"));
        User host = saveUser("host3@example.com", "host3", region, PreferredLevel.INTERMEDIATE);
        User member = saveUser("member@example.com", "member", region, PreferredLevel.INTERMEDIATE);
        Long crewId = createCrew(host, region, "저녁 러닝");

        MockHttpSession memberSession = login(member, "password123");

        mockMvc.perform(post("/api/v1/crews/{id}/join", crewId)
                        .session(memberSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/crews/{id}/leave", crewId)
                        .session(memberSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private Long createCrew(User host, Region region, String title) throws Exception {
        CrewCreateRequest request = new CrewCreateRequest(
                title,
                "설명",
                region.getId(),
                LocalDateTime.now().plusDays(1),
                "장소",
                37.5,
                127.0,
                10,
                PreferredLevel.INTERMEDIATE
        );

        String response = mockMvc.perform(post("/api/v1/crews")
                        .with(user(UserPrincipal.from(host)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // data는 Long 타입 crewId
        return objectMapper.readTree(response).path("data").asLong();
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
