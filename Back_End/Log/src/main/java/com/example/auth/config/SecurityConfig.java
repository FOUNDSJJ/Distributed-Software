package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 启用 CORS
                .cors().and()

                // 关闭 CSRF（因为你是 REST API，用 SESSIONID 或 token）
                .csrf().disable()

                // 配置请求权限
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/**").permitAll()   // 放行注册/登录接口
                        .anyRequest().authenticated()              // 其他接口需要认证
                )

                // 禁用默认 Basic Auth
                .httpBasic().disable()

                // 禁用表单登录（Spring Boot 默认会启用表单登录）
                .formLogin().disable();

        return http.build();
    }

    // 全局 CORS 配置 Bean
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // 是否允许携带 Cookie
        config.addAllowedOriginPattern("*"); // 允许所有来源
        config.setAllowCredentials(true);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 允许方法
        config.setAllowedHeaders(Arrays.asList("*")); // 允许请求头

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 所有接口都允许跨域
        return new CorsFilter(source);
    }
}