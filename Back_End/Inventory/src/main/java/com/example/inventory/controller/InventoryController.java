package com.example.inventory.controller;

import com.example.inventory.model.InventoryTxLog;
import com.example.inventory.model.Product;
import com.example.inventory.model.SeckillOrderMessage;
import com.example.inventory.model.StockDeductionResultMessage;
import com.example.inventory.model.StockRollbackMessage;
import com.example.inventory.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/products/{id:\\d+}")
    public Map<String, Object> getProduct(@PathVariable("id") Long id) {
        Product product = inventoryService.getProduct(id);
        if (product == null) {
            return Map.of("success", false, "message", "商品不存在");
        }
        return Map.of("success", true, "data", product);
    }

    @GetMapping("/tx/{orderId:\\d+}")
    public Map<String, Object> getTxLog(@PathVariable("orderId") Long orderId) {
        InventoryTxLog txLog = inventoryService.getTxLog(orderId);
        if (txLog == null) {
            return Map.of("success", false, "message", "库存事务记录不存在");
        }
        return Map.of("success", true, "data", txLog);
    }

    @PostMapping("/deduct")
    public Map<String, Object> deduct(@RequestBody Map<String, Object> request) {
        Long orderId = getLong(request.get("order_id"));
        Long userId = getLong(request.get("user_id"));
        Long productId = getLong(request.get("product_id"));
        if (orderId == null || userId == null || productId == null) {
            return Map.of("success", false, "message", "order_id、user_id 和 product_id 不能为空");
        }

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(userId);
        message.setProductId(productId);

        StockDeductionResultMessage result = inventoryService.deduct(message);
        return Map.of("success", result.isSuccess(), "data", result);
    }

    @PostMapping("/rollback")
    public Map<String, Object> rollback(@RequestBody Map<String, Object> request) {
        Long orderId = getLong(request.get("order_id"));
        Long productId = getLong(request.get("product_id"));
        if (orderId == null || productId == null) {
            return Map.of("success", false, "message", "order_id 和 product_id 不能为空");
        }

        String reason = request.get("reason") instanceof String text ? text : "手动回滚";

        StockRollbackMessage message = new StockRollbackMessage();
        message.setOrderId(orderId);
        message.setProductId(productId);
        message.setReason(reason);
        inventoryService.rollback(message);

        return Map.of("success", true, "message", "库存回滚请求已完成");
    }

    private Long getLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
