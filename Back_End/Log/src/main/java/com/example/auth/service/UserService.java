package com.example.auth.service;

import com.example.auth.mapper.UserMapper;
import com.example.auth.model.User;
import com.example.auth.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Transactional(readOnly = true)
    public User findByPhoneNumber(String phoneNumber) {
        return userMapper.findByPhoneNumber(phoneNumber);
    }

    @Transactional(readOnly = true)
    public User findByUserName(String username) {
        return userMapper.findByUserName(username);
    }

    @Transactional
    public boolean registerUser(String username, String phoneNumber, String rawPassword) {
        // 再次校验唯一性，避免并发场景下写入重复用户
        if (userMapper.findByUserName(username) != null) {
            return false;
        }

        if (userMapper.findByPhoneNumber(phoneNumber) != null) {
            return false;
        }

        User user = new User();
        user.setUsername(username);
        user.setPhoneNumber(phoneNumber);
        // 数据库存储密码摘要，不直接保存明文密码
        user.setPasswordHash(PasswordUtil.hashPassword(rawPassword));
        user.setStatus(1);
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return userMapper.insertUser(user) > 0;
    }

    @Transactional
    public User validateLogin(String username, String rawPassword) {
        // 登录校验依次检查用户存在性、状态和密码摘要
        User user = userMapper.findByUserName(username);
        if (user == null) {
            return null;
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            return null;
        }

        if (!PasswordUtil.verifyPassword(rawPassword, user.getPasswordHash())) {
            return null;
        }

        // 登录成功后刷新最后登录时间
        userMapper.updateLastLogin(user.getId(), new Timestamp(System.currentTimeMillis()));
        return user;
    }
}
