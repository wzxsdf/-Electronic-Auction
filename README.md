# 🎯 直播竞拍全栈系统

> 一个高并发、分布式的实时竞拍系统，支持毫秒级实时同步、智能出价和完整业务闭环

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.8-green)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 📋 项目简介

本项目是一个企业级的高并发实时竞拍系统，采用 DDD（领域驱动设计）架构，支持完整的拍卖业务闭环。系统具备以下核心能力：

### 核心特性

| 特性 | 说明 |
|------|------|
| 🔄 **实时通信** | WebSocket 毫秒级消息推送，支持竞拍状态实时同步 |
| 🔒 **并发安全** | Redis 分布式锁 + 乐观锁 + 限流拦截器三重防护 |
| 🤖 **智能出价** | AI 出价建议（豆包 API）+ 自动出价 Agent |
| 🛡️ **风控系统** | 多维度风险检测，实时拦截异常行为 |
| 💰 **支付结算** | 完整的订单、支付、结算业务闭环 |
| 👥 **权限管理** | RBAC 权限模型 + JWT 认证 |
| 📦 **商品管理** | 支持 OSS 直传、多图上传、商品审核 |
| 📊 **数据统计** | 出价统计、用户行为分析、风控事件记录 |

---

## 🏗️ 技术栈

### 后端技术

| 层级 | 技术栈 | 版本 |
|------|--------|------|
| 应用框架 | Spring Boot | 3.1.8 |
| 开发语言 | Java | 17 |
| 数据库 | MySQL | 8.0+ |
| 缓存/分布式锁 | Redis + Redisson | 7.0+ / 3.25.0 |
| ORM | MyBatis-Plus | 3.5.5 |
| 连接池 | Druid | 1.2.23 |
| 安全认证 | Spring Security + JWT | - |
| 实时通信 | WebSocket | - |
| 异步处理 | Spring Async | - |
| 参数校验 | Jakarta Validation | - |
| 文件存储 | 阿里云 OSS | 3.17.3 |
| API 文档 | - | - |

### 前端技术

| 层级 | 技术栈 |
|------|--------|
| 框架 | React 18 |
| 语言 | TypeScript |
| 构建工具 | Vite |
| 状态管理 | - |
| UI 组件 | - |

---

## 📁 项目结构

```
auction-system/
├── src/main/
│   ├── java/com/auction/
│   │   ├── api/                      # API 接口层
│   │   │   ├── controller/           # REST 控制器
│   │   │   └── dto/                  # 数据传输对象
│   │   ├── application/              # 应用服务层
│   │   ├── common/                   # 公共组件
│   │   │   ├── Result.java          # 统一响应封装
│   │   │   ├── BaseException.java   # 基础异常
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── config/                   # 配置类
│   │   │   ├── SecurityConfig.java  # 安全配置
│   │   │   ├── RedissonConfig.java  # 分布式锁配置
│   │   │   ├── AsyncConfig.java     # 异步配置
│   │   │   └── OssConfig.java       # OSS 配置
│   │   ├── domain/                   # 领域模型层
│   │   │   ├── entity/              # 实体
│   │   │   ├── enums/               # 枚举
│   │   │   ├── valueobject/        # 值对象
│   │   │   └── service/            # 领域服务
│   │   ├── infrastructure/          # 基础设施层
│   │   │   ├── mapper/             # MyBatis Mapper
│   │   │   ├── security/           # 安全相关
│   │   │   ├── limiter/            # 限流器
│   │   │   └── lock/               # 分布式锁
│   │   ├── repository/              # 仓储层
│   │   └── service/                 # 业务服务层
│   │       ├── auth/               # 认证服务
│   │       ├── user/               # 用户服务
│   │       ├── product/            # 商品服务
│   │       ├── auction/            # 拍卖服务
│   │       ├── payment/            # 支付服务
│   │       ├── order/              # 订单服务
│   │       ├── websocket/          # WebSocket 服务
│   │       ├── scheduler/          # 调度服务
│   │       └── notification/      # 通知服务
│   └── resources/
│       ├── application.yml         # 主配置
│       ├── application-dev.yml     # 开发环境配置
│       └── db/                     # 数据库脚本
├── docs/                           # 项目文档
│   ├── api_documentation/         # API 文档
│   ├── database/                  # 数据库文档
│   ├── module_introduction/       # 模块介绍
│   └── superpowers/               # 技术文档
├── src/test/                      # 测试代码
├── pom.xml                        # Maven 配置
├── start.sh                       # Linux 启动脚本
├── start.bat                      # Windows 启动脚本
└── README.md                      # 本文件
```

