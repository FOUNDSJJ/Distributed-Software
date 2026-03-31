package com.example.inventory.mapper;

import com.example.inventory.model.InventoryTxLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryTxLogMapper {
    String findStatusByOrderNo(@Param("orderNo") Long orderNo);

    InventoryTxLog findByOrderNo(@Param("orderNo") Long orderNo);

    int insert(@Param("orderNo") Long orderNo,
               @Param("productId") Long productId,
               @Param("status") String status,
               @Param("reason") String reason);

    int updateStatus(@Param("orderNo") Long orderNo,
                     @Param("status") String status,
                     @Param("reason") String reason);
}
