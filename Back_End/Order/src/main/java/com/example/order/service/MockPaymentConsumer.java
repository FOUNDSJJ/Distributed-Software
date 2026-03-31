package com.example.order.service;

import com.example.order.model.PaymentRequestMessage;
import com.example.order.model.PaymentResultMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MockPaymentConsumer {

    private final JsonUtil jsonUtil;
    private final SeckillOrderProducer producer;

    public MockPaymentConsumer(JsonUtil jsonUtil, SeckillOrderProducer producer) {
        this.jsonUtil = jsonUtil;
        this.producer = producer;
    }

    @KafkaListener(topics = SeckillOrderProducer.PAYMENT_REQUEST_TOPIC, groupId = "${spring.kafka.payment-simulator.group-id}")
    public void consume(String payload) {
        PaymentRequestMessage request = jsonUtil.fromJson(payload, PaymentRequestMessage.class);

        PaymentResultMessage result = new PaymentResultMessage();
        result.setOrderId(request.getOrderId());
        result.setSuccess(true);
        result.setReason("PAYMENT_CONFIRMED");
        producer.sendPaymentResult(result);
    }
}
