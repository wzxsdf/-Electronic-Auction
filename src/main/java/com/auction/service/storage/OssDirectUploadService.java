package com.auction.service.storage;

import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.auction.config.OssConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * OSS直传服务
 * 使用签名策略实现前端直传OSS，完全不需要后端做中转
 */
@Slf4j
@Service
public class OssDirectUploadService {

    private final OssConfig ossConfig;

    public OssDirectUploadService(OssConfig ossConfig) {
        this.ossConfig = ossConfig;
        log.info("OSS直传服务初始化成功: region={}, bucket={}",
                 ossConfig.getRegion(), ossConfig.getBucketName());
    }

    /**
     * 生成上传签名和策略
     *
     * @param userId 用户ID（用于隔离上传路径）
     * @param fileName 文件名（可选）
     * @return 上传签名信息
     */
    public UploadSignature generateUploadSignature(Long userId, String fileName) {
        try {
            // 1. 设置过期时间（1小时后）
            Instant expiration = Instant.now().plusSeconds(3600);
            String expirationStr = expiration.atZone(ZoneId.of("GMT"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

            // 2. 生成上传策略（限制上传条件）
            String policy = generateUploadPolicy(userId, expirationStr);

            // 3. 对策略进行Base64编码
            String base64Policy = Base64.getEncoder().encodeToString(
                policy.getBytes(StandardCharsets.UTF_8));

            // 4. 使用SecretKey对策略进行签名（HMAC-SHA1）
            String signature = hmacSHA1(ossConfig.getAccessKeySecret(), base64Policy);

            // 5. 构建返回结果
            UploadSignature uploadSignature = new UploadSignature();
            uploadSignature.setAccessKeyId(ossConfig.getAccessKeyId());
            uploadSignature.setPolicy(base64Policy);
            uploadSignature.setSignature(signature);
            uploadSignature.setExpiration(expirationStr);
            uploadSignature.setBucket(ossConfig.getBucketName());
            uploadSignature.setRegion(ossConfig.getRegion());
            uploadSignature.setEndpoint(ossConfig.getEndpoint());
            uploadSignature.setKeyPrefix(ossConfig.getKeyPrefix());
            uploadSignature.setUploadDir(ossConfig.getKeyPrefix() + userId + "/");

            log.info("生成上传签名成功: userId={}, expiration={}", userId, expirationStr);

            return uploadSignature;

        } catch (Exception e) {
            log.error("生成上传签名失败: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("生成上传签名失败", e);
        }
    }

    /**
     * 生成上传策略
     * 限制上传路径、文件大小、文件类型等
     */
    private String generateUploadPolicy(Long userId, String expiration) {
        Map<String, Object> policyConditions = new LinkedHashMap<>();

        // 过期时间
        policyConditions.put("expiration", expiration);

        // 上传条件
        List<Map<String, Object>> conditions = new ArrayList<>();

        // 1. 限制上传路径（只能上传到指定用户目录下）
        Map<String, Object> bucketCondition = new HashMap<>();
        bucketCondition.put("bucket", ossConfig.getBucketName());
        conditions.add(bucketCondition);

        Map<String, Object> keyCondition = new HashMap<>();
        keyCondition.put("starts-with", "$key");
        keyCondition.put("value", ossConfig.getKeyPrefix() + userId + "/");
        conditions.add(keyCondition);

        // 2. 限制文件大小（最大10MB）
        long maxSize = ossConfig.getMaxFileSize() * 1024 * 1024;
        Map<String, Object> sizeCondition = new HashMap<>();
        sizeCondition.put("content-length-range", Arrays.asList(0, maxSize));
        conditions.add(sizeCondition);

        // 3. 限制文件类型（只允许图片）
        String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
        Map<String, Object> typeCondition = new HashMap<>();
        typeCondition.put("in", "$Content-Type");
        typeCondition.put("value", Arrays.asList(allowedTypes));
        conditions.add(typeCondition);

        policyConditions.put("conditions", conditions);

        // 转换为JSON字符串
        return buildPolicyJson(policyConditions);
    }

    /**
     * 构建策略JSON
     */
    private String buildPolicyJson(Map<String, Object> policy) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"expiration\":\"").append(policy.get("expiration")).append("\",");
        json.append("\"conditions\":[");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) policy.get("conditions");

        for (int i = 0; i < conditions.size(); i++) {
            Map<String, Object> condition = conditions.get(i);
            json.append("{");

            int j = 0;
            for (Map.Entry<String, Object> entry : condition.entrySet()) {
                if (j > 0) json.append(",");

                json.append("\"").append(entry.getKey()).append("\":");

                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(value).append("\"");
                } else if (value instanceof List) {
                    json.append("[");
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) value;
                    for (int k = 0; k < list.size(); k++) {
                        if (k > 0) json.append(",");
                        json.append("\"").append(list.get(k)).append("\"");
                    }
                    json.append("]");
                } else {
                    json.append(value);
                }

                j++;
            }

            json.append("}");
            if (i < conditions.size() - 1) json.append(",");
        }

        json.append("]}");
        return json.toString();
    }

    /**
     * HMAC-SHA1签名
     */
    private String hmacSHA1(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception e) {
            throw new RuntimeException("签名失败", e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileUrl) {
        try {
            // 创建OSS客户端
            DefaultCredentialProvider credentialsProvider = new DefaultCredentialProvider(
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
            );

            var ossClient = new OSSClientBuilder()
                .build(ossConfig.getEndpoint(), credentialsProvider);

            // 从URL中提取objectKey
            String objectKey = extractObjectKeyFromUrl(fileUrl);
            if (objectKey != null && objectKey.startsWith("products/")) {
                ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
                log.info("文件删除成功: bucket={}, key={}",
                         ossConfig.getBucketName(), objectKey);
            }
        } catch (Exception e) {
            log.error("文件删除失败: url={}, error={}", fileUrl, e.getMessage());
        }
    }

    /**
     * 从URL中提取ObjectKey
     */
    private String extractObjectKeyFromUrl(String fileUrl) {
        try {
            String path = fileUrl.substring(fileUrl.indexOf("/", 8) + 1); // 跳过 https://
            return path.split("\\?")[0]; // 移除查询参数
        } catch (Exception e) {
            log.error("解析URL失败: url={}", fileUrl, e);
        }
        return null;
    }

    /**
     * 上传签名信息
     */
    public static class UploadSignature {
        private String accessKeyId;      // AccessKey ID
        private String policy;            // Base64编码的上传策略
        private String signature;         // 签名
        private String expiration;        // 过期时间
        private String bucket;            // 存储桶名称
        private String region;            // 区域
        private String endpoint;          // OSS端点
        private String keyPrefix;         // 上传路径前缀
        private String uploadDir;         // 上传目录

        // Getters and Setters
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }

        public String getPolicy() { return policy; }
        public void setPolicy(String policy) { this.policy = policy; }

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }

        public String getExpiration() { return expiration; }
        public void setExpiration(String expiration) { this.expiration = expiration; }

        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

        public String getUploadDir() { return uploadDir; }
        public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    }
}

