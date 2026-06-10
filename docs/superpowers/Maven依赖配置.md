# Maven依赖配置

# OSS表单直传所需的Maven依赖

## 📦 必需依赖

```xml
<!-- 阿里云OSS SDK -->
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>
```

## 📋 完整依赖说明

### 1. aliyun-sdk-oss
**用途**: OSS Java SDK
**版本**: 3.17.4
**功能**: 提供OSS的Java接口，用于OSS客户端的创建和管理

**为什么需要**: 虽然签名直传方案不直接使用OSS客户端，但在删除文件等功能中需要用到。

**使用场景**:
- 删除OSS文件
- 检查文件是否存在
- 其他OSS管理功能

### 2. 可选依赖

如果需要更多OSS功能，可以添加以下依赖：

```xml
<!-- 阿里云STS SDK（如果使用STS方案） -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-sts</artifactId>
    <version>3.1.0</version>
</dependency>

<!-- 阿里云核心SDK -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-core</artifactId>
    <version>4.6.4</version>
</dependency>
```

**注意**: 当前签名直传方案不需要STS依赖，签名算法在后端实现。

---

## 🔧 依赖冲突处理

### Jackson版本冲突

如果项目中Jackson版本过低，可能需要升级：

```xml
<!-- 确保Jackson版本 >= 2.13.0 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jdk8</artifactId>
    <version>2.15.2</version>
</dependency>
```

### Servlet API版本

确保使用Servlet 3.1+：

```xml
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>3.1.0</version>
    <scope>provided</scope>
</dependency>
```

---

## 📝 依赖优化建议

### 1. 排除不必要的依赖
如果项目中已经有其他云存储的SDK，需要排除：

```xml
<!-- 排除冲突的云存储SDK -->
<dependency>
    <groupId>com.qcloud</groupId>
    <artifactId>cos_api</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
        </exclusion>
    </exclusions>
</dependency>
```

### 2. 使用BOM统一管理依赖

**推荐**: 在父pom.xml中定义依赖版本，子模块继承：

```xml
<!-- 父pom.xml -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.aliyun.oss</groupId>
            <artifactId>aliyun-sdk-oss</artifactId>
            <version>3.17.4</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 子模块pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.aliyun.oss</groupId>
        <artifactId>aliyun-sdk-oss</artifactId>
    </dependency>
</dependencies>
```

---

## ✅ 验证依赖

### 1. 查看依赖树
```bash
mvn dependency:tree
```

应该看到：
```
└─ com.aliyun.oss:aliyun-sdk-oss:jar:3.17.4:compile
```

### 2. 检查依赖版本
```bash
mvn dependency:list | grep aliyun
```

应该显示：
```
com.aliyun.oss:aliyun-sdk-oss:jar:3.17.4:compile
```

### 3. 重新编译
```bash
mvn clean compile
```

如果编译成功，说明依赖配置正确！

---

## 🎯 版本选择建议

### 稳定版本
```xml
<aliyun-sdk-oss.version>3.17.4</aliyun-sdk-oss.version>
```
- 经过充分测试
- 生产环境验证
- 社区广泛使用

### 获取最新版本
访问: https://mvnrepository.com/artifact/com.aliyun.oss/aliyun-sdk-oss

---

## 💡 最佳实践

1. **统一版本管理**: 在父pom.xml中定义版本号
2. **定期更新**: 每季度检查一次新版本
3. **测试验证**: 升级前在测试环境验证
4. **查看更新日志**: https://github.com/aliyun/aliyun-oss-sdk-java/blob/master/CHANGELOG.md

---

这样配置后，项目就可以使用OSS签名直传功能了！