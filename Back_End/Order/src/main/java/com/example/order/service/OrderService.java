package com.example.order.service;

import com.example.order.mapper.OrderMapper;
import com.example.order.mapper.ProductMapper;
import com.example.order.model.PaymentRequestMessage;
import com.example.order.model.PaymentResultMessage;
import com.example.order.model.Product;
import com.example.order.model.SeckillOrder;
import com.example.order.model.SeckillOrderMessage;
import com.example.order.model.StockDeductionResultMessage;
import com.example.order.model.StockRollbackMessage;
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
    private static final String STATUS_PENDING_STOCK = "PENDING_STOCK";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_PAYING = "PAYING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PAY_FAILED = "PAY_FAILED";
    private static final String STATUS_FAILED = "FAILED";
    private static final Duration ORDER_STATUS_TTL = Duration.ofHours(24);
    private static final Duration PENDING_ORDER_TTL = Duration.ofMinutes(30);
    // 通过 Lua 脚本将库存校验、去重和占位写入合并为原子操作
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
        // 下单入口：先校验商品，再在 Redis 中做原子预占
        if (productName == null || productName.isBlank()) {
            return SubmitOrderResult.failed("商品名称不能为空");
        }

        Product product = productMapper.findByName(productName.trim());
        if (product == null) {
            return SubmitOrderResult.failed("商品不存在");
        }

        Long productId = product.getId();
        if (productId == null) {
            return SubmitOrderResult.failed("商品编号缺失");
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
            return SubmitOrderResult.failed("下达秒杀订单指令失败");
        }
        if (result == -3L) {
            return SubmitOrderResult.failed("库存缓存尚未准备就绪");
        }
        if (result == -2L) {
            return SubmitOrderResult.failed("每个用户只能为此产品下单一次");
        }
        if (result == -1L) {
            return SubmitOrderResult.failed("商品已售空");
        }

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(userId);
        message.setProductId(productId);
        message.setProductName(product.getName());
        message.setOrderAmount(product.getPrice());

        try {
            // 先落库为待扣减状态，保证后续异步处理可追踪
            createPendingOrder(message);
        } catch (Exception e) {
            cancelReservedOrder(message, "CreatePendingOrderFailed");
            log.error("Failed to create pending order, orderId={}", orderId, e);
            return SubmitOrderResult.failed("保存待处理订单失败");
        }

        try {
            // 再发送库存扣减消息，交给库存服务继续处理
            seckillOrderProducer.sendStockDeduction(message);
        } catch (Exception e) {
            cancelReservedOrder(message, "KafkaSendFailed");
            log.error("Failed to send seckill order message to Kafka, orderId={}", orderId, e);
            return SubmitOrderResult.failed("提交订单请求失败");
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
    public void createPendingOrder(SeckillOrderMessage message) {
        // 订单初始状态为待扣库存，等待库存服务回执
        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setOrderAmount(message.getOrderAmount());
        order.setStatus(STATUS_PENDING_STOCK);
        orderMapper.insert(order);
        markRedisOrderStatus(message.getOrderId(), STATUS_PENDING_STOCK);
    }

    @Transactional
    public void confirmOrderAfterStockSuccess(StockDeductionResultMessage message) {
        // 扣库存成功后将订单推进到可支付状态
        int updated = orderMapper.updateStatusIfCurrent(message.getOrderId(), STATUS_PENDING_STOCK, STATUS_CREATED);
        if (updated <= 0) {
            SeckillOrder order = orderMapper.findByOrderNo(message.getOrderId());
            if (order == null || !STATUS_CREATED.equals(order.getStatus())) {
                throw new IllegalStateException("订单状态不是待扣减库存");
            }
        }
        markRedisOrderStatus(message.getOrderId(), STATUS_CREATED);
        clearPendingOrder(message.getUserId(), message.getProductId());
    }

    @Transactional
    public void failOrderAfterStockFailure(StockDeductionResultMessage message) {
        // 扣库存失败时关闭订单并释放预占资源
        orderMapper.updateStatus(message.getOrderId(), STATUS_FAILED);
        cancelReservedOrder(message.getOrderId(), message.getUserId(), message.getProductId(), message.getReason());
    }

    @Transactional
    public PayOrderResult payOrder(Long userId, Long orderId) {
        // 支付前必须确认订单存在、归属正确且状态允许支付
        SeckillOrder order = orderMapper.findByOrderNo(orderId);
        if (order == null) {
            return PayOrderResult.failed("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return PayOrderResult.failed("无权支付该订单");
        }
        if (!STATUS_CREATED.equals(order.getStatus()) && !STATUS_PAY_FAILED.equals(order.getStatus())) {
            return PayOrderResult.failed("当前订单状态不允许支付");
        }

        String previousStatus = order.getStatus();
        int updated = orderMapper.updateStatusIfCurrent(orderId, previousStatus, STATUS_PAYING);
        if (updated <= 0) {
            return PayOrderResult.failed("订单状态已变化，请重试");
        }
        // 先将状态切到支付中，再发起异步支付请求
        markRedisOrderStatus(orderId, STATUS_PAYING);

        PaymentRequestMessage request = new PaymentRequestMessage();
        request.setOrderId(orderId);
        request.setUserId(order.getUserId());
        request.setProductId(order.getProductId());
        request.setOrderAmount(order.getOrderAmount());

        try {
            seckillOrderProducer.sendPaymentRequest(request);
            return PayOrderResult.success("支付请求已提交，正在处理中");
        } catch (Exception e) {
            orderMapper.updateStatus(orderId, previousStatus);
            markRedisOrderStatus(orderId, previousStatus);
            log.error("Failed to send payment request, orderId={}", orderId, e);
            return PayOrderResult.failed("提交支付请求失败");
        }
    }

    @Transactional
    public void handlePaymentResult(PaymentResultMessage result) {
        // 支付回执只处理处于支付中的订单，避免状态回退
        SeckillOrder order = orderMapper.findByOrderNo(result.getOrderId());
        if (order == null) {
            return;
        }
        if (STATUS_PAID.equals(order.getStatus())) {
            markRedisOrderStatus(order.getOrderNo(), STATUS_PAID);
            return;
        }
        if (!STATUS_PAYING.equals(order.getStatus())) {
            return;
        }

        String targetStatus = result.isSuccess() ? STATUS_PAID : STATUS_PAY_FAILED;
        orderMapper.updateStatus(result.getOrderId(), targetStatus);
        markRedisOrderStatus(result.getOrderId(), targetStatus);
    }

    public void compensateStockRollback(Long orderId, Long productId, String reason) {
        StockRollbackMessage rollbackMessage = new StockRollbackMessage();
        rollbackMessage.setOrderId(orderId);
        rollbackMessage.setProductId(productId);
        rollbackMessage.setReason(reason);
        seckillOrderProducer.sendStockRollback(rollbackMessage);
    }

    public void markOrderCreated(Long orderId) {
        markRedisOrderStatus(orderId, STATUS_CREATED);
    }

    public void markOrderFailed(SeckillOrderMessage message, String reason) {
        cancelReservedOrder(message, reason);
    }

    public void clearPendingOrder(SeckillOrderMessage message) {
        clearPendingOrder(message.getUserId(), message.getProductId());
    }

    public void cancelReservedOrder(SeckillOrderMessage message, String reason) {
        cancelReservedOrder(message.getOrderId(), message.getUserId(), message.getProductId(), reason);
    }

    public void markRedisOrderStatus(Long orderId, String status) {
        stringRedisTemplate.opsForValue().set(orderStatusKey(orderId), status, ORDER_STATUS_TTL);
    }

    public void clearPendingOrder(Long userId, Long productId) {
        stringRedisTemplate.delete(pendingOrderKey(userId, productId));
    }

    public void cancelReservedOrder(Long orderId, Long userId, Long productId, String reason) {
        // 取消预占时同步回滚去重集合、库存缓存和待处理标记
        markRedisOrderStatus(orderId, STATUS_FAILED + ":" + reason);
        stringRedisTemplate.opsForSet().remove(orderedUsersKey(productId), String.valueOf(userId));
        stringRedisTemplate.opsForValue().increment(stockKey(productId));
        clearPendingOrder(userId, productId);
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
            return new SubmitOrderResult(true, "下单请求已受理", orderId, productName);
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

    public static class PayOrderResult {
        private final boolean success;
        private final String message;

        private PayOrderResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static PayOrderResult success(String message) {
            return new PayOrderResult(true, message);
        }

        public static PayOrderResult failed(String message) {
            return new PayOrderResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
