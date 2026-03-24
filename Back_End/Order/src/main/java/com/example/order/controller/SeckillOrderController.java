package com.example.order.controller;

import com.example.order.model.SeckillOrder;
import com.example.order.service.OrderService;
import com.example.order.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill/orders")
public class SeckillOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SessionService sessionService;

    @PostMapping
    public Map<String, Object> createOrder(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpServletRequest
    ) {
        Long userId = getCurrentUserId(httpServletRequest);
        if (userId == null) {
            return Map.of("success", false, "message", "User is not logged in");
        }

        Object productIdValue = request.get("product_id");
        if (!(productIdValue instanceof Number productIdNumber)) {
            return Map.of("success", false, "message", "product_id must be a number");
        }

        OrderService.SubmitOrderResult result = orderService.submitSeckillOrder(userId, productIdNumber.longValue());
        if (!result.isSuccess()) {
            return Map.of("success", false, "message", result.getMessage());
        }

        return Map.of(
                "success", true,
                "message", result.getMessage(),
                "order_id", result.getOrderId(),
                "status", "QUEUED",
                "product_name", result.getProductName()
        );
    }

    @GetMapping("/{orderId:\\d+}")
    public Map<String, Object> getByOrderId(@PathVariable Long orderId) {
        SeckillOrder order = orderService.findByOrderId(orderId);
        if (order != null) {
            return Map.of("success", true, "data", order);
        }

        String redisStatus = orderService.getOrderStatus(orderId);
        if (redisStatus != null) {
            return Map.of(
                    "success", true,
                    "data", Map.of("orderId", orderId, "status", redisStatus)
            );
        }

        return Map.of("success", false, "message", "Order not found");
    }

    @GetMapping
    public Map<String, Object> getByUserId(@RequestParam("userId") Long userId) {
        List<SeckillOrder> orders = orderService.findByUserId(userId);
        return Map.of(
                "success", true,
                "count", orders.size(),
                "data", orders
        );
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("SESSIONID".equals(cookie.getName())) {
                return sessionService.getUserIdByToken(cookie.getValue());
            }
        }

        return null;
    }
}
