package com.sole.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.sole.global.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

/**
 * GlobalExceptionHandler가 BusinessException과 검증 실패를 표준 에러 응답으로 변환하는지 단위 수준에서 확인한다.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException을 에러 코드에 맞게 매핑한다")
    void businessException_mapped_to_error_response() {
        BusinessException ex = new BusinessException(ErrorCode.CREW_NOT_FOUND);

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.CREW_NOT_FOUND.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.CREW_NOT_FOUND.getCode());
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException을 INVALID_INPUT_VALUE로 매핑한다")
    void methodArgumentNotValid_mapped_to_error_response() throws NoSuchMethodException {
        // 가짜 BindingResult에 필드 에러를 하나 세팅
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Dummy(""), "dummy");
        bindingResult.addError(new FieldError("dummy", "name", "must not be blank"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", Dummy.class),
                0
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(ex);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().getErrors()).hasSize(1);
    }

    // 테스트용 더미 타입/메서드
    private record Dummy(String name) {}

    private void dummyMethod(Dummy dummy) {
        // no-op
    }
}
