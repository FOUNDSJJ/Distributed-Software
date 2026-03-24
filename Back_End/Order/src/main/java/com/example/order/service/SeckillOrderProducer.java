package com.example.order.service;

import com.example.order.model.SeckillOrderMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SeckillOrderProducer {

    public static final String TOPIC = "seckill-order-topic";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JsonUtil jsonUtil;

    public void send(SeckillOrderMessage message) {
        try {
            SendResult<String, String> result = kafkaTemplate
                    .send(TOPIC, String.valueOf(message.getProductId()), jsonUtil.toJson(message))
                    .get(5, TimeUnit.SECONDS);

            if (result == null || result.getRecordMetadata() == null) {
                throw new IllegalStateException("Kafka send returned empty metadata");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send message to Kafka", e);
        }
    }
}
