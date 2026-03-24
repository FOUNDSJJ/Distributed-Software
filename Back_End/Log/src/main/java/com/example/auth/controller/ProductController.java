package com.example.auth.controller;

import com.example.auth.model.Product;
import com.example.auth.service.ProductReplicationService;
import com.example.auth.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductReplicationService productReplicationService;

    @GetMapping("/info")
    public Map<String, Object> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        if (products == null || products.isEmpty()) {
            return Map.of("success", false, "message", "No products found");
        }

        Map<String, Object> datas = new LinkedHashMap<>();
        for (int i = 0; i < products.size(); i++) {
            datas.put("product_" + (i + 1), products.get(i));
        }

        return Map.of(
                "number", products.size(),
                "success", true,
                "datas", datas
        );
    }

    @GetMapping("/{id:\\d+}")
    public Map<String, Object> getProductById(@PathVariable("id") Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Map.of("success", false, "message", "Product not found");
        }
        return Map.of("success", true, "data", product);
    }

    @GetMapping("/by-name")
    public Map<String, Object> getProductByName(@RequestParam String name) {
        Product product = productService.getProductByName(name);
        if (product == null) {
            return Map.of("success", false, "message", "Product not found");
        }
        return Map.of("success", true, "data", product);
    }

    @GetMapping("/replication-status/{id:\\d+}")
    public Map<String, Object> getReplicationStatus(@PathVariable("id") Long id) {
        return Map.of(
                "success", true,
                "data", productReplicationService.inspectProduct(id)
        );
    }

    @PostMapping("/replication-test/{id:\\d+}")
    public Map<String, Object> testReplication(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> request
    ) {
        Object stockValue = request.get("stock");
        if (!(stockValue instanceof Number stockNumber)) {
            return Map.of("success", false, "message", "stock must be a number");
        }

        long waitMillis = 0L;
        Object waitValue = request.get("wait_millis");
        if (waitValue instanceof Number waitNumber) {
            waitMillis = Math.max(waitNumber.longValue(), 0L);
        }

        try {
            return Map.of(
                    "success", true,
                    "data", productReplicationService.updateStockAndInspect(
                            id,
                            stockNumber.intValue(),
                            waitMillis
                    )
            );
        } catch (IllegalArgumentException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}
