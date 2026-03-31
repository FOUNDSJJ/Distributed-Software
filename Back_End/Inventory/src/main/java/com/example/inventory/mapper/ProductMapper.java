package com.example.inventory.mapper;

import com.example.inventory.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper {
    Product findById(@Param("id") Long id);

    int deductStock(@Param("id") Long id);

    int increaseStock(@Param("id") Long id);
}
