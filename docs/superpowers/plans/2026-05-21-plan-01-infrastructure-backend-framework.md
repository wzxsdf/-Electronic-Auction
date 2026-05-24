# 直播竞拍全栈系统 - Plan 1: 基础设施 + 后端框架

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建后端项目基础设施，包括 Spring Boot 脚手架、数据库表结构、Redis 配置、统一响应格式、异常处理等基础能力。

**Architecture:** 单体 Spring Boot 应用，分层架构（Controller → Service → Repository），使用 MyBatis-Plus 作为 ORM 框架，Redis 做缓存，MySQL 做持久化。

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, Redis, MySQL 8.0, Maven

---

## 文件结构

```
auction-system/
├── pom.xml                                    # Maven 配置
├── src/main/java/com/auction/
│   ├── AuctionApplication.java                # 启动类
│   ├── common/
│   │   ├── Result.java                        # 统一响应格式
│   │   ├── BaseException.java                 # 基础异常
│   │   ├── ErrorCode.java                     # 错误码枚举
│   │   └── GlobalExceptionHandler.java        # 全局异常处理
│   ├── config/
│   │   ├── RedisConfig.java                   # Redis 配置
│   │   ├── MyBatisPlusConfig.java             # MyBatis-Plus 配置
│   │   └── CorsConfig.java                    # 跨域配置
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   ├── Product.java
│   │   │   ├── Auction.java
│   │   │   ├── Bid.java
│   │   │   └── Order.java
│   │   └── enums/
│   │       ├── AuctionStatus.java
│   │       ├── BidStatus.java
│   │       └── OrderStatus.java
│   └── infrastructure/
│       ├── mapper/
│       │   ├── UserMapper.java
│       │   ├── ProductMapper.java
│       │   ├── AuctionMapper.java
│       │   ├── BidMapper.java
│       │   └── OrderMapper.java
│       └── redis/
│           └── RedisService.java              # Redis 操作封装
└── src/main/resources/
    ├── application.yml                        # 配置文件
    ├── application-dev.yml                     # 开发环境配置
    ├── schema.sql                             # 数据库建表脚本
    └── logback-spring.xml                     # 日志配置
```

---

## Task 1: 创建 Maven 项目结构

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/auction/AuctionApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.auction</groupId>
    <artifactId>auction-system</artifactId>
    <version>1.0.0</version>
    <name>auction-system</name>
    <description>Live Auction System</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <druid.version>1.2.20</druid.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Druid 数据源 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
            <version>${druid.version}</version>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建启动类**

```java
package com.auction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.auction.infrastructure.mapper")
public class AuctionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
spring:
  application:
    name: auction-system
  profiles:
    active: dev

server:
  port: 8080
  servlet:
    context-path: /api

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

- [ ] **Step 4: 创建 application-dev.yml**

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/auction?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root123
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000

  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      timeout: 3000
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

logging:
  level:
    com.auction: DEBUG
    com.baomidou.mybatisplus: DEBUG
```

- [ ] **Step 5: 创建 logback-spring.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="APP_NAME" source="spring.application.name"/>
    <property name="LOG_PATH" value="logs"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

- [ ] **Step 6: 验证项目可以启动**

```bash
mvn clean install
mvn spring-boot:run
```

Expected: 应用成功启动，端口 8080

- [ ] **Step 7: 提交**

```bash
git add .
git commit -m "feat: 创建 Spring Boot 项目脚手架"
```

---

## Task 2: 创建统一响应格式和异常处理

