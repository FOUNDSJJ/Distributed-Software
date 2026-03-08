package com.example.auth.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    private final Map<String, Long> sessionMap = new ConcurrentHashMap<>();

    public String createSession(Long userId) {
        String token = UUID.randomUUID().toString();
        sessionMap.put(token, userId);
        return token;
    }

    public Long getUserIdByToken(String token) {
        return sessionMap.get(token);
    }

    public void deleteSession(String token) {
        sessionMap.remove(token);
    }
}