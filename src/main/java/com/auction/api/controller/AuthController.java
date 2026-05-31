package com.auction.api.controller;

import com.auction.api.dto.request.LoginRequest;
import com.auction.api.dto.request.RegisterRequest;
import com.auction.api.dto.response.LoginResponse;
import com.auction.api.dto.response.UserResponse;
import com.auction.annotation.RateLimit;
import com.auction.common.Result;
import com.auction.domain.entity.User;
import com.auction.service.auth.AuthService;
import com.auction.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * 用户注册
     * 限流：5次/小时
     */
    @PostMapping("/register")
    @RateLimit(key = "register", time = 3600, count = 5, message = "注册过于频繁，请稍后再试")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        UserResponse response = userService.findById(user.getId());
        return Result.ok(response);
    }

    /**
     * 用户登录
     * 限流：10次/分钟
     */
    @PostMapping("/login")
    @RateLimit(key = "login", time = 60, count = 10, message = "登录过于频繁，请稍后再试")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = authService.login(request, ipAddress, userAgent);
        return Result.ok(response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            authService.logout(userId);
        }
        return Result.ok();
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestHeader("Authorization") String authorization) {
        // 提取Bearer Token
        String refreshToken = authorization.replace("Bearer ", "");
        String newAccessToken = authService.refreshToken(refreshToken);
        return Result.ok(newAccessToken);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserResponse> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
        UserResponse response = userService.findCurrentUser(userId);
        return Result.ok(response);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
