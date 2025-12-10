package com.sole.domain.crew.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sole.domain.crew.dto.CrewCreateRequest;
import com.sole.domain.crew.dto.CrewDetailResponse;
import com.sole.domain.crew.dto.CrewDetailResponse.MemberDto;
import com.sole.domain.crew.dto.CrewSummaryResponse;
import com.sole.domain.crew.dto.MeetingDateFilter;
import com.sole.domain.crew.entity.CrewRole;
import com.sole.domain.crew.service.CrewService;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.domain.user.entity.User;
import com.sole.domain.user.service.UserPrincipal;
import com.sole.global.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CrewControllerTest {

    @Mock
    private CrewService crewService;

    @InjectMocks
    private CrewController crewController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(crewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new org.springframework.data.web.PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("모임을 생성하고 생성된 ID를 반환한다")
    void createCrew() throws Exception {
        UserPrincipal principal = testPrincipal(1L);
        authenticate(principal);
        CrewCreateRequest request = new CrewCreateRequest(
                "아침 러닝",
                "가볍게 한 바퀴",
                1L,
                LocalDateTime.now().plusDays(1),
                "한강",
                1.0,
                2.0,
                10,
                PreferredLevel.BEGINNER
        );
        when(crewService.createCrew(eq(1L), any(CrewCreateRequest.class))).thenReturn(50L);

        mockMvc.perform(post("/api/v1/crews")
                        .with(user(principal))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(50)));
    }

    @Test
    @DisplayName("모임 목록을 조회하면 페이지 형태의 데이터를 반환한다")
    void getCrews() throws Exception {
        CrewSummaryResponse summary = new CrewSummaryResponse(
                1L,
                "저녁 러닝",
                10L,
                "서울",
                "강동구",
                LocalDateTime.now(),
                "천호",
                1.0,
                2.0,
                5,
                1,
                PreferredLevel.INTERMEDIATE
        );
        when(crewService.getCrews(any(), any())).thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/v1/crews")
                        .param("dateFilter", MeetingDateFilter.UPCOMING.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title", is("저녁 러닝")));
    }

    @Test
    @DisplayName("모임 상세 정보를 반환한다")
    void getCrewDetail() throws Exception {
        CrewDetailResponse detail = new CrewDetailResponse(
                1L,
                "상세",
                "설명",
                1L,
                "호스트",
                10L,
                "서울",
                "성동구",
                LocalDateTime.now(),
                "뚝섬",
                1.0,
                2.0,
                5,
                PreferredLevel.BEGINNER,
                List.of(new MemberDto(1L, "호스트", CrewRole.HOST.name()))
        );
        when(crewService.getCrewDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/crews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("상세")))
                .andExpect(jsonPath("$.data.members[0].role", is("HOST")));
    }

    @Test
    @DisplayName("모임 참여 요청 시 인증 사용자의 ID를 전달한다")
    void joinCrewUsesPrincipal() throws Exception {
        UserPrincipal principal = testPrincipal(7L);
        authenticate(principal);

        mockMvc.perform(post("/api/v1/crews/3/join")
                        .with(user(principal)))
                .andExpect(status().isOk());

        verify(crewService).joinCrew(3L, 7L);
    }

    private UserPrincipal testPrincipal(Long id) {
        User user = User.builder()
                .email("user" + id + "@example.com")
                .password("pw")
                .nickname("user" + id)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return UserPrincipal.from(user);
    }

    private void authenticate(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities())
        );
    }
}
