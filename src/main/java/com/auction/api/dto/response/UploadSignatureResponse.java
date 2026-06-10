package com.auction.api.dto.response;

import lombok.Data;

/**
 * OSS上传签名响应DTO
 */
@Data
public class UploadSignatureResponse {
    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * Base64编码的上传策略
     */
    private String policy;

    /**
     * 签名
     */
    private String signature;

    /**
     * 过期时间
     */
    private String expiration;

    /**
     * 存储桶名称
     */
    private String bucket;

    /**
     * 区域
     */
    private String region;

    /**
     * OSS端点
     */
    private String endpoint;

    /**
     * 存储路径前缀
     */
    private String keyPrefix;

    /**
     * 上传目录
     */
    private String uploadDir;
}
