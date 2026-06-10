# OSS表单直传架构设计

## 🎯 核心理念：完全不需要后端做中转

### 传统方案的问题

```
❌ 传统方式：
文件 → 前端 → 后端服务器 → OSS
              ↑
         占用大量带宽和服务器资源
```

### 签名直传方案

```
✅ 优化方式：
文件 → 前端 → OSS
         ↑
    后端只提供签名，完全不参与上传
```

---

## 🏗️ 架构设计

### 整体架构图

```
┌──────────────┐
│   前端/移动端  │
└───────┬───────┘
        │
        │ 1. 请求签名
        ▼
┌──────────────┐
│   后端服务器   │
│ ┌────────────┐ │
│ │Controller  │ │
│ │/upload/    │ │
│ │signature   │ │
│ └──────┬─────┘ │
│        │      │
│ │ 生成策略和签名│
│ └──────┬───────┘ │
└────────┼────────┘
         │
         │ 2. 返回签名信息
         │    {
         │      accessKeyId: "...",
         │      policy: "base64...",
         │      signature: "签名",
         │      bucket: "bucket",
         │      uploadDir: "products/USER_ID/"
         │    }
         ▼
┌──────────────────┐
│   前端/移动端     │
│ ┌──────────────┐ │
│ │OSS SDK/FormData│ │
│ │ 直接上传到OSS  │ │
│ └───────┬───────┘ │
└────────┼──────────┘
         │
         │ 3. POST https://bucket.oss-cn-hangzhou.aliyuncs.com
         │    OSSAccessKeyId: ...
         │    policy: ...
         │    signature: ...
         │    file: <binary>
         ▼
┌──────────────────┐
│   阿里云OSS      │
│  验证签名        │
│  保存文件        │
│  返回URL        │
└────────┼──────────┘
         │
         │ 4. 返回图片URL
         ▼
┌──────────────────┐
│   前端/移动端     │
│  接收URL:        │
│  https://cdn...  │
└────────┼──────────┘
         │
         │ 5. 提交商品信息
         ▼
┌──────────────────┐
│   后端服务器     │
│ 保存到数据库:    │
│ imageUrl字段     │
└──────────────────┘
```

---

## 🔐 安全机制

### 1. 上传策略（Policy）

**后端生成的策略限制**：
```json
{
  "expiration": "2026-06-07T02:45:00Z",
  "conditions": [
    {"bucket": "auction-products"},
    {"starts-with": "$key", "products/USER_ID/"},
    {"content-length-range": 0, 10485760},
    {"in": "$Content-Type", ["image/jpeg", "image/png", "image/gif"]}
  ]
}
```

**安全保护**：
- ✅ 只能上传到指定Bucket
- ✅ 只能上传到用户自己的目录
- ✅ 限制文件大小（最大10MB）
- ✅ 限制文件类型（只允许图片）
- ✅ 签名有时效性（1小时）

### 2. 签名机制

**HMAC-SHA1签名流程**：
```
1. 后端生成上传策略（JSON）
2. Base64编码策略
3. 使用SecretKey对策略进行HMAC-SHA1签名
4. 将策略和签名返回给前端
5. 前端使用签名上传
6. OSS验证签名和策略
```

**为什么安全？**
- 签名无法伪造（需要SecretKey）
- 策略不可篡改（签名会验证）
- 有时效性（1小时后自动失效）
- 路径受限（只能上传到指定目录）

---

## 📊 数据流程详解

### 步骤1：前端请求签名

```http
POST /upload/signature?fileName=example.jpg
Authorization: Bearer <JWT_TOKEN>

后端处理：
1. 验证JWT Token
2. 提取userId（用于路径隔离）
3. 生成上传策略（限制上传条件）
4. 生成HMAC-SHA1签名
5. 返回签名信息
```

### 步骤2：后端返回签名

```json
{
  "code": 200,
  "data": {
    "accessKeyId": "LTAI5t7xxxxx",
    "policy": "eyJleHBpcmF0aW9uIjoiMjAyNi0wNi0wN1QwMjo0NTowMFoifQ==",
    "signature": "ZjYxM2YzNDU2Nzc4OQ==",
    "expiration": "2026-06-07T02:45:00Z",
    "bucket": "auction-products",
    "region": "oss-cn-hangzhou",
    "endpoint": "https://oss-cn-hangzhou.aliyuncs.com",
    "keyPrefix": "products/1/",
    "uploadDir": "products/1/"
  }
}
```

