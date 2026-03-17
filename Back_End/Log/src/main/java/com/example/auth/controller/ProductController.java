package com.example.auth.controller;

import com.example.auth.model.Product;
import com.example.auth.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/info")
    public Map<String, Object> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        if (products == null || products.isEmpty()) {
            return Map.of("success", false, "message", "仓库当中没有商品可出售");
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
            return Map.of("success", false, "message", "该商品id不存在");
        }
        return Map.of("success", true, "data", product);
    }

    @GetMapping("/{name:[a-zA-Z_][a-zA-Z0-9_]*}")
    public Map<String, Object> getProductByName(@PathVariable("name") String name) {
        Product product = productService.getProductByName(name);
        if (product == null) {
            return Map.of("success", false, "message", "该商品名不存在");
        }
        return Map.of("success", true, "data", product);
    }
}
