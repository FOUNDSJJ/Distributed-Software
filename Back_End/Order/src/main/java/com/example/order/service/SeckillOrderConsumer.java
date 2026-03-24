package com.example.order.service;

import com.example.order.model.SeckillOrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    @Autowired
    private JsonUtil jsonUtil;

    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = SeckillOrderProducer.TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        SeckillOrderMessage message = jsonUtil.fromJson(payload, SeckillOrderMessage.class);

        try {
            orderService.createOrder(message);
            orderService.markOrderCreated(message.getOrderId());
            orderService.clearPendingOrder(message);
        } catch (Exception e) {
            log.error("Failed to create seckill order, orderId={}", message.getOrderId(), e);
            orderService.markOrderFailed(message, e.getClass().getSimpleName());
        }
    }
}
