package com.example.order.service;

import com.example.order.mapper.OrderMapper;
import com.example.order.mapper.ProductMapper;
import com.example.order.model.Product;
import com.example.order.model.SeckillOrder;
import com.example.order.model.SeckillOrderMessage;
import com.example.order.util.OrderIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Duration ORDER_STATUS_TTL = Duration.ofHours(24);
    private static final Duration PENDING_ORDER_TTL = Duration.ofMinutes(30);
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>(
            """
            local stockKey = KEYS[1]
            local orderedKey = KEYS[2]
            local pendingOrderKey = KEYS[3]
            local statusKey = KEYS[4]
            local userId = ARGV[1]
            local orderId = ARGV[2]
            local pendingTtl = tonumber(ARGV[3])
            local statusTtl = tonumber(ARGV[4])
            local stock = tonumber(redis.call('GET', stockKey) or '-1')
            if stock < 0 then
                return -3
            end
            if redis.call('SISMEMBER', orderedKey, userId) == 1 then
                return -2
            end
            if stock <= 0 then
                return -1
            end
            redis.call('DECR', stockKey)
            redis.call('SADD', orderedKey, userId)
            redis.call('SET', pendingOrderKey, orderId, 'EX', pendingTtl)
            redis.call('SET', statusKey, 'QUEUED', 'EX', statusTtl)
            return stock - 1
            """,
            Long.class
    );

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OrderIdGenerator orderIdGenerator;

    @Autowired
    private SeckillOrderProducer seckillOrderProducer;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    public SubmitOrderResult submitSeckillOrder(Long userId, String productName) {
        if (productName == null || productName.isBlank()) {
            return SubmitOrderResult.failed("product_name must not be blank");
        }

        Product product = productMapper.findByName(productName.trim());
        if (product == null) {
            return SubmitOrderResult.failed("Product not found");
        }

        Long productId = product.getId();
        if (productId == null) {
            return SubmitOrderResult.failed("Product id is missing");
        }

        long orderId = orderIdGenerator.nextId();
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                List.of(
                        stockKey(productId),
                        orderedUsersKey(productId),
                        pendingOrderKey(userId, productId),
                        orderStatusKey(orderId)
                ),
                String.valueOf(userId),
                String.valueOf(orderId),
                String.valueOf(PENDING_ORDER_TTL.toSeconds()),
                String.valueOf(ORDER_STATUS_TTL.toSeconds())
        );

        if (result == null) {
            return SubmitOrderResult.failed("Failed to place seckill order");
        }
        if (result == -3L) {
            return SubmitOrderResult.failed("Stock cache is not ready");
        }
        if (result == -2L) {
            return SubmitOrderResult.failed("Each user can only place one seckill order for this product");
        }
        if (result == -1L) {
            return SubmitOrderResult.failed("Product is sold out");
        }

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(userId);
        message.setProductId(productId);

        try {
            seckillOrderProducer.send(message);
        } catch (Exception e) {
            cancelReservedOrder(message, "KafkaSendFailed");
            log.error("Failed to send seckill order message to Kafka, orderId={}", orderId, e);
            return SubmitOrderResult.failed("Failed to queue order request");
        }

        return SubmitOrderResult.success(orderId, product.getName());
    }

    @Transactional(readOnly = true)
    public SeckillOrder findByOrderId(Long orderId) {
        return orderMapper.findByOrderNo(orderId);
    }

    @Transactional(readOnly = true)
    public List<SeckillOrder> findByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    public String getOrderStatus(Long orderId) {
        return stringRedisTemplate.opsForValue().get(orderStatusKey(orderId));
    }

    @Transactional
    public void createOrder(SeckillOrderMessage message) {
        SeckillOrder existingByOrderId = orderMapper.findByOrderNo(message.getOrderId());
        if (existingByOrderId != null) {
            markOrderCreated(existingByOrderId.getOrderNo());
            clearPendingOrder(message);
            return;
        }

        SeckillOrder existingByUserProduct = orderMapper.findByUserIdAndProductId(message.getUserId(), message.getProductId());
        if (existingByUserProduct != null) {
            markOrderCreated(existingByUserProduct.getOrderNo());
            clearPendingOrder(message);
            return;
        }

        Product product = productMapper.findById(message.getProductId());
        if (product == null) {
            throw new IllegalStateException("Product not found");
        }

        int updatedRows = productMapper.deductStock(message.getProductId());
        if (updatedRows <= 0) {
            throw new IllegalStateException("Database stock is insufficient");
        }

        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setOrderAmount(product.getPrice());
        order.setStatus("CREATED");
        orderMapper.insert(order);

        log.info("Seckill order created successfully, orderId={}, userId={}, productId={}",
                message.getOrderId(), message.getUserId(), message.getProductId());
    }

    public void markOrderCreated(Long orderId) {
        stringRedisTemplate.opsForValue().set(orderStatusKey(orderId), "CREATED", ORDER_STATUS_TTL);
    }

    public void markOrderFailed(SeckillOrderMessage message, String reason) {
        cancelReservedOrder(message, reason);
    }

    public void clearPendingOrder(SeckillOrderMessage message) {
        stringRedisTemplate.delete(pendingOrderKey(message.getUserId(), message.getProductId()));
    }

    public void cancelReservedOrder(SeckillOrderMessage message, String reason) {
        stringRedisTemplate.opsForValue().set(orderStatusKey(message.getOrderId()), "FAILED:" + reason, ORDER_STATUS_TTL);
        stringRedisTemplate.opsForSet().remove(orderedUsersKey(message.getProductId()), String.valueOf(message.getUserId()));
        stringRedisTemplate.opsForValue().increment(stockKey(message.getProductId()));
        stringRedisTemplate.delete(pendingOrderKey(message.getUserId(), message.getProductId()));
    }

    public static String stockKey(Long productId) {
        return "seckill:stock:" + productId;
    }

    public static String orderedUsersKey(Long productId) {
        return "seckill:ordered:product:" + productId;
    }

    public static String pendingOrderKey(Long userId, Long productId) {
        return "seckill:pending:user:" + userId + ":product:" + productId;
    }

    public static String orderStatusKey(Long orderId) {
        return "seckill:order:status:" + orderId;
    }

    public static class SubmitOrderResult {
        private final boolean success;
        private final String message;
        private final Long orderId;
        private final String productName;

        private SubmitOrderResult(boolean success, String message, Long orderId, String productName) {
            this.success = success;
            this.message = message;
            this.orderId = orderId;
            this.productName = productName;
        }

        public static SubmitOrderResult success(Long orderId, String productName) {
            return new SubmitOrderResult(true, "Order request accepted", orderId, productName);
        }

        public static SubmitOrderResult failed(String message) {
            return new SubmitOrderResult(false, message, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Long getOrderId() {
            return orderId;
        }

        public String getProductName() {
            return productName;
        }
    }
}
