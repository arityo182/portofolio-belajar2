package com.koperasi.loan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new DistributedLockService(redisTemplate);
    }

    @Test
    void tryAcquire_whenRedisAccepts_returnsFencingToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        String token = lockService.tryAcquire("lock:key", Duration.ofSeconds(10));

        assertThat(token).isNotBlank();
    }

    @Test
    void tryAcquire_whenLockHeld_returnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThat(lockService.tryAcquire("lock:key", Duration.ofSeconds(10))).isNull();
    }

    @Test
    void tryAcquire_whenRedisReturnsNull_returnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(null);

        assertThat(lockService.tryAcquire("lock:key", Duration.ofSeconds(10))).isNull();
    }

    @Test
    void release_executesTokenCheckedLuaScript() {
        lockService.release("lock:key", "token-1");

        verify(redisTemplate).execute(any(), eq(List.of("lock:key")), eq("token-1"));
    }

    @Test
    void release_withoutToken_doesNothing() {
        lockService.release("lock:key", null);

        verify(redisTemplate, never()).execute(any(), any(), any());
    }
}
