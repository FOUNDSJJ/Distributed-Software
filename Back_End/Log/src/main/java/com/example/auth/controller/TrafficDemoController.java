package com.example.auth.controller;

import com.example.auth.service.TrafficDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/traffic")
public class TrafficDemoController {

    private final TrafficDemoService trafficDemoService;

    public TrafficDemoController(TrafficDemoService trafficDemoService) {
        this.trafficDemoService = trafficDemoService;
    }

    @GetMapping("/unstable")
    public Map<String, Object> unstable(
            @RequestParam(name = "failNow", defaultValue = "false") boolean failNow
    ) {
        return trafficDemoService.protectedCall(failNow);
    }
}
