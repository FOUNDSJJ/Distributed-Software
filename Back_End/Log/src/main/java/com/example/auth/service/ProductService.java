package com.example.auth.service;

import com.example.auth.mapper.ProductMapper;
import com.example.auth.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    public Product getProductById(Long id) {
        String cacheKey = PRODUCT_CACHE_PREFIX + id;

        // 1. 查缓存
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            if (NULL_VALUE.equals(cachedValue)) {
                return null;
            }
            return jsonUtil.fromJson(cachedValue, Product.class);
        }

        // 2. 处理缓存击穿：加互斥锁
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

            // 3. 处理缓存穿透：缓存空值
            if (product == null) {
                stringRedisTemplate.opsForValue().set(cacheKey, NULL_VALUE, Duration.ofMinutes(2));
                return null;
            }

            // 4. 处理缓存雪崩：加随机过期时间
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
}