### 步骤3：前端直接上传

```javascript
// 使用FormData上传
const formData = new FormData();
formData.append('file', file);                              // 文件内容
formData.append('OSSAccessKeyId', signature.accessKeyId);      // AccessKey ID
formData.append('policy', signature.policy);                  // 上传策略
formData.append('signature', signature.signature);            // 签名
formData.append('key', signature.uploadDir + file.name);      // 存储路径
formData.append('Content-Type', file.type);                    // 文件类型

// 直接POST到OSS
const uploadUrl = `https://${signature.bucket}.${signature.region}.aliyuncs.com`;
await fetch(uploadUrl, { method: 'POST', body: formData });
```

### 步骤4：OSS验证并保存

```
OSS接收请求 → 验证签名 → 验证策略 → 检查条件 → 保存文件 → 返回成功
```

**OSS验证内容**：
- 签名是否正确
- 策略是否过期
- 文件大小是否超限
- 文件类型是否允许
- 上传路径是否符合策略

---

## 💡 核心优势

### 1. 不占用服务器资源

**传统方式**：
```
文件（5MB）→ 后端 → OSS
- 后端接收：5MB带宽
- 后端转发：5MB带宽
- 总共：10MB带宽 + CPU处理成本
```

**签名直传**：
```
文件（5MB）→ OSS
- 后端：只返回几百字节的签名信息
- 前端：直连OSS，5MB带宽
- 节省：10MB服务器带宽 + CPU成本
```

### 2. 更好的用户体验

| 对比项 | 传统方式 | 签名直传 |
|--------|----------|----------|
| **上传速度** | ⚠️ 较慢（经转发） | ✅ 更快（直连） |
| **大文件** | ❌ 容易超时 | ✅ 支持大文件 |
| **并发上传** | ⚠️ 受服务器限制 | ✅ 无限制 |
| **断点续传** | ⚠️ 需要后端支持 | ✅ 原生支持 |

### 3. 更简单的架构

**传统方式需要**：
- ❌ 后端文件处理逻辑
- ❌ 后端内存管理
- ❌ 后端带宽管理
- ❌ 临时文件清理

**签名直传只需**：
- ✅ 简单的签名生成逻辑
- ✅ 几行代码即可实现

---

## 🔧 实现细节

### 后端签名生成

```java
/**
 * 生成上传签名
 */
public UploadSignature generateUploadSignature(Long userId, String fileName) {
    // 1. 设置过期时间（1小时）
    Instant expiration = Instant.now().plusSeconds(3600);
    String expirationStr = expiration.atZone(ZoneId.of("GMT"))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

    // 2. 生成上传策略
    String policy = generateUploadPolicy(userId, expirationStr);

    // 3. Base64编码策略
    String base64Policy = Base64.getEncoder().encodeToString(
        policy.getBytes(StandardCharsets.UTF_8));

    // 4. 使用SecretKey进行HMAC-SHA1签名
    String signature = hmacSHA1(accessKeySecret, base64Policy);

    // 5. 构建返回结果
    UploadSignature uploadSignature = new UploadSignature();
    uploadSignature.setAccessKeyId(accessKeyId);
    uploadSignature.setPolicy(base64Policy);
    uploadSignature.setSignature(signature);
    uploadSignature.setExpiration(expirationStr);
    uploadSignature.setBucket(bucketName);
    uploadSignature.setUploadDir("products/" + userId + "/");

    return uploadSignature;
}
```

### 前端上传实现

```javascript
/**
 * 使用FormData直接上传
 */
