package com.example.order.service;

import com.example.order.mapper.ProductMapper;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Product;
import com.example.order.model.SeckillOrder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockWarmupService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OrderMapper orderMapper;

    @PostConstruct
    @Transactional(readOnly = true)
    public void warmUpStock() {
        List<Product> products = productMapper.findAll();
        for (Product product : products) {
            if (product.getId() == null || product.getStock() == null) {
                continue;
            }
            stringRedisTemplate.opsForValue().set(
                    OrderService.stockKey(product.getId()),
                    String.valueOf(Math.max(product.getStock(), 0))
            );
        }

        List<SeckillOrder> reservedOrders = orderMapper.findReservedOrders();
        for (SeckillOrder order : reservedOrders) {
            stringRedisTemplate.opsForSet().add(
                    OrderService.orderedUsersKey(order.getProductId()),
                    String.valueOf(order.getUserId())
            );
            stringRedisTemplate.opsForValue().set(
                    OrderService.orderStatusKey(order.getOrderNo()),
                    order.getStatus()
            );
        }
    }
}
