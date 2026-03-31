package com.example.order.service;

import com.example.order.model.PaymentRequestMessage;
import com.example.order.model.PaymentResultMessage;
import com.example.order.model.SeckillOrderMessage;
import com.example.order.model.StockDeductionResultMessage;
import com.example.order.model.StockRollbackMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SeckillOrderProducer {

    public static final String STOCK_DEDUCT_TOPIC = "seckill-stock-deduct-topic";
    public static final String STOCK_RESULT_TOPIC = "seckill-stock-result-topic";
    public static final String STOCK_ROLLBACK_TOPIC = "seckill-stock-rollback-topic";
    public static final String PAYMENT_REQUEST_TOPIC = "seckill-payment-request-topic";
    public static final String PAYMENT_RESULT_TOPIC = "seckill-payment-result-topic";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JsonUtil jsonUtil;

    public void sendStockDeduction(SeckillOrderMessage message) {
        send(STOCK_DEDUCT_TOPIC, message.getProductId(), message);
    }

    public void sendStockResult(StockDeductionResultMessage message) {
        send(STOCK_RESULT_TOPIC, message.getProductId(), message);
    }

    public void sendStockRollback(StockRollbackMessage message) {
        send(STOCK_ROLLBACK_TOPIC, message.getProductId(), message);
    }

    public void sendPaymentRequest(PaymentRequestMessage message) {
        send(PAYMENT_REQUEST_TOPIC, message.getOrderId(), message);
    }

    public void sendPaymentResult(PaymentResultMessage message) {
        send(PAYMENT_RESULT_TOPIC, message.getOrderId(), message);
    }

    private void send(String topic, Object key, Object message) {
        try {
            SendResult<String, String> result = kafkaTemplate
                    .send(topic, String.valueOf(key), jsonUtil.toJson(message))
                    .get(5, TimeUnit.SECONDS);

            if (result == null || result.getRecordMetadata() == null) {
                throw new IllegalStateException("Kafka send returned empty metadata");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send message to Kafka", e);
        }
    }
}
