package com.auction.infrastructure.security;

import com.auction.common.AuthException;
import com.auction.common.ErrorCode;
import com.auction.config.JwtConfig;
import com.auction.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final StringRedisTemplate redisTemplate;

    /**
     * 不需要认证的路径
     */
    private static final List<String> EXCLUDE_PATHS = List.of(
        "/auth/register",
        "/auth/login",
        "/ws",
        "/swagger-resources",
        "/v3/api-docs",
        "/swagger-ui"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // 检查是否为排除路径
        if (isExcludePath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 提取Token
            String token = extractToken(request);
            if (token == null) {
                handleAuthenticationFailure(response, "未提供认证令牌");
                return;
            }

            // 验证Token
            if (!jwtService.validateToken(token)) {
                handleAuthenticationFailure(response, "令牌无效或已过期");
                return;
            }

            // 检查Token类型
            if (!jwtService.isAccessToken(token)) {
                handleAuthenticationFailure(response, "令牌类型错误");
                return;
            }

            // 检查Token是否在黑名单中（已注销）
            Long userId = jwtService.getUserIdFromToken(token);
            if (isTokenBlacklisted(userId, token)) {
                handleAuthenticationFailure(response, "令牌已失效");
                return;
            }

            // 设置用户信息到请求头
            String username = jwtService.getUsernameFromToken(token);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("token", token);

            // 继续过滤器链
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage());
            handleAuthenticationFailure(response, "认证失败");
        }
    }

    /**
     * 提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtConfig.getTokenHeader());
        if (bearerToken != null && bearerToken.startsWith(jwtConfig.getTokenPrefix())) {
            return bearerToken.substring(jwtConfig.getTokenPrefix().length());
        }
        return null;
    }

    /**
     * 检查是否为排除路径
     */
    private boolean isExcludePath(String requestPath) {
        return EXCLUDE_PATHS.stream().anyMatch(requestPath::startsWith);
    }

    /**
     * 检查Token是否在黑名单中
     */
    private boolean isTokenBlacklisted(Long userId, String token) {
        String blacklistKey = "token_blacklist:" + userId;
        String blacklistedToken = redisTemplate.opsForValue().get(blacklistKey);
        return token.equals(blacklistedToken);
    }

    /**
     * 处理认证失败
     */
    private void handleAuthenticationFailure(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String jsonResponse = String.format("{\"code\":401,\"message\":\"%s\",\"timestamp\":%d}",
            message, System.currentTimeMillis());
        response.getWriter().write(jsonResponse);
    }
}
