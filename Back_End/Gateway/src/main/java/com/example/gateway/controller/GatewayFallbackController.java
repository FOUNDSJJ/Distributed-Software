package com.example.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class GatewayFallbackController {

    @GetMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> authFallback() {
        return fallback("auth-service");
    }

    @GetMapping(value = "/order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> orderFallback() {
        return fallback("order-service");
    }

    @GetMapping(value = "/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> inventoryFallback() {
        return fallback("inventory-service");
    }

    private ResponseEntity<Map<String, Object>> fallback(String serviceName) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "status", "DEGRADED",
                "service", serviceName,
                "message", "Gateway fallback triggered, please retry later.",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
