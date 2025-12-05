package com.sole.domain.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void 회원가입_이메일형식아니면_검증실패() {
        SignUpRequest request = new SignUpRequest(
            "not-an-email",
            "P@ssword123",
            "nickname",
            null,
            null
        );

        Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void 회원가입_비밀번호최소길이미만이면_검증실패() {
        SignUpRequest request = new SignUpRequest(
            "user@example.com",
            "short",
            "nickname",
            null,
            null
        );

        Set<ConstraintViolation<SignUpRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void 로그인_빈비밀번호면_검증실패() {
        LoginRequest request = new LoginRequest(
            "user@example.com",
            ""
        );

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}
