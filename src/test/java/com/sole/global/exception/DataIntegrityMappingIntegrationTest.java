package com.sole.global.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.service.CrewService;
import com.sole.domain.region.entity.Region;
import com.sole.domain.region.repository.RegionRepository;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.repository.UserRepository;
import com.sole.domain.user.service.UserPrincipal;
import com.sole.global.common.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataIntegrityMappingIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CrewService crewService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("이메일 중복 회원가입 시 DUPLICATED_EMAIL로 매핑한다")
    void duplicateEmailSignUp() throws Exception {
        String email = "dup" + System.currentTimeMillis() + "@example.com";
        SignUpRequest request = new SignUpRequest(
                email,
                "password123",
                "닉네임",
                null,
                PreferredLevel.BEGINNER
        );
        String payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATED_EMAIL.getCode()));
    }

    @Test
    @DisplayName("모임 중복 참여 시 CREW_MEMBER_ALREADY_JOINED로 매핑한다")
    void duplicateJoinCrew() throws Exception {
        Region region = regionRepository.save(new Region("서울", "서초구"));
        User host = userRepository.save(User.builder()
                .email("host" + System.currentTimeMillis() + "@example.com")
                .password("pw")
                .nickname("호스트")
                .region(region)
                .preferredLevel(PreferredLevel.BEGINNER)
                .build());
        User member = userRepository.save(User.builder()
                .email("member" + System.currentTimeMillis() + "@example.com")
                .password("pw")
                .nickname("멤버")
                .region(region)
                .preferredLevel(PreferredLevel.BEGINNER)
                .build());

        Long crewId = crewService.createCrew(host.getId(), new CrewCreateRequest(
                "중복 참여 테스트",
                "설명",
                region.getId(),
                LocalDateTime.now().plusDays(1),
                "장소",
                37.0,
                127.0,
                5,
                PreferredLevel.BEGINNER
        ));

        UserPrincipal principal = UserPrincipal.from(member);

        mockMvc.perform(post("/api/v1/crews/{crewId}/join", crewId)
                        .with(user(principal)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/crews/{crewId}/join", crewId)
                        .with(user(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.CREW_MEMBER_ALREADY_JOINED.getCode()));
    }
}
