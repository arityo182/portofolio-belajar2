package com.koperasi.loan.exception;

import com.koperasi.loan.domain.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    void handleKoperasi_withResourceNotFound_returns404() {
        ResponseEntity<ErrorResponse> response = handler.handleKoperasi(new LoanNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void handleKoperasi_withConcurrentModification_includesVersionDetail() {
        ResponseEntity<ErrorResponse> response =
                handler.handleKoperasi(new ConcurrentModificationException(2L, 3L));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(response.getBody().details()).containsExactly("expectedVersion=2, actualVersion=3");
    }

    @Test
    void handleKoperasi_withTechnicalException_returns500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleKoperasi(new KafkaPublishException("failed", new RuntimeException("broker")));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("KAFKA_PUBLISH_ERROR");
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
    void handleMissingHeader_returnsBadRequestWithHeaderDetail() {
        MissingRequestHeaderException exception =
                new MissingRequestHeaderException("Idempotency-Key", mock(MethodParameter.class));

        ResponseEntity<ErrorResponse> response = handler.handleMissingHeader(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).containsExactly("Idempotency-Key: header wajib diisi");
    }

    @Test
    void handleUnreadable_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(
                new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void handleTypeMismatch_returnsBadRequest() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", mock(MethodParameter.class), new NumberFormatException());

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).containsExactly("Parameter 'page' memiliki tipe yang tidak sesuai");
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleOptimisticLock_returnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException("LoanApplication", UUID.randomUUID()));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
    }

    @Test
    void handleDataIntegrity_returnsConflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate key"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void handleUnexpected_returnsInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
