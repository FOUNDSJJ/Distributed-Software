package com.example.auth.controller;

import com.example.auth.model.Product;
import com.example.auth.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/{id}")
    public Map<String, Object> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Map.of("success", false, "message", "商品不存在");
        }
        return Map.of("success", true, "data", product);
    }
}