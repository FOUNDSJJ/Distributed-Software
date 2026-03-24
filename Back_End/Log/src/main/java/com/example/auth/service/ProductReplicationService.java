package com.example.auth.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductReplicationService {

    private final JdbcTemplate writeJdbcTemplate;
    private final JdbcTemplate readJdbcTemplate;

    public ProductReplicationService(
            @Qualifier("writeJdbcTemplate") JdbcTemplate writeJdbcTemplate,
            @Qualifier("readJdbcTemplate") JdbcTemplate readJdbcTemplate
    ) {
        this.writeJdbcTemplate = writeJdbcTemplate;
        this.readJdbcTemplate = readJdbcTemplate;
    }

    public Map<String, Object> inspectProduct(Long productId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("writeNode", queryNode(writeJdbcTemplate));
        result.put("writeData", queryProduct(writeJdbcTemplate, productId));
        result.put("readNode", queryNode(readJdbcTemplate));
        result.put("readData", queryProduct(readJdbcTemplate, productId));
        return result;
    }

    public Map<String, Object> updateStockAndInspect(Long productId, Integer stock, long waitMillis) {
        int updatedRows = writeJdbcTemplate.update(
                "UPDATE products SET stock = ?, updated_at = NOW() WHERE id = ?",
                stock,
                productId
        );

        if (updatedRows == 0) {
            throw new IllegalArgumentException("Product does not exist: " + productId);
        }

        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Map<String, Object> result = inspectProduct(productId);
        result.put("updatedRows", updatedRows);
        result.put("requestedStock", stock);
        result.put("waitMillis", waitMillis);
        return result;
    }

    private Map<String, Object> queryNode(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForMap(
                """
                SELECT
                    @@server_id AS serverId,
                    @@hostname AS hostName,
                    @@read_only AS readOnly,
                    NOW() AS databaseTime
                """
        );
    }

    private Map<String, Object> queryProduct(JdbcTemplate jdbcTemplate, Long productId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT id, name, price, stock, description, created_at, updated_at
                FROM products
                WHERE id = ?
                """,
                productId
        );

        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }
}
