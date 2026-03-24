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
        if (userMapper.findByUserName(username) != null) {
            return false;
        }

        if (userMapper.findByPhoneNumber(phoneNumber) != null) {
            return false;
        }

        User user = new User();
        user.setUsername(username);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(PasswordUtil.hashPassword(rawPassword));
        user.setStatus(1);
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return userMapper.insertUser(user) > 0;
    }

    @Transactional
    public User validateLogin(String username, String rawPassword) {
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

        userMapper.updateLastLogin(user.getId(), new Timestamp(System.currentTimeMillis()));
        return user;
    }
}
