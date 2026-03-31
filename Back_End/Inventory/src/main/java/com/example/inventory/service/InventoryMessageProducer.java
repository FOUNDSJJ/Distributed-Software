package com.example.inventory.service;

import com.example.inventory.model.StockDeductionResultMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class InventoryMessageProducer {

    public static final String STOCK_DEDUCT_TOPIC = "seckill-stock-deduct-topic";
    public static final String STOCK_RESULT_TOPIC = "seckill-stock-result-topic";
    public static final String STOCK_ROLLBACK_TOPIC = "seckill-stock-rollback-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonUtil jsonUtil;

    public InventoryMessageProducer(KafkaTemplate<String, String> kafkaTemplate, JsonUtil jsonUtil) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonUtil = jsonUtil;
    }

    public void sendStockResult(StockDeductionResultMessage message) {
        try {
            SendResult<String, String> result = kafkaTemplate
                    .send(STOCK_RESULT_TOPIC, String.valueOf(message.getProductId()), jsonUtil.toJson(message))
                    .get(5, TimeUnit.SECONDS);

            if (result == null || result.getRecordMetadata() == null) {
                throw new IllegalStateException("Kafka send returned empty metadata");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send stock result message", e);
        }
    }
}
