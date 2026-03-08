package com.example.auth.service;

import com.example.auth.mapper.UserMapper;
import com.example.auth.model.User;
import com.example.auth.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 根据手机号查询用户
     */
    public User findByPhoneNumber(String phoneNumber) {
        return userMapper.findByPhoneNumber(phoneNumber);
    }

    /**
     * 注册新用户
     * @return 插入是否成功
     */
    public boolean registerUser(String username, String phoneNumber, String rawPassword) {
        // 1. 检查手机号是否已注册
        if (userMapper.findByPhoneNumber(phoneNumber) != null) {
            return false; // 手机号已注册
        }

        // 2. 创建 User 对象
        User user = new User();
        user.setUsername(username);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(PasswordUtil.hashPassword(rawPassword));
        user.setStatus(1); // 默认启用
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        // 3. 插入数据库
        return userMapper.insertUser(user) > 0;
    }

    /**
     * 验证登录
     * @return 用户对象，如果验证失败返回 null
     */
    public User validateLogin(String phoneNumber, String rawPassword) {
        User user = userMapper.findByPhoneNumber(phoneNumber);
        if (user == null || user.getStatus() != 1) {
            return null; // 用户不存在或已禁用
        }

        // 验证密码
        if (!PasswordUtil.verifyPassword(rawPassword, user.getPasswordHash())) {
            return null; // 密码错误
        }

        // 更新最后登录时间
        userMapper.updateLastLogin(user.getId(), new Timestamp(System.currentTimeMillis()));

        return user;
    }
}