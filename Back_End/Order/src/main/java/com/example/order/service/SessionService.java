package com.example.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private static final String SESSION_PREFIX = "session:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
}
