package com.sole.global.exception;

import com.sole.global.common.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
        // UK_crew_member 위반 시 중복 참여로 매핑
        ErrorCode code = ErrorCode.CREW_MEMBER_ALREADY_JOINED;
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
}
