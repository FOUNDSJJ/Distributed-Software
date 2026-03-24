package com.example.order.service;

import com.example.order.mapper.ProductMapper;
import com.example.order.model.Product;
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
    }
}
