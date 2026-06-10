package com.auction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OSS配置类
 * 从application.yml读取配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "oss")
public class OssConfig {

    /**
     * OSS区域
     */
    private String region = "oss-cn-hangzhou";

    /**
     * OSS访问域名
     */
    private String endpoint;

    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * 存储桶名称
     */
    private String bucketName = "auction-products";

    /**
     * 存储目录前缀
     */
    private String keyPrefix = "products/";

    /**
     * CDN域名（如果使用CDN加速）
     */
    private String cdnDomain;

    /**
     * 最大文件大小（MB）
     */
    private Long maxFileSize = 10L;

    /**
     * STS临时凭证有效期（秒）
     */
    private Long tokenExpireTime = 3600L;

    /**
     * 允许的图片格式
     */
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
}
