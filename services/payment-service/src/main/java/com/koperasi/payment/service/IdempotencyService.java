package com.koperasi.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.payment.config.PaymentProperties;
import com.koperasi.payment.domain.entity.IdempotencyKey;
import com.koperasi.payment.domain.enums.IdempotencyStatus;
import com.koperasi.payment.exception.IdempotencyConflictException;
import com.koperasi.payment.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String CACHE_PREFIX = "pay:idem:";
    private static final String LOCK_PREFIX = "pay:idem:lock:";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final StringRedisTemplate redisTemplate;
    private final DistributedLockService distributedLockService;
    private final ObjectMapper objectMapper;
    private final PaymentProperties paymentProperties;

    public Optional<Map<String, Object>> findCached(String key) {
        String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + key);
        if (cached != null) {
            try {
                return Optional.of(objectMapper.readValue(cached, MAP_TYPE));
            } catch (Exception ex) {
                log.warn("Cache idempotency rusak key={}, fallback ke database", key);
                redisTemplate.delete(CACHE_PREFIX + key);
            }
        }

        IdempotencyKey record = idempotencyKeyRepository.findByKey(key).orElse(null);
        if (record == null) {
            return Optional.empty();
        }
        if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getResponseJson() != null) {
            putCache(key, record.getResponseJson());
            return Optional.of(record.getResponseJson());
        }
        if (record.getStatus() == IdempotencyStatus.PROCESSING && !record.isExpired(Instant.now())) {
            throw new IdempotencyConflictException("Permintaan dengan Idempotency-Key yang sama sedang diproses");
        }
        return Optional.empty();
    }

    @Transactional
    public String start(String key, String service) {
        String lockKey = LOCK_PREFIX + key;
        String lockToken = distributedLockService.tryAcquire(lockKey, Duration.ofSeconds(60));
        if (lockToken == null) {
            throw new IdempotencyConflictException("Permintaan dengan Idempotency-Key yang sama sedang diproses");
        }

        try {
            Instant now = Instant.now();
            IdempotencyKey record = idempotencyKeyRepository.findByKey(key).orElse(null);
            if (record != null) {
                if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getResponseJson() != null) {
                    throw new IdempotencyConflictException("Idempotency-Key sudah selesai diproses");
                }
                if (record.getStatus() == IdempotencyStatus.PROCESSING && !record.isExpired(now)) {
                    throw new IdempotencyConflictException(
                            "Permintaan dengan Idempotency-Key yang sama sedang diproses");
                }
                record.setService(service);
                record.setStatus(IdempotencyStatus.PROCESSING);
                record.setResponseJson(null);
                record.setExpiresAt(now.plusSeconds(60));
                idempotencyKeyRepository.save(record);
            } else {
                idempotencyKeyRepository.save(IdempotencyKey.processing(
                        key, service, now.plusSeconds(60)));
            }
            return lockToken;
        } catch (RuntimeException ex) {
            distributedLockService.release(lockKey, lockToken);
            throw ex;
        }
    }

    @Transactional
    public void complete(String key, String lockToken, Object response) {
        Map<String, Object> responseMap = objectMapper.convertValue(response, MAP_TYPE);
        IdempotencyKey record = idempotencyKeyRepository.findByKey(key)
                .orElseGet(() -> IdempotencyKey.processing(
                        key, "payment-service", Instant.now().plus(paymentProperties.idempotencyTtl())));
        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setResponseJson(responseMap);
        record.setExpiresAt(Instant.now().plus(paymentProperties.idempotencyTtl()));
        idempotencyKeyRepository.save(record);
        putCache(key, responseMap);
        distributedLockService.release(LOCK_PREFIX + key, lockToken);
    }

    @Transactional
    public void fail(String key, String lockToken) {
        IdempotencyKey record = idempotencyKeyRepository.findByKey(key).orElse(null);
        if (record != null) {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponseJson(null);
            idempotencyKeyRepository.save(record);
        }
        redisTemplate.delete(CACHE_PREFIX + key);
        distributedLockService.release(LOCK_PREFIX + key, lockToken);
    }

    private void putCache(String key, Map<String, Object> body) {
        try {
            redisTemplate.opsForValue().set(
                    CACHE_PREFIX + key,
                    objectMapper.writeValueAsString(body),
                    paymentProperties.idempotencyTtl()
            );
        } catch (Exception ex) {
            log.warn("Gagal menyimpan cache idempotency key={}: {}", key, ex.getMessage());
        }
    }
}
