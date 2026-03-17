package com.example.auth.mapper;

import com.example.auth.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    Product findById(@Param("id") Long id);

    Product findByName(@Param("name") String name);

    List<Product> findAll();
}
