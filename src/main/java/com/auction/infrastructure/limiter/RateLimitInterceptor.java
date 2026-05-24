package com.auction.infrastructure.limiter;

import com.auction.annotation.RateLimit;
import com.auction.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * 限流拦截器
 * 基于 Redis + Lua 脚本实现滑动窗口限流
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RATE_LIMIT_LUA =
            "local key = KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local expire = tonumber(ARGV[2])\n" +
            "local current = tonumber(redis.call('get', key) or '0')\n" +
            "if current + 1 > limit then\n" +
            "    return 0\n" +
            "else\n" +
            "    redis.call('incrby', key, 1)\n" +
            "    if current == 0 then\n" +
            "        redis.call('expire', key, expire)\n" +
            "    end\n" +
            "    return 1\n" +
            "end";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        // 获取客户端标识（IP 或用户 ID）
        String clientId = getClientId(request);
        String limitKey = rateLimit.key() + ":" + clientId + ":" + request.getRequestURI();

        // 执行限流检查
        boolean allowed = checkRateLimit(limitKey, rateLimit.count(), rateLimit.time());

        if (!allowed) {
            responseRateLimit(response, rateLimit.message());
            log.warn("限流触发: client={}, uri={}", clientId, request.getRequestURI());
            return false;
        }

        return true;
    }

    private boolean checkRateLimit(String key, int limit, int expire) {
        Long result = redisTemplate.execute(
                connection -> connection.eval(
                        RATE_LIMIT_LUA.getBytes(StandardCharsets.UTF_8),
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        1,
                        key.getBytes(StandardCharsets.UTF_8),
                        String.valueOf(limit).getBytes(StandardCharsets.UTF_8),
                        String.valueOf(expire).getBytes(StandardCharsets.UTF_8)
                ),
                true
        );
        return result != null && result == 1;
    }

    private String getClientId(HttpServletRequest request) {
        // 优先从请求头获取用户 ID
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // 其次使用 IP 地址
        String ip = getClientIp(request);
        return "ip:" + ip;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void responseRateLimit(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.fail(429, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
