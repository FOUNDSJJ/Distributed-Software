package com.example.auth.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RefreshScope
public class TrafficDemoService {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${demo.message:Dynamic config is not loaded from Nacos yet.}")
    private String demoMessage;

    @Value("${traffic.demo.delay-millis:0}")
    private long delayMillis;

    @Value("${traffic.demo.force-failure:false}")
    private boolean forceFailure;

    @Value("${traffic.demo.degrade-message:Traffic demo fallback}")
    private String degradeMessage;

    @CircuitBreaker(name = "trafficDemo", fallbackMethod = "fallback")
    @RateLimiter(name = "trafficDemo")
    public Map<String, Object> protectedCall(boolean failNow) {
        waitIfNecessary();
        if (forceFailure || failNow) {
            throw new IllegalStateException("Traffic demo forced failure.");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("status", "OK");
        data.put("service", serviceName);
        data.put("demoMessage", demoMessage);
        data.put("delayMillis", delayMillis);
        data.put("forcedFailure", forceFailure);
        data.put("timestamp", OffsetDateTime.now().toString());
        return data;
    }

    public Map<String, Object> fallback(boolean failNow, Throwable throwable) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", false);
        data.put("status", "DEGRADED");
        data.put("service", serviceName);
        data.put("demoMessage", demoMessage);
        data.put("delayMillis", delayMillis);
        data.put("forcedFailure", forceFailure || failNow);
        data.put("message", degradeMessage);
        data.put("reason", throwable.getClass().getSimpleName());
        data.put("timestamp", OffsetDateTime.now().toString());
        return data;
    }

    private void waitIfNecessary() {
        if (delayMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Traffic demo interrupted.", exception);
        }
    }
}
