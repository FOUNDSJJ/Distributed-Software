package com.example.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RefreshScope
@RestController
@RequestMapping("/api/config")
public class ConfigDemoController {

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

    @GetMapping("/demo")
    public Map<String, Object> demo() {
        return Map.of(
                "success", true,
                "service", serviceName,
                "demoMessage", demoMessage,
                "delayMillis", delayMillis,
                "forceFailure", forceFailure,
                "degradeMessage", degradeMessage,
                "timestamp", OffsetDateTime.now().toString()
        );
    }
}
