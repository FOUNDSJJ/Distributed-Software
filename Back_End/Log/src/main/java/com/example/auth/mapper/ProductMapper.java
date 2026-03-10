package com.example.auth.mapper;

import com.example.auth.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper {
    Product findById(@Param("id") Long id);
}