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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT认证过滤器
 * 从请求头中提取JWT token，解析用户信息并存储到SecurityContext
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
        "/api/auth/register",
        "/api/auth/login",
        "/ws",
        "/swagger-resources",
        "/v3/api-docs",
        "/swagger-ui",
        "/products"  // 商品查询接口公开访问
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        try {
            // 1. 检查是否为排除路径（公开接口）
            if (isExcludePath(requestPath)) {
                // 公开接口，不设置用户信息，但继续处理请求
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 提取Token
            String token = extractToken(request);
            if (token == null) {
                // 没有 token，继续处理（可能后续会有其他认证方式或参数解析器处理）
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 验证Token
            if (!jwtService.validateToken(token)) {
                handleAuthenticationFailure(response, "令牌无效或已过期");
                return;
            }

            // 4. 检查Token类型
            if (!jwtService.isAccessToken(token)) {
                handleAuthenticationFailure(response, "令牌类型错误");
                return;
            }

            // 5. 检查Token是否在黑名单中（已注销）
            Long userId = jwtService.getUserIdFromToken(token);
            if (isTokenBlacklisted(userId, token)) {
                handleAuthenticationFailure(response, "令牌已失效");
                return;
            }

            // 6. 提取用户信息
            String username = jwtService.getUsernameFromToken(token);
            List<String> roles = jwtService.getRolesFromToken(token);

            // 7. 创建Spring Security认证对象
            List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

            // 设置到Spring Security上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 8. 同时设置到我们自定义的SecurityContext（供@CurrentUser使用）
            UserPrincipal userPrincipal = new UserPrincipal(userId, username,
                roles != null && !roles.isEmpty() ? roles.get(0) : "USER");
            com.auction.infrastructure.security.SecurityContextHolder.setContext(
                new com.auction.infrastructure.security.SecurityContext(userPrincipal));

            // 9. 设置到请求属性中（便于其他地方使用）
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("token", token);

            // 10. 继续过滤器链
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage());
            handleAuthenticationFailure(response, "认证失败");
        } finally {
            // 清理自定义Security上下文，避免线程池复用时的安全问题
            com.auction.infrastructure.security.SecurityContextHolder.clearContext();
            // 清理Spring Security上下文
            SecurityContextHolder.clearContext();
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
