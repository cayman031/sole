package com.sole.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.sole.global.common.ErrorCode;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("제약명이 매핑되면 대응 ErrorCode로 반환한다 - UK_user_email")
    void mapKnownConstraint() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate email",
                hibernateConstraint("UK_user_email")
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.DUPLICATED_EMAIL.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.DUPLICATED_EMAIL.getCode());
    }

    @Test
    @DisplayName("제약명이 메시지에만 있어도 매핑한다 - UK_crew_member")
    void mapConstraintFromMessage() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; constraint [UK_crew_member]"
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.CREW_MEMBER_ALREADY_JOINED.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.CREW_MEMBER_ALREADY_JOINED.getCode());
    }

    @Test
    @DisplayName("미매핑 제약이면 기본 코드(INTERNAL_SERVER_ERROR)로 반환한다")
    void unknownConstraintFallsBackToInternalServerError() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "unknown constraint",
                hibernateConstraint("SOME_UNKNOWN_CONSTRAINT")
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }

    private ConstraintViolationException hibernateConstraint(String constraintName) {
        SQLException sqlException = new SQLException("23000", "23000", 1062);
        return new ConstraintViolationException("violation", sqlException, constraintName);
    }
}