**Files:**
- Create: `src/main/java/com/auction/common/Result.java`
- Create: `src/main/java/com/auction/common/ErrorCode.java`
- Create: `src/main/java/com/auction/common/BaseException.java`
- Create: `src/main/java/com/auction/common/BizException.java`
- Create: `src/main/java/com/auction/common/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建 Result 统一响应类**

```java
package com.auction.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = ok();
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
```

- [ ] **Step 2: 创建 ErrorCode 错误码枚举**

```java
package com.auction.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用错误码
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 业务错误码 1000-1999
    AUCTION_NOT_FOUND(1001, "竞拍不存在"),
    AUCTION_NOT_STARTED(1002, "竞拍未开始"),
    AUCTION_ALREADY_ENDED(1003, "竞拍已结束"),
    AUCTION_CANCELLED(1004, "竞拍已取消"),

    // 出价相关 2000-2999
    BID_AMOUNT_TOO_LOW(2001, "出价金额必须高于当前价格"),
    BID_AMOUNT_INVALID(2002, "出价金额不符合加价幅度要求"),
    BID_FREQUENCY_HIGH(2003, "出价频率过高，请稍后再试"),
    BID_USER_BLOCKED(2004, "账户已被限制，无法出价"),
    BID_EXCEED_MAX_PRICE(2005, "出价超过封顶价"),

    // 用户相关 3000-3999
    USER_NOT_FOUND(3001, "用户不存在"),
    USER_BALANCE_INSUFFICIENT(3002, "余额不足"),

    // 商品相关 4000-4999
    PRODUCT_NOT_FOUND(4001, "商品不存在"),
    PRODUCT_ALREADY_IN_AUCTION(4002, "商品已在竞拍中"),

    // 风控相关 5000-5999
    RISK_BLOCKED(5001, "触发风控拦截"),
    RISK_HIGH(5002, "账户风险等级过高，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 3: 创建 BaseException 基础异常**

```java
package com.auction.common;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final int code;

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
```

- [ ] **Step 4: 创建 BizException 业务异常**

```java
package com.auction.common;

public class BizException extends BaseException {

    public BizException(int code, String message) {
        super(code, message);
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BizException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
```

- [ ] **Step 5: 创建 GlobalExceptionHandler 全局异常处理**

```java
package com.auction.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        FieldError fieldError = e.getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR.getCode(), "系统内部错误");
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add .
git commit -m "feat: 添加统一响应格式和全局异常处理"
```

---

## Task 3: 创建配置类

**Files:**
- Create: `src/main/java/com/auction/config/RedisConfig.java`
- Create: `src/main/java/com/auction/config/MyBatisPlusConfig.java`
- Create: `src/main/java/com/auction/config/CorsConfig.java`

- [ ] **Step 1: 创建 RedisConfig**

```java
package com.auction.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        serializer.setObjectMapper(mapper);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

- [ ] **Step 2: 创建 MyBatisPlusConfig**

```java
package com.auction.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 3: 创建 CorsConfig**

```java
package com.auction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add .
git commit -m "feat: 添加 Redis、MyBatis-Plus、CORS 配置"
```

---

## Task 4: 创建数据库表结构

**Files:**
- Create: `src/main/resources/schema.sql`

- [ ] **Step 1: 创建 schema.sql**

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS auction DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE auction;

-- 用户表
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50) UNIQUE NOT NULL,
    nickname        VARCHAR(100),
    avatar_url      VARCHAR(500),
    total_bids      INT DEFAULT 0,
    total_wins      INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
CREATE TABLE products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    image_url       VARCHAR(500),
    description     TEXT,
    category        VARCHAR(50),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 竞拍活动表
CREATE TABLE auctions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    start_price     DECIMAL(10,2) NOT NULL DEFAULT 0,
    bid_increment   DECIMAL(10,2) NOT NULL,
    max_price       DECIMAL(10,2),
    delay_seconds   INT NOT NULL DEFAULT 15,
    start_time      DATETIME NOT NULL,
    end_time        DATETIME NOT NULL,
    original_end_time DATETIME NOT NULL,
    current_price   DECIMAL(10,2) DEFAULT 0,
    highest_bidder  BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_time (start_time, end_time),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞拍活动表';

