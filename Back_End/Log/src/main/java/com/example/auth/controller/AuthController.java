package com.example.auth.controller;

import com.example.auth.mapper.UserMapper;
import com.example.auth.model.User;
import com.example.auth.service.SessionService;
import com.example.auth.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SessionService sessionService;

    @PostMapping("/register")
    public Map<String,Object> register(@RequestBody Map<String,String> req){
        String username = req.get("username");
        String phone = req.get("phone_number");
        String password = req.get("password");

        if(userMapper.findByUserName(username) != null){
            return Map.of("success", false, "message", "用户名已经存在");
        }

        if(userMapper.findByPhoneNumber(phone) != null){
            return Map.of("success", false, "message", "手机号已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPhoneNumber(phone);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setStatus(1);

        userMapper.insertUser(user);

        return Map.of("success", true, "message", "注册成功");
    }

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,String> req, HttpServletResponse response){
        String username = req.get("username");
        String password = req.get("password");

        User user = userMapper.findByUserName(username);
        if (user == null){
            return Map.of("success", false, "message", "用户名不存在");
        }

        if(user.getStatus() != 1){
            return Map.of("success", false, "message", "用户名处于不可用状态");
        }

        if(!PasswordUtil.verifyPassword(password, user.getPasswordHash())){
            return Map.of("success", false, "message", "密码不正确");
        }

        // 更新最后登录时间
        userMapper.updateLastLogin(user.getId(), new Timestamp(System.currentTimeMillis()));

        // 创建 session / token
        String token = sessionService.createSession(user.getId());

        // 设置 Cookie
        Cookie cookie = new Cookie("SESSIONID", token);
        cookie.setHttpOnly(true);   // JS 无法访问，提高安全性
        cookie.setSecure(false);    // 开发阶段 false, 生产环境应为 true
        cookie.setPath("/");        // 整个域名有效
        cookie.setMaxAge(24*3600);  // 1 天
        response.addCookie(cookie);

        // 返回 JSON，包括登录状态和用户信息
        Map<String,Object> userInfo = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "phone_number", user.getPhoneNumber()
        );

        return Map.of(
                "success", true,
                "message", "登录成功",
                "user", userInfo
        );
    }
}