---

## 🚀 快速开始

### 环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |
| Node.js | 18+ (前端) |

### 数据库初始化

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS auction DEFAULT CHARSET utf8mb4;"

# 2. 执行数据库脚本
mysql -u root -p auction < src/main/resources/db/schema.sql
```

### 配置文件

编辑 `src/main/resources/application-dev.yml`，配置以下信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auction
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

# OSS 配置（可选）
aliyun:
  oss:
    endpoint: your_endpoint
    accessKeyId: your_access_key
    accessKeySecret: your_secret
    bucketName: your_bucket
```

### 启动应用

**Linux/Mac:**
```bash
chmod +x start.sh
./start.sh
```

**Windows:**
```cmd
start.bat
```

**或使用 Maven:**
```bash
mvn spring-boot:run
```

### 验证启动

```bash
curl http://localhost:8080/api/test/ping
```

返回 `pong` 表示启动成功。

---

## 🔑 核心功能模块

### 1. 用户认证与权限

- **功能**: 用户注册/登录、JWT 认证、RBAC 权限管理
- **技术**: Spring Security + JWT
- **文档**: [用户认证技术文档](docs/module_introduction/用户认证和权限系统技术文档.md)

### 2. 商品管理

- **功能**: 商品上架、编辑、删除、图片上传、商品审核
- **技术**: 阿里云 OSS 直传、MyBatis-Plus
- **文档**: [商品管理技术文档](docs/module_introduction/商品管理系统技术文档.md)

### 3. 拍卖管理

- **功能**: 创建拍卖、拍卖调度、自动延时、自动结算
- **技术**: 状态机、分布式调度
- **文档**: [拍卖管理技术文档](docs/module_introduction/拍卖管理系统技术文档.md)

### 4. 出价管理

- **功能**: 实时出价、出价排行、自动出价、并发控制
- **技术**: Redis 分布式锁、乐观锁、限流
- **文档**: [出价管理技术文档](docs/module_introduction/出价管理系统技术文档.md)

### 5. 订单管理

- **功能**: 订单创建、订单查询、订单状态流转
- **技术**: 状态机、事务管理
- **文档**: [订单管理技术文档](docs/module_introduction/订单管理系统技术文档.md)

### 6. 支付管理

- **功能**: 支付创建、支付回调、支付状态同步
- **技术**: 支付接口集成、异步通知
- **文档**: [支付管理技术文档](docs/module_introduction/支付管理系统技术文档.md)

### 7. WebSocket 实时通信

- **功能**: 拍卖室推送、出价通知、聊天室
- **技术**: Spring WebSocket、消息广播
- **文档**: [WebSocket 技术文档](docs/module_introduction/WebSocket实时通讯系统技术文档.md)

---

## 📡 API 接口

### 认证接口

```bash
# 用户注册
POST /api/auth/register
Content-Type: application/json

{
  "username": "test_user",
  "password": "password123",
  "email": "test@example.com"
}

# 用户登录
POST /api/auth/login
Content-Type: application/json

{
  "username": "test_user",
  "password": "password123"
}

# 刷新 Token
POST /api/auth/refresh
Authorization: Bearer <refresh_token>
```

### 拍卖接口

```bash
# 获取活跃拍卖列表
GET /api/auctions/active
Authorization: Bearer <access_token>

# 获取拍卖详情
GET /api/auctions/{id}
Authorization: Bearer <access_token>

# 创建拍卖
POST /api/auctions
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "productId": 1,
  "startPrice": 1000,
  "minIncrement": 100,
  "startTime": "2026-06-10T10:00:00",
  "endTime": "2026-06-10T12:00:00"
}
```

