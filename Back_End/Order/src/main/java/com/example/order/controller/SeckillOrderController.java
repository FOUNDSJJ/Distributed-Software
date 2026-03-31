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
        // 下单接口必须先识别当前登录用户
        Long userId = getCurrentUserId(httpServletRequest);
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }

        Object productNameValue = request.get("product_name");
        if (!(productNameValue instanceof String productName) || productName.isBlank()) {
            return Map.of("success", false, "message", "商品名称不能为空");
        }

        OrderService.SubmitOrderResult result = orderService.submitSeckillOrder(userId, productName);
        if (!result.isSuccess()) {
            return Map.of("success", false, "message", result.getMessage());
        }

        // 下单成功后先返回排队状态，后续由异步流程推进
        return Map.of(
                "success", true,
                "message", result.getMessage(),
                "order_id", String.valueOf(result.getOrderId()),
                "status", "QUEUED",
                "product_name", result.getProductName()
        );
    }

    @GetMapping("/{orderId:\\d+}")
    public Map<String, Object> getByOrderId(@PathVariable Long orderId) {
        // 优先查数据库，查不到时再回退到 Redis 中的临时状态
        SeckillOrder order = orderService.findByOrderId(orderId);
        if (order != null) {
            return Map.of("success", true, "data", order);
        }

        String redisStatus = orderService.getOrderStatus(orderId);
        if (redisStatus != null) {
            return Map.of(
                    "success", true,
                    "data", Map.of("orderId", String.valueOf(orderId), "status", redisStatus)
            );
        }

        return Map.of("success", false, "message", "订单不存在");
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

    @PostMapping("/{orderId:\\d+}/pay")
    public Map<String, Object> payOrder(@PathVariable Long orderId, HttpServletRequest httpServletRequest) {
        // 支付前同样要确认订单归属用户
        Long userId = getCurrentUserId(httpServletRequest);
        if (userId == null) {
            return Map.of("success", false, "message", "用户未登录");
        }

        OrderService.PayOrderResult result = orderService.payOrder(userId, orderId);
        if (!result.isSuccess()) {
            return Map.of("success", false, "message", result.getMessage());
        }

        return Map.of(
                "success", true,
                "message", result.getMessage(),
                "order_id", String.valueOf(orderId),
                "status", "PAYING"
        );
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        // 统一从 SESSIONID Cookie 中提取用户身份
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
