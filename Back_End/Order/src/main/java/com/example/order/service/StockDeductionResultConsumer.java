package com.example.order.service;

import com.example.order.model.StockDeductionResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class StockDeductionResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockDeductionResultConsumer.class);

    private final JsonUtil jsonUtil;
    private final OrderService orderService;

    public StockDeductionResultConsumer(JsonUtil jsonUtil, OrderService orderService) {
        this.jsonUtil = jsonUtil;
        this.orderService = orderService;
    }

    @KafkaListener(topics = SeckillOrderProducer.STOCK_RESULT_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        StockDeductionResultMessage message = jsonUtil.fromJson(payload, StockDeductionResultMessage.class);

        try {
            if (message.isSuccess()) {
                orderService.confirmOrderAfterStockSuccess(message);
            } else {
                orderService.failOrderAfterStockFailure(message);
            }
        } catch (Exception e) {
            log.error("Failed to handle stock deduction result, orderId={}", message.getOrderId(), e);
            if (message.isSuccess()) {
                orderService.compensateStockRollback(message.getOrderId(), message.getProductId(), "OrderStatusUpdateFailed");
                StockDeductionResultMessage failedMessage = new StockDeductionResultMessage();
                failedMessage.setOrderId(message.getOrderId());
                failedMessage.setUserId(message.getUserId());
                failedMessage.setProductId(message.getProductId());
                failedMessage.setSuccess(false);
                failedMessage.setReason("OrderStatusUpdateFailed");
                orderService.failOrderAfterStockFailure(failedMessage);
            }
        }
    }
}
