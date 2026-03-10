package com.example.auth.mapper;

import com.example.auth.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;

@Mapper
public interface UserMapper {
    User findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    User findByUserName(@Param("username") String username);

    User findById(@Param("id") Long id);

    int insertUser(User user);

    int updateLastLogin(@Param("id") Long id, @Param("lastLogin") Timestamp lastLogin);
}