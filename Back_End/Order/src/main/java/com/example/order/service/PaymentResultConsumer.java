package com.example.order.service;

import com.example.order.model.PaymentResultMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentResultConsumer {

    private final JsonUtil jsonUtil;
    private final OrderService orderService;

    public PaymentResultConsumer(JsonUtil jsonUtil, OrderService orderService) {
        this.jsonUtil = jsonUtil;
        this.orderService = orderService;
    }

    @KafkaListener(topics = SeckillOrderProducer.PAYMENT_RESULT_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        PaymentResultMessage result = jsonUtil.fromJson(payload, PaymentResultMessage.class);
        orderService.handlePaymentResult(result);
    }
}
