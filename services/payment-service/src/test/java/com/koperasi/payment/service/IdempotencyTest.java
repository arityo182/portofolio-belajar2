package com.koperasi.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.payment.config.PaymentProperties;
import com.koperasi.payment.domain.entity.IdempotencyKey;
import com.koperasi.payment.domain.enums.IdempotencyStatus;
import com.koperasi.payment.exception.IdempotencyConflictException;
import com.koperasi.payment.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private DistributedLockService distributedLockService;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        properties.setIdempotencyTtlHours(24);
        idempotencyService = new IdempotencyService(
                idempotencyKeyRepository,
                redisTemplate,
                distributedLockService,
                new ObjectMapper().findAndRegisterModules(),
                properties
        );
    }

    @Test
    void findCached_whenRedisHasResponse_returnsParsedCache() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("pay:idem:key-1"))
                .thenReturn(new ObjectMapper().writeValueAsString(Map.of("paymentId", "p-1")));

        Optional<Map<String, Object>> cached = idempotencyService.findCached("key-1");

        assertThat(cached).isPresent();
        assertThat(cached.get()).containsEntry("paymentId", "p-1");
        verifyNoInteractions(idempotencyKeyRepository);
    }

    @Test
    void findCached_whenDatabaseCompleted_returnsAndPopulatesRedis() {
        IdempotencyKey record = IdempotencyKey.processing(
                "key-1", "payment", Instant.now().plusSeconds(3600));
        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setResponseJson(Map.of("paymentId", "p-1"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("pay:idem:key-1")).thenReturn(null);
        when(idempotencyKeyRepository.findByKey("key-1")).thenReturn(Optional.of(record));

        Optional<Map<String, Object>> cached = idempotencyService.findCached("key-1");

        assertThat(cached).isPresent();
        verify(valueOperations).set(eq("pay:idem:key-1"), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void findCached_whenProcessing_throwsConflict() {
        IdempotencyKey record = IdempotencyKey.processing(
                "key-1", "payment", Instant.now().plusSeconds(3600));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("pay:idem:key-1")).thenReturn(null);
        when(idempotencyKeyRepository.findByKey("key-1")).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> idempotencyService.findCached("key-1"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void start_whenKeyNew_acquiresLockAndPersistsProcessing() {
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("lock-token");
        when(idempotencyKeyRepository.findByKey("key-1")).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String token = idempotencyService.start("key-1", "payment");

        assertThat(token).isEqualTo("lock-token");
        verify(idempotencyKeyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void start_whenLockCannotBeAcquired_throwsConflict() {
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn(null);

        assertThatThrownBy(() -> idempotencyService.start("key-1", "payment"))
                .isInstanceOf(IdempotencyConflictException.class);

        verifyNoInteractions(idempotencyKeyRepository);
    }

    @Test
    void start_whenPreviousAttemptFailed_resetsToProcessing() {
        IdempotencyKey failed = IdempotencyKey.processing(
                "key-1", "payment", Instant.now().minusSeconds(1));
        failed.setStatus(IdempotencyStatus.FAILED);
        when(distributedLockService.tryAcquire(anyString(), any(Duration.class))).thenReturn("lock-token");
        when(idempotencyKeyRepository.findByKey("key-1")).thenReturn(Optional.of(failed));
        when(idempotencyKeyRepository.save(failed)).thenReturn(failed);

        idempotencyService.start("key-1", "payment");

        assertThat(failed.getStatus()).isEqualTo(IdempotencyStatus.PROCESSING);
        assertThat(failed.getResponseJson()).isNull();
    }

    @Test
    void complete_marksCompletedCachesResponseAndReleasesLock() {
        IdempotencyKey record = IdempotencyKey.processing(
                "key-1", "payment", Instant.now().plusSeconds(3600));
        when(idempotencyKeyRepository.findByKey("key-1")).thenReturn(Optional.of(record));
        when(idempotencyKeyRepository.save(record)).thenReturn(record);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        idempotencyService.complete("key-1", "lock-token", Map.of("paymentId", "p-1"));

        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(record.getResponseJson()).containsEntry("paymentId", "p-1");
        verify(valueOperations).set(eq("pay:idem:key-1"), anyString(), eq(Duration.ofHours(24)));
        verify(distributedLockService).release("pay:idem:lock:key-1", "lock-token");
    }

    @Test
    void fail_marksFailedDeletesCacheAndReleasesLock() {
        IdempotencyKey record = IdempotencyKey.processing(
                "key-1", "payment", Instant.now().plusSeconds(3600));
        when(idempotencyKeyRepository.findByKey("key-1")).thenReturn(Optional.of(record));
        when(idempotencyKeyRepository.save(record)).thenReturn(record);

        idempotencyService.fail("key-1", "lock-token");

        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.FAILED);
        verify(redisTemplate).delete("pay:idem:key-1");
        verify(distributedLockService).release("pay:idem:lock:key-1", "lock-token");
    }

    @Test
    void findCached_whenRedisValueIsCorrupt_fallsBackToDatabase() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("pay:idem:key-1")).thenReturn("not-json");

        Optional<Map<String, Object>> cached = idempotencyService.findCached("key-1");

        assertThat(cached).isEmpty();
        verify(redisTemplate).delete("pay:idem:key-1");
        verify(idempotencyKeyRepository).findByKey("key-1");
    }
}
