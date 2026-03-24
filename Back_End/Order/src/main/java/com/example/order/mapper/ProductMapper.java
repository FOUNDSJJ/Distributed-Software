package com.example.order.mapper;

import com.example.order.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    Product findById(@Param("id") Long id);

    int deductStock(@Param("id") Long id);

    List<Product> findAll();
}
