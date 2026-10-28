package com.koperasi.loan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private static final String RELEASE_SCRIPT_TEXT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """;

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(RELEASE_SCRIPT_TEXT, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * Acquires a Redis lock using SET NX PX. Returns a fencing token, or {@code null} when the lock is held.
     */
    public String tryAcquire(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    /**
     * Releases the lock only if the token still matches, preventing deletion of another owner's lock.
     */
    public void release(String key, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        redisTemplate.execute(RELEASE_SCRIPT, List.of(key), token);
    }
}
