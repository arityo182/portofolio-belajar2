package com.koperasi.payment.exception;

import com.koperasi.payment.domain.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleKoperasi_withBusinessException_returnsStatus() {
        ResponseEntity<ErrorResponse> response = handler.handleKoperasi(new PaymentNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void handleKoperasi_withTechnicalException_returnsBadGateway() {
        ResponseEntity<ErrorResponse> response = handler.handleKoperasi(
                new PaymentGatewayException("gateway down"));

        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("PAYMENT_GATEWAY_ERROR");
    }

    @Test
    void handleValidation_collectsFieldErrors() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "amount", "Jumlah wajib diisi"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(mock(MethodParameter.class), binding);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).containsExactly("amount: Jumlah wajib diisi");
    }

    @Test
    void handleMissingHeader_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleMissingHeader(
                new MissingRequestHeaderException("Idempotency-Key", mock(MethodParameter.class)));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).containsExactly("Idempotency-Key: header wajib diisi");
    }

    @Test
    void handleUnreadable_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(
                new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void handleTypeMismatch_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("abc", Integer.class, "page",
                        mock(MethodParameter.class), new NumberFormatException()));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).containsExactly("Parameter 'page' memiliki tipe yang tidak sesuai");
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void handleDataIntegrity_returnsConflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void handleUnexpected_returnsInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