### 出价接口

```bash
# 提交出价
POST /api/bids
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "auctionId": 1,
  "amount": 1500
}

# 获取出价排行
GET /api/bids/rank?auctionId=1
Authorization: Bearer <access_token>
```

### WebSocket 连接

```
# 连接拍卖室
WS /api/ws/auction/{auctionId}?userId={userId}&token={jwt_token}

# 聊天室连接
WS /api/ws/chat/{auctionId}?userId={userId}&token={jwt_token}
```

### 完整 API 文档

详见：[API 文档](docs/api_documentation/)

---

## 🔒 并发安全机制

系统采用四层并发防护机制：

```
请求 → 限流拦截器 → 分布式锁 → 业务逻辑 → 乐观锁 → 异步推送
       (Redis)      (Redisson)              (@Version)   (线程池)
```

| 层级 | 技术 | 阈值/配置 |
|------|------|-----------|
| 第一层 | Redis 限流 | 出价 30 次/分钟 |
| 第二层 | Redisson 分布式锁 | 等待 5 秒，超时 10 秒 |
| 第三层 | MyBatis-Plus 乐观锁 | @Version 字段 |
| 第四层 | 异步消息推送 | 独立线程池 |

详细说明：[并发升级文档](CONCURRENCY_UPGRADE.md)

---

## 🧪 测试

### 运行单元测试

```bash
mvn test
```

### 并发测试

```bash
mvn test -Dtest=ConcurrentBidTest
```

### 性能指标

| 指标 | 优化前 | 优化后 |
|------|--------|--------|
| 单实例 QPS | ~100 | 500+ |
| 出价响应时间 | ~50ms | ~20ms |
| 并发冲突保护 | ❌ | ✅ |
| 分布式支持 | ❌ | ✅ |

---

## 📊 数据库设计

### 核心表结构

| 表名 | 说明 |
|------|------|
| users | 用户表 |
| roles | 角色表 |
| permissions | 权限表 |
| products | 商品表 |
| auctions | 拍卖表 |
| auction_items | 拍卖场次表 |
| bids | 出价记录表 |
| orders | 订单表 |
| payments | 支付记录表 |
| risk_events | 风控事件表 |

数据库文档：[数据库使用说明](docs/database/数据库使用说明.md)

---

## 📚 项目文档

- [技术设计文档](docs/superpowers/specs/)
- [模块技术文档](docs/module_introduction/)
- [API 文档](docs/api_documentation/)
- [数据库文档](docs/database/)
- [并发安全升级](CONCURRENCY_UPGRADE.md)

---

## 🛠️ 开发指南

### 代码规范

- 遵循阿里巴巴 Java 开发规范
- 使用 Lombok 简化代码
- 统一异常处理
- RESTful API 设计

### 提交规范

```bash
# 功能开发
git commit -m "feat: 添加新功能描述"

# Bug 修复
git commit -m "fix: 修复问题描述"

# 文档更新
git commit -m "docs: 更新文档描述"
```

### 分支策略

- `master`: 生产环境
- `develop`: 开发环境
- `feature/*`: 功能分支
- `fix/*`: 修复分支

---

## ❓ 常见问题

### 问题 1: Redis 连接失败

**错误信息**: `Cannot connect to Redis`

**解决方案**:
```bash
# 检查 Redis 是否运行
redis-cli ping

# 启动 Redis
redis-server
```

### 问题 2: 获取锁失败

**错误信息**: `IllegalStateException: 获取锁失败，系统繁忙`

**原因**: 锁等待超时（5秒）

**解决方案**: 检查是否有长时间运行的出价操作，或调整超时配置。

### 问题 3: 出价冲突

**错误信息**: `HTTP 409: 出价冲突，请重试`

**原因**: 乐观锁版本号冲突

**解决方案**: 客户端自动重试即可。

---

## 📄 开源协议

MIT License

---

## 👥 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📞 联系方式

如有问题，请联系项目维护者。
