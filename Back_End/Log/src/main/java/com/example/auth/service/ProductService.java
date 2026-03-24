package com.example.auth.service;

import com.example.auth.mapper.ProductMapper;
import com.example.auth.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductService {

    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String PRODUCT_LOCK_PREFIX = "lock:product:";
    private static final String NULL_VALUE = "NULL";

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JsonUtil jsonUtil;

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        String cacheKey = PRODUCT_CACHE_PREFIX + id;

        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            if (NULL_VALUE.equals(cachedValue)) {
                return null;
            }
            return jsonUtil.fromJson(cachedValue, Product.class);
        }

        String lockKey = PRODUCT_LOCK_PREFIX + id;
        boolean lock = Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10))
        );

        if (!lock) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String retryValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (retryValue != null) {
                if (NULL_VALUE.equals(retryValue)) {
                    return null;
                }
                return jsonUtil.fromJson(retryValue, Product.class);
            }
            return productMapper.findById(id);
        }

        try {
            Product product = productMapper.findById(id);

            if (product == null) {
                stringRedisTemplate.opsForValue().set(cacheKey, NULL_VALUE, Duration.ofMinutes(2));
                return null;
            }

            int randomMinutes = ThreadLocalRandom.current().nextInt(30, 41);
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    jsonUtil.toJson(product),
                    Duration.ofMinutes(randomMinutes)
            );

            return product;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Transactional(readOnly = true)
    public Product getProductByName(String name) {
        return productMapper.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productMapper.findAll();
    }

    @Transactional(readOnly = true)
    public Integer getProductNumber() {
        return getAllProducts().size();
    }
}