-- 出价记录表
CREATE TABLE bids (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    rank_when_bid   INT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_auto_bid     BOOLEAN DEFAULT FALSE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auction_user (auction_id, user_id),
    INDEX idx_auction_amount (auction_id, amount DESC),
    INDEX idx_created (created_at),
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出价记录表';

-- 订单表
CREATE TABLE orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    final_amount    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_auction (auction_id),
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 插入测试数据
INSERT INTO users (username, nickname) VALUES
('test_user_1', '测试用户1'),
('test_user_2', '测试用户2');

INSERT INTO products (name, description, category) VALUES
('翡翠手镯', '天然A货翡翠手镯，色泽温润，质地细腻', '珠宝'),
('黄金项链', '999足金项链，精致工艺，时尚百搭', '珠宝');
```

- [ ] **Step 2: 执行 SQL 创建数据库和表**

```bash
mysql -u root -p < src/main/resources/schema.sql
```

Expected: 数据库和表创建成功

- [ ] **Step 3: 验证表结构**

```bash
mysql -u root -p -e "USE auction; SHOW TABLES; DESCRIBE auctions;"
```

Expected: 显示所有表和 auctions 表结构

- [ ] **Step 4: 提交**

```bash
git add .
git commit -m "feat: 添加数据库表结构"
```

---

## Task 5: 创建实体类

**Files:**
- Create: `src/main/java/com/auction/domain/entity/User.java`
- Create: `src/main/java/com/auction/domain/entity/Product.java`
- Create: `src/main/java/com/auction/domain/entity/Auction.java`
- Create: `src/main/java/com/auction/domain/entity/Bid.java`
- Create: `src/main/java/com/auction/domain/entity/Order.java`

- [ ] **Step 1: 创建 User 实体**

```java
package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String nickname;
    private String avatarUrl;

    private Integer totalBids;
    private Integer totalWins;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 Product 实体**

```java
package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("products")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String imageUrl;
    private String description;
    private String category;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建 AuctionStatus 枚举**

```java
package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum AuctionStatus {
    PENDING("待开始"),
    ACTIVE("进行中"),
    PAUSED("已暂停"),
    CANCELLED("已取消"),
    COMPLETED("已结束");

    private final String description;

    AuctionStatus(String description) {
        this.description = description;
    }
}
```

- [ ] **Step 4: 创建 Auction 实体**

```java
package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.AuctionStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("auctions")
public class Auction {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private String title;
    private BigDecimal startPrice;
    private BigDecimal bidIncrement;
    private BigDecimal maxPrice;
    private Integer delaySeconds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime originalEndTime;
    private BigDecimal currentPrice;
    private Long highestBidder;
    private String status;  // 存储 AuctionStatus.name()

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public AuctionStatus getStatusEnum() {
        return status != null ? AuctionStatus.valueOf(status) : null;
    }

    public void setStatusEnum(AuctionStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
}
```

- [ ] **Step 5: 创建 BidStatus 枚举**

```java
package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum BidStatus {
    ACTIVE("有效"),
    SUPPLANTED("被超越"),
    WINNING("领先"),
    INVALID("无效");

    private final String description;

    BidStatus(String description) {
        this.description = description;
    }
}
```

- [ ] **Step 6: 创建 Bid 实体**

```java
package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bids")
public class Bid {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long auctionId;
    private Long userId;
    private BigDecimal amount;
    private Integer rankWhenBid;
    private String status;
    private Boolean isAutoBid;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 7: 创建 OrderStatus 枚举**

```java
package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("待支付"),
    PAID("已支付"),
    CANCELLED("已取消"),
    REFUNDED("已退款");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }
}
```

- [ ] **Step 8: 创建 Order 实体**

```java
package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long auctionId;
    private Long userId;
    private Long productId;
    private BigDecimal finalAmount;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public OrderStatus getStatusEnum() {
        return status != null ? OrderStatus.valueOf(status) : null;
    }

    public void setStatusEnum(OrderStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
}
```

- [ ] **Step 9: 提交**

```bash
git add .
git commit -m "feat: 添加实体类和枚举"
```

---

## Task 6: 创建 Mapper 接口

**Files:**
- Create: `src/main/java/com/auction/infrastructure/mapper/UserMapper.java`
- Create: `src/main/java/com/auction/infrastructure/mapper/ProductMapper.java`
- Create: `src/main/java/com/auction/infrastructure/mapper/AuctionMapper.java`
- Create: `src/main/java/com/auction/infrastructure/mapper/BidMapper.java`
- Create: `src/main/java/com/auction/infrastructure/mapper/OrderMapper.java`

- [ ] **Step 1: 创建 UserMapper**

```java
package com.auction.infrastructure.mapper;

import com.auction.domain.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 2: 创建 ProductMapper**

```java
package com.auction.infrastructure.mapper;

import com.auction.domain.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
```

- [ ] **Step 3: 创建 AuctionMapper**

```java
package com.auction.infrastructure.mapper;

import com.auction.domain.entity.Auction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuctionMapper extends BaseMapper<Auction> {
}
```

- [ ] **Step 4: 创建 BidMapper**

```java
package com.auction.infrastructure.mapper;

import com.auction.domain.entity.Bid;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BidMapper extends BaseMapper<Bid> {
}
```

- [ ] **Step 5: 创建 OrderMapper**

```java
package com.auction.infrastructure.mapper;

import com.auction.domain.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
```

- [ ] **Step 6: 创建 MetaObjectHandler 自动填充**

```java
package com.auction.infrastructure.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

- [ ] **Step 7: 提交**

```bash
git add .
git commit -m "feat: 添加 Mapper 接口和自动填充配置"
```

---

## Task 7: 创建 Redis 服务封装

**Files:**
- Create: `src/main/java/com/auction/infrastructure/redis/RedisService.java`

- [ ] **Step 1: 创建 RedisService**

```java
package com.auction.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== String 操作 ====================

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        return value != null ? clazz.cast(value) : null;
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // ==================== Hash 操作 ====================

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public Boolean hDelete(String key, String field) {
        return redisTemplate.opsForHash().delete(key, field) > 0;
    }

    // ==================== ZSet 操作 ====================

    public Boolean zAdd(String key, Object member, double score) {
        return redisTemplate.opsForZSet().add(key, member, score);
    }

    public Long zRemove(String key, Object... members) {
        return redisTemplate.opsForZSet().remove(key, members);
    }

    public Long zRank(String key, Object member) {
        return redisTemplate.opsForZSet().rank(key, member);
    }

    public Long zReverseRank(String key, Object member) {
        return redisTemplate.opsForZSet().reverseRank(key, member);
    }

    public Double zScore(String key, Object member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    public Long zCount(String key, double min, double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    // ==================== Set 操作 ====================

    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加 Redis 服务封装"
```

---

## Task 8: 创建测试 API 验证基础设施

**Files:**
- Create: `src/main/java/com/auction/api/TestController.java`

- [ ] **Step 1: 创建测试控制器**

```java
package com.auction.api;

import com.auction.common.Result;
import com.auction.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final RedisService redisService;

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }

    @PostMapping("/redis/set")
    public Result<Void> testRedisSet(@RequestParam String key, @RequestParam String value) {
        redisService.set(key, value, 60, java.util.concurrent.TimeUnit.SECONDS);
        return Result.ok();
    }

    @GetMapping("/redis/get")
    public Result<Object> testRedisGet(@RequestParam String key) {
        Object value = redisService.get(key);
        return Result.ok(value);
    }

    @GetMapping("/redis/incr")
    public Result<Long> testRedisIncr(@RequestParam String key) {
        Long value = redisService.increment(key);
        return Result.ok(value);
    }
}
```

- [ ] **Step 2: 启动应用并测试**

```bash
mvn spring-boot:run
```

在另一个终端测试：

```bash
# 测试基础接口
curl http://localhost:8080/api/test/ping

# 测试 Redis 写入
curl "http://localhost:8080/api/test/redis/set?key=test&value=hello"

# 测试 Redis 读取
curl http://localhost:8080/api/test/redis/get?key=test

# 测试 Redis 自增
curl http://localhost:8080/api/test/redis/incr?key=counter
curl http://localhost:8080/api/test/redis/incr?key=counter
```

Expected: 所有接口返回正常响应

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "feat: 添加测试 API 验证基础设施"
```

---

## 验收标准

完成本计划后，应该能够：

1. ✅ 项目成功启动，端口 8080
2. ✅ 数据库表创建成功，包含 5 张核心表
3. ✅ Redis 连接正常，读写操作正常
4. ✅ 统一响应格式生效，异常处理正常工作
5. ✅ 测试 API 可以正常访问
6. ✅ 日志正常输出到控制台和文件

---

## 下一步

完成本计划后，继续 **Plan 2: 竞拍核心功能**，实现竞拍的 CRUD 操作和状态机。
