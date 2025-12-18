package com.sole.global.exception;

import com.sole.global.common.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<String, ErrorCode> CONSTRAINT_ERROR_MAP = Map.of(
            "UK_user_email", ErrorCode.DUPLICATED_EMAIL,
            "UK_crew_member", ErrorCode.CREW_MEMBER_ALREADY_JOINED,
            "UK_region_city_district", ErrorCode.INVALID_INPUT_VALUE,
            "FK_users_region", ErrorCode.INVALID_INPUT_VALUE,
            "FK_running_crews_host", ErrorCode.INVALID_INPUT_VALUE,
            "FK_running_crews_region", ErrorCode.INVALID_INPUT_VALUE,
            "FK_crew_members_crew", ErrorCode.INVALID_INPUT_VALUE,
            "FK_crew_members_user", ErrorCode.INVALID_INPUT_VALUE
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorResponse.FieldError.of(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList());
        ErrorResponse body = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errors);
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponse.FieldError> errors = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .map(msg -> ErrorResponse.FieldError.of("", msg))
                .collect(Collectors.toList());
        ErrorResponse body = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errors);
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        ErrorResponse body = ErrorResponse.of(code, ex.getMessage());
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ErrorCode code = resolveErrorCodeFromConstraint(ex).orElse(ErrorCode.INTERNAL_SERVER_ERROR);
        log.warn("data integrity violation mapped code={} constraint={} message={}",
                code.getCode(), extractConstraintName(ex).orElse("unknown"), ex.getMessage());
        ErrorResponse body = ErrorResponse.of(code);
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        ErrorCode code = ErrorCode.AUTHENTICATION_FAILED;
        ErrorResponse body = ErrorResponse.of(code);
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).body(body);
    }

    private Optional<ErrorCode> resolveErrorCodeFromConstraint(DataIntegrityViolationException ex) {
        return extractConstraintName(ex).map(CONSTRAINT_ERROR_MAP::get);
    }

    private Optional<String> extractConstraintName(DataIntegrityViolationException ex) {
        Throwable root = ex.getCause();
        if (root instanceof org.hibernate.exception.ConstraintViolationException hibernateEx) {
            if (hibernateEx.getConstraintName() != null) {
                return Optional.of(hibernateEx.getConstraintName());
            }
        }
        String message = ex.getMessage();
        if (message == null) {
            return Optional.empty();
        }
        return CONSTRAINT_ERROR_MAP.keySet().stream()
                .filter(message::contains)
                .findFirst();
    }
}
