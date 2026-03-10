package com.example.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class SessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public String createSession(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = SESSION_PREFIX + token;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(userId), SESSION_TTL);
        return token;
    }

    public Long getUserIdByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String value = stringRedisTemplate.opsForValue().get(SESSION_PREFIX + token);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    public void deleteSession(String token) {
        if (token != null && !token.isBlank()) {
            stringRedisTemplate.delete(SESSION_PREFIX + token);
        }
    }
}