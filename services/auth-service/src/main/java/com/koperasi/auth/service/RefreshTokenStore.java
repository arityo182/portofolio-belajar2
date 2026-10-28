package com.koperasi.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(UUID userId, UUID tokenId, String tokenHash, Duration ttl) {
        redisTemplate.opsForValue().set(key(userId, tokenId), tokenHash, ttl);
    }

    public Optional<String> find(UUID userId, UUID tokenId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId, tokenId)));
    }

    public void delete(UUID userId, UUID tokenId) {
        redisTemplate.delete(key(userId, tokenId));
    }

    public void deleteAllForUser(UUID userId) {
        String pattern = KEY_PREFIX + userId + ":*";
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String key(UUID userId, UUID tokenId) {
        return KEY_PREFIX + userId + ":" + tokenId;
    }
}
