package com.example.order.mapper;

import com.example.order.model.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    int insert(SeckillOrder order);

    SeckillOrder findByOrderNo(@Param("orderNo") Long orderNo);

    SeckillOrder findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    List<SeckillOrder> findByUserId(@Param("userId") Long userId);

    int updateStatus(@Param("orderNo") Long orderNo, @Param("status") String status);

    int updateStatusIfCurrent(@Param("orderNo") Long orderNo,
                              @Param("currentStatus") String currentStatus,
                              @Param("targetStatus") String targetStatus);

    List<SeckillOrder> findReservedOrders();
}
