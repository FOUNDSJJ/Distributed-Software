package com.example.inventory.service;

import com.example.inventory.model.SeckillOrderMessage;
import com.example.inventory.model.StockDeductionResultMessage;
import com.example.inventory.model.StockRollbackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryConsumer.class);

    private final JsonUtil jsonUtil;
    private final InventoryService inventoryService;
    private final InventoryMessageProducer producer;

    public InventoryConsumer(JsonUtil jsonUtil, InventoryService inventoryService, InventoryMessageProducer producer) {
        this.jsonUtil = jsonUtil;
        this.inventoryService = inventoryService;
        this.producer = producer;
    }

    @KafkaListener(topics = InventoryMessageProducer.STOCK_DEDUCT_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeDeduct(String payload) {
        SeckillOrderMessage message = jsonUtil.fromJson(payload, SeckillOrderMessage.class);

        try {
            StockDeductionResultMessage result = inventoryService.deduct(message);
            producer.sendStockResult(result);
        } catch (Exception e) {
            log.error("Failed to deduct stock, orderId={}", message.getOrderId(), e);
            StockDeductionResultMessage result = new StockDeductionResultMessage();
            result.setOrderId(message.getOrderId());
            result.setUserId(message.getUserId());
            result.setProductId(message.getProductId());
            result.setSuccess(false);
            result.setReason("INVENTORY_ERROR");
            producer.sendStockResult(result);
        }
    }

    @KafkaListener(topics = InventoryMessageProducer.STOCK_ROLLBACK_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRollback(String payload) {
        StockRollbackMessage message = jsonUtil.fromJson(payload, StockRollbackMessage.class);
        inventoryService.rollback(message);
    }
}
