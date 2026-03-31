package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.SessionService;
import com.example.auth.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> req) {
        // 注册时先读取并校验基础参数
        String username = req.get("username");
        String phone = req.get("phone_number");
        String password = req.get("password");

        if (username == null || username.isBlank()
                || phone == null || phone.isBlank()
                || password == null || password.isBlank()) {
            return Map.of("success", false, "message", "参数不能为空");
        }

        if (userService.findByUserName(username) != null) {
            return Map.of("success", false, "message", "用户名已经存在");
        }

        if (userService.findByPhoneNumber(phone) != null) {
            return Map.of("success", false, "message", "手机号已被注册");
        }

        boolean ok = userService.registerUser(username, phone, password);
        if (!ok) {
            return Map.of("success", false, "message", "注册失败");
        }

        return Map.of("success", true, "message", "注册成功");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> req, HttpServletResponse response) {
        // 登录成功后创建会话并写入 Cookie
        String username = req.get("username");
        String password = req.get("password");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return Map.of("success", false, "message", "用户名和密码不能为空");
        }

        User user = userService.validateLogin(username, password);
        if (user == null) {
            return Map.of("success", false, "message", "用户名不存在或密码错误，或账号不可用");
        }

        // 会话标识保存在 Redis，中间只向浏览器下发 token
        String token = sessionService.createSession(user.getId());

        Cookie cookie = new Cookie("SESSIONID", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 3600);
        response.addCookie(cookie);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("phone_number", user.getPhoneNumber());

        return Map.of(
                "success", true,
                "message", "登录成功",
                "user", userInfo
        );
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        // 根据 Cookie 中的会话标识回查当前登录用户
        String token = getTokenFromCookie(request);
        if (token == null) {
            return Map.of("success", false, "message", "未登录");
        }

        Long userId = sessionService.getUserIdByToken(token);
        if (userId == null) {
            return Map.of("success", false, "message", "登录已失效");
        }

        User user = userService.findById(userId);
        if (user == null) {
            return Map.of("success", false, "message", "用户不存在");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("phone_number", user.getPhoneNumber());

        return Map.of("success", true, "user", userInfo);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request, HttpServletResponse response) {
        // 退出登录时同时清理服务端会话和浏览器 Cookie
        String token = getTokenFromCookie(request);
        if (token != null) {
            sessionService.deleteSession(token);
        }

        Cookie cookie = new Cookie("SESSIONID", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return Map.of("success", true, "message", "已退出登录");
    }

    private String getTokenFromCookie(HttpServletRequest request) {
        // 统一从请求 Cookie 中提取会话 token
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("SESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
