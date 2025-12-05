package com.sole.global.exception;

import com.sole.global.common.ErrorCode;
import java.util.List;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final boolean success;
    private final String code;
    private final String message;
    private final List<FieldError> errors;

    private ErrorResponse(ErrorCode errorCode, String message, List<FieldError> errors) {
        this.success = false;
        this.code = errorCode.getCode();
        this.message = message;
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode, errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode, message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode, errorCode.getMessage(), errors);
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String reason;

        private FieldError(String field, String reason) {
            this.field = field;
            this.reason = reason;
        }

        public static FieldError of(String field, String reason) {
            return new FieldError(field, reason);
        }
    }
}