async function uploadToOSS(file, signature) {
    const formData = new FormData();
    
    // 必须字段
    formData.append('file', file);
    formData.append('OSSAccessKeyId', signature.accessKeyId);
    formData.append('policy', signature.policy);
    formData.append('signature', signature.signature);
    formData.append('key', signature.uploadDir + file.name);
    formData.append('Content-Type', file.type);

    // 上传到OSS
    const uploadUrl = `https://${signature.bucket}.${signature.region}.aliyuncs.com`;
    const response = await fetch(uploadUrl, {
        method: 'POST',
        body: formData
    });

    if (response.ok) {
        // 返回图片URL
        return `${uploadUrl}/${signature.uploadDir}${file.name}`;
    } else {
        throw new Error('上传失败');
    }
}
```

---

## 📋 配置说明

### 阿里云OSS配置

**1. 创建Bucket**
```bash
- 名称：auction-products
- 区域：oss-cn-hangzhou
- 读写权限：私有
```

**2. 配置CORS**
```json
{
  "AllowedOrigins": ["*"],
  "AllowedMethods": ["GET", "POST", "PUT", "DELETE", "HEAD"],
  "AllowedHeaders": ["*"],
  "ExposeHeaders": ["ETag", "x-oss-request-id"],
  "MaxAgeSeconds": 3600
}
```

**3. 跨域设置位置**
```
阿里云OSS控制台 → Bucket管理 → 权限管理 → 跨域设置
```

### 应用配置

```yaml
# application.yml
oss:
  region: oss-cn-hangzhou
  endpoint: https://oss-cn-hangzhou.aliyuncs.com
  access-key-id: YOUR_ACCESS_KEY_ID
  access-key-secret: YOUR_ACCESS_KEY_SECRET
  bucket-name: auction-products
  key-prefix: products/
  cdn-domain: https://cdn.yourdomain.com
  max-file-size: 10
```

---

## 🚀 性能对比

### 带宽成本

**场景：100个用户同时上传5MB图片**

| 方案 | 后端带宽 | 服务器压力 | 成本 |
|------|---------|-----------|------|
| **传统中转** | 500MB × 2 = 1GB | 高 | 高 |
| **签名直传** | 10KB（签名） | 无 | 低 |

**节省成本**：99.99%的带宽 + CPU成本

### 上传速度

**场景：上传10MB图片**

| 方案 | 网络路径 | 耗时 | 备注 |
|------|---------|------|------|
| **传统中转** | 前端→后端→OSS | ~15秒 | 受后端处理速度影响 |
| **签名直传** | 前端→OSS | ~5秒 | 直连速度 |

---

## 🎯 适用场景

### ✅ 推荐场景

1. **电商平台**：商品图片上传
2. **社交应用**：用户头像、动态图片
3. **内容管理**：文章配图、媒体文件
4. **文档系统**：PDF、Office文档
5. **大文件上传**：视频、音频

### ⚠️ 注意事项

1. **签名有效期**：1小时，过期需重新获取
2. **路径限制**：必须使用后端返回的uploadDir
3. **文件验证**：前后端都要验证文件类型和大小
4. **CORS配置**：OSS必须配置跨域规则

---

## 💡 最佳实践

### 1. 前端

```javascript
// ✅ 推荐：使用签名直传
const signature = await getUploadSignature(fileName);
const imageUrl = await uploadToOSS(file, signature);

// ❌ 不推荐：上传到后端
const formData = new FormData();
formData.append('file', file);
const imageUrl = await fetch('/upload/image', {  // 占用服务器带宽
  method: 'POST',
  body: formData
}).then(r => r.json()).then(d => d.data.imageUrl);
```

### 2. 后端

```java
// ✅ 推荐：只提供签名
@PostMapping("/signature")
public Result<UploadSignature> getSignature(@CurrentUser UserPrincipal user) {
    return Result.ok(ossDirectUploadService.generateUploadSignature(user.getUserId()));
}

// ❌ 不推荐：接收文件并转发
@PostMapping("/image")
public String uploadImage(@RequestParam MultipartFile file) {
    return ossStorageService.upload(file);  // 占用服务器资源
}
```

### 3. 错误处理

```javascript
try {
    const signature = await getUploadSignature(fileName);
    const imageUrl = await uploadToOSS(file, signature);
} catch (error) {
    if (error.message.includes('签名过期')) {
        // 重新获取签名
        signature = await getUploadSignature(fileName);
        imageUrl = await uploadToOSS(file, signature);
    } else if (error.message.includes('策略验证失败')) {
        // 文件不符合要求（大小、类型等）
        alert('文件验证失败：' + error.message);
    }
}
```

---

这个方案被阿里云官方推荐，是生产环境的首选方案！完全不需要后端做中转，性能最优，成本最低。