package com.sole.domain.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoValidationTest {

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
    void 프로필수정_닉네임공백이면_검증실패() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
            "   ",
            null,
            null
        );

        Set<ConstraintViolation<UpdateUserProfileRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void 비밀번호변경_현재비밀번호공백이면_검증실패() {
        ChangePasswordRequest request = new ChangePasswordRequest(
            "",
            "NewPassword123"
        );

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void 비밀번호변경_새비밀번호최소길이미만이면_검증실패() {
        ChangePasswordRequest request = new ChangePasswordRequest(
            "CurrentPassword123",
            "short"
        );

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}
