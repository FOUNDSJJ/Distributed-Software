package com.example.auth.mapper;

import com.example.auth.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    User findByUserName(@Param("username") String username);
    int insertUser(User user);
    int updateLastLogin(@Param("id") Long id, @Param("lastLogin") java.sql.Timestamp lastLogin);
}