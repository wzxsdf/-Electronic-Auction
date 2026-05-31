package com.auction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * 密钥
     */
    private String secret = "auction-system-secret-key-change-in-production-environment";

    /**
     * Access Token过期时间（毫秒）
     */
    private Long accessTokenExpiration = 7200000L; // 2小时

    /**
     * Refresh Token过期时间（毫秒）
     */
    private Long refreshTokenExpiration = 604800000L; // 7天

    /**
     * Token前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Token请求头名称
     */
    private String tokenHeader = "Authorization";
}
