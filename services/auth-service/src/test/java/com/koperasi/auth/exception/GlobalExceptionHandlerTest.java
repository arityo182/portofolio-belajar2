package com.koperasi.auth.exception;

import com.koperasi.auth.domain.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleKoperasi_withBusinessException_returnsExceptionStatusAndCode() {
        ResponseEntity<ErrorResponse> response = handler.handleKoperasi(new InvalidCredentialsException());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void handleKoperasi_withTechnicalException_returnsInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleKoperasi(
                new KafkaPublishException("publish failed", new RuntimeException("broker")));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("KAFKA_PUBLISH_ERROR");
    }

    @Test
    void handleValidation_collectsFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "Email wajib diisi"));
        bindingResult.addError(new FieldError("registerRequest", "password", "Kata sandi terlalu pendek"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().details())
                .containsExactly("email: Email wajib diisi", "password: Kata sandi terlalu pendek");
    }

    @Test
    void handleUnreadable_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(new HttpMessageNotReadableException("bad body", new MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void handleTypeMismatch_returnsBadRequestWithParameterDetail() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", mock(MethodParameter.class), new NumberFormatException());

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).containsExactly("Parameter 'page' memiliki tipe yang tidak sesuai");
    }

    @Test
    void handleUnexpected_returnsInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
