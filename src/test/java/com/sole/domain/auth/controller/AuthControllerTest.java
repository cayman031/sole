package com.sole.domain.auth.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sole.domain.auth.dto.LoginRequest;
import com.sole.domain.auth.dto.LoginResponse;
import com.sole.domain.auth.dto.SignUpRequest;
import com.sole.domain.auth.service.AuthService;
import com.sole.domain.user.entity.PreferredLevel;
import com.sole.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("회원가입 요청에 성공하면 성공 응답을 반환한다")
    void signUp() throws Exception {
        when(authService.signUp(any(SignUpRequest.class))).thenReturn(100L);

        SignUpRequest request = new SignUpRequest(
                "user@example.com",
                "password123",
                "닉네임",
                null,
                PreferredLevel.BEGINNER
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(100)));
    }

    @Test
    @DisplayName("로그인 요청 필드가 유효하지 않으면 400을 반환한다")
    void loginValidation() throws Exception {
        LoginRequest request = new LoginRequest("not-email", "short");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_INPUT_VALUE")));
    }

    @Test
    @DisplayName("로그인 성공 시 성공 응답을 반환한다")
    void loginSuccess() throws Exception {
        when(authService.login(any(LoginRequest.class), any()))
                .thenReturn(new LoginResponse(1L, "닉네임", "user@example.com"));

        LoginRequest request = new LoginRequest("user@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("user@example.com")))
                .andExpect(jsonPath("$.data.nickname", is("닉네임")));
    }
}
