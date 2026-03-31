package com.example.inventory.service;

import com.example.inventory.mapper.InventoryTxLogMapper;
import com.example.inventory.mapper.ProductMapper;
import com.example.inventory.model.InventoryTxLog;
import com.example.inventory.model.Product;
import com.example.inventory.model.SeckillOrderMessage;
import com.example.inventory.model.StockDeductionResultMessage;
import com.example.inventory.model.StockRollbackMessage;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final String STATUS_DEDUCTED = "DEDUCTED";
    private static final String STATUS_ROLLED_BACK = "ROLLED_BACK";

    private final ProductMapper productMapper;
    private final InventoryTxLogMapper inventoryTxLogMapper;

    public InventoryService(ProductMapper productMapper, InventoryTxLogMapper inventoryTxLogMapper) {
        this.productMapper = productMapper;
        this.inventoryTxLogMapper = inventoryTxLogMapper;
    }

    @Transactional
    public StockDeductionResultMessage deduct(SeckillOrderMessage message) {
        // 先依据事务日志判断是否重复扣减，保证消息消费幂等
        String currentStatus = inventoryTxLogMapper.findStatusByOrderNo(message.getOrderId());
        if (STATUS_DEDUCTED.equals(currentStatus)) {
            return buildResult(message, true, "ALREADY_DEDUCTED");
        }
        if (STATUS_ROLLED_BACK.equals(currentStatus)) {
            return buildResult(message, false, "ALREADY_ROLLED_BACK");
        }

        int updated = productMapper.deductStock(message.getProductId());
        if (updated <= 0) {
            // 库存不足时记录失败原因，便于订单服务回写状态
            insertOrUpdateLog(message.getOrderId(), message.getProductId(), "DEDUCT_FAILED", "INSUFFICIENT_STOCK");
            return buildResult(message, false, "INSUFFICIENT_STOCK");
        }

        insertOrUpdateLog(message.getOrderId(), message.getProductId(), STATUS_DEDUCTED, null);
        return buildResult(message, true, "STOCK_DEDUCTED");
    }

    @Transactional
    public void rollback(StockRollbackMessage message) {
        // 仅对已经成功扣减的订单执行库存回滚
        String currentStatus = inventoryTxLogMapper.findStatusByOrderNo(message.getOrderId());
        if (!STATUS_DEDUCTED.equals(currentStatus)) {
            return;
        }

        productMapper.increaseStock(message.getProductId());
        inventoryTxLogMapper.updateStatus(message.getOrderId(), STATUS_ROLLED_BACK, message.getReason());
    }

    private void insertOrUpdateLog(Long orderId, Long productId, String status, String reason) {
        try {
            inventoryTxLogMapper.insert(orderId, productId, status, reason);
        } catch (DuplicateKeyException ex) {
            // 并发重复写入时改为更新事务日志状态
            inventoryTxLogMapper.updateStatus(orderId, status, reason);
        }
    }

    private StockDeductionResultMessage buildResult(SeckillOrderMessage message, boolean success, String reason) {
        StockDeductionResultMessage result = new StockDeductionResultMessage();
        result.setOrderId(message.getOrderId());
        result.setUserId(message.getUserId());
        result.setProductId(message.getProductId());
        result.setSuccess(success);
        result.setReason(reason);
        return result;
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        return productMapper.findById(productId);
    }

    @Transactional(readOnly = true)
    public InventoryTxLog getTxLog(Long orderId) {
        return inventoryTxLogMapper.findByOrderNo(orderId);
    }
}
