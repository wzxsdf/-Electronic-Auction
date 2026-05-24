# 直播竞拍全栈系统 - 技术设计文档

**项目名称**：实时竞拍大师 —— 抖音电商直播竞拍全栈系统
**文档版本**：1.0
**创建日期**：2026-05-21
**团队配置**：3人（1前端 + 2后端），AI 辅助全栈开发

---

## 1. 项目概述

### 1.1 项目背景

直播电商兴起，高价值商品（珠宝、艺术品、二手奢侈品）需要竞拍形式实现动态定价。本项目构建一套支持高并发、实时竞拍的全栈系统。

### 1.2 核心目标

- 实现完整的「商品上架 → 规则配置 → 实时出价 → 动态排名 → 竞拍成交」闭环
- 支持 WebSocket 毫秒级实时同步
- 实现竞拍规则：0元起拍、加价幅度、封顶价、自动延时、异常取消
- 集成 AI 功能作为技术加分项

### 1.3 技术选型

| 层级 | 技术栈 | 说明 |
|------|--------|------|
| 前端 | React 18 + TypeScript + Vite | 现代化前端框架 |
| 状态管理 | Zustand | 轻量级状态管理 |
| 后端 | Java 17 + Spring Boot 3.x | 成熟的企业级框架 |
| 数据库 | MySQL 8.0 | 业务数据持久化 |
| 缓存 | Redis 7.0 | 高频读写 + 分布式锁 |
| 实时通信 | WebSocket | 长连接推送 |
| AI 能力 | 豆包 API (Doubao-Seed-2.0-lite) | 智能建议 + 风控 |

---

## 2. 整体架构

### 2.1 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端层                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌─────────────────┐ │
│  │  移动端 H5       │  │  PC 管理后台     │  │  风控后台面板   │ │
│  │  - 竞价/排名     │  │  - 商品管理      │  │  - 异常告警     │ │
│  │  - 出价建议💡    │  │  - 规则配置      │  │  - 用户画像     │ │
│  │  - 代理出价🤖   │  │                  │  │  - 处置操作     │ │
│  └────────┬─────────┘  └────────┬─────────┘  └─────────────────┘ │
└───────────┼────────────────────┼─────────────────────────────────┘
            │                    │
            │    ┌───────────────┴────────────────┐
            │    │     HTTP + WebSocket            │
            │    └───────────────┬────────────────┘
            │                    │
            ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                       后端层 (Spring Boot)                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │
│  │ 竞拍核心    │  │ WebSocket   │  │  AI 服务模块 🤖          │   │
│  │ Service     │  │ 推送服务    │  │  ┌───────────────────┐   │   │
│  │             │  │             │  │  │ 智能出价建议      │   │   │
│  └─────────────┘  └─────────────┘  │  │ (调用豆包API)     │   │   │
│  ┌─────────────┐  ┌─────────────┘  │  └───────────────────┘   │   │
│  │ 自动出价    │  ┌─────────────┐  │  ┌───────────────────┐   │   │
│  │ Agent       │  │ 风控检测    │  │  │ 反作弊引擎        │   │   │
│  │ (定时任务)  │  │ Service     │  │  │ - 规则引擎        │   │   │
│  └─────────────┘  └─────────────┘  │  │ - 异常检测        │   │   │
│  ┌─────────────┐  ┌─────────────┐  │  │ - 用户评分        │   │   │
│  │ 商品管理    │  │ 状态机引擎  │  │  └───────────────────┘   │   │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│    MySQL     │    │    Redis     │    │ AI Features  │
│  (业务数据)  │    │  (缓存/锁)   │    │  (用户行为)  │
└──────────────┘    └──────────────┘    └──────────────┘
                                                    │
                                                    ▼
                                          ┌──────────────────┐
                                          │  豆包 API        │
                                          │  (Doubao-Seed)   │
                                          └──────────────────┘
```

### 2.2 架构特点

- **单体架构**：单机部署，降低复杂度
- **前后端分离**：通过 REST API + WebSocket 通信
- **Redis 分层**：缓存 + 分布式锁
- **状态机驱动**：竞拍状态严格可控

---

## 3. 数据库设计

### 3.1 MySQL 核心表

```sql
-- 商品表
CREATE TABLE products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL COMMENT '商品名称',
    image_url       VARCHAR(500) COMMENT '商品图片',
    description     TEXT COMMENT '商品描述',
    category        VARCHAR(50) COMMENT '分类',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 竞拍活动表
CREATE TABLE auctions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT NOT NULL COMMENT '关联商品ID',
    title           VARCHAR(200) NOT NULL COMMENT '竞拍标题',
    start_price     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '起拍价',
    bid_increment   DECIMAL(10,2) NOT NULL COMMENT '加价幅度',
    max_price       DECIMAL(10,2) COMMENT '封顶价',
    delay_seconds   INT NOT NULL DEFAULT 15 COMMENT '延时秒数',
    start_time      DATETIME NOT NULL COMMENT '开始时间',
    end_time        DATETIME NOT NULL COMMENT '结束时间（动态延长）',
    original_end_time DATETIME NOT NULL COMMENT '原始结束时间',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_time (start_time, end_time),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 出价记录表
CREATE TABLE bids (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    rank_when_bid   INT COMMENT '出价时的排名',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auction_user (auction_id, user_id),
    INDEX idx_auction_amount (auction_id, amount DESC),
    FOREIGN KEY (auction_id) REFERENCES auctions(id)
);

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
    FOREIGN KEY (auction_id) REFERENCES auctions(id)
);

-- 用户表
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50) UNIQUE NOT NULL,
    nickname        VARCHAR(100),
    avatar_url      VARCHAR(500),
    total_bids      INT DEFAULT 0,
    total_wins      INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- AI 功能相关表
CREATE TABLE user_behaviors (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    auction_id      BIGINT NOT NULL,
    bid_count       INT COMMENT '出价次数',
    avg_bid_interval INT COMMENT '平均出价间隔(秒)',
    last_bid_time   DATETIME COMMENT '最后出价时间',
    risk_score      DECIMAL(3,2) COMMENT '风险评分 0-1',
    risk_level      VARCHAR(20) COMMENT '风险等级',
    is_blocked      BOOLEAN DEFAULT FALSE,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_risk (risk_score, is_blocked)
);

CREATE TABLE auto_bid_configs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    auction_id      BIGINT NOT NULL,
    max_price       DECIMAL(10,2) NOT NULL,
    strategy        VARCHAR(20) NOT NULL COMMENT 'LAST_SEC/SMART/AGGRESSIVE',
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    bid_count       INT DEFAULT 0,
    current_bid     DECIMAL(10,2),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_auction (user_id, auction_id)
);

CREATE TABLE risk_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    auction_id      BIGINT NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    description     TEXT,
    metadata        JSON,
    action_taken    VARCHAR(50),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_severity (severity)
);
```

### 3.2 Redis 数据结构

```
# Key 命名规范：namespace:id:field

# 当前价格
String:auction:{auctionId}:current_price

# 出价次数
String:auction:{auctionId}:bid_count

# 最高出价者
String:auction:{auctionId}:highest_bidder

# 实时排行榜（有序集合，score为出价金额倒序）
ZSet:auction:{auctionId}:leaderboard

# 最近出价记录
List:auction:{auctionId}:recent_bids

# 分布式锁
String:lock:auction:{auctionId}:bid

# 竞拍状态缓存
Hash:auction:{auctionId}:state

# 用户最后出价
String:bid:{auctionId}:user:{userId}:last

# WebSocket 房间成员
Set:ws:room:{auctionId}:members

# 用户出价频率（限流用）
String:bid:count:{userId}:1min
```

---

## 4. 后端模块设计

### 4.1 项目结构

```
auction-system/
├── auction-api/                    # API 接口层
│   └── src/main/java/com/auction/api/
│       ├── controller/             # REST 控制器
│       ├── dto/                    # 数据传输对象
│       ├── websocket/              # WebSocket 处理器
│       └── common/                 # 公共组件
├── auction-service/                # 业务服务层
│   └── src/main/java/com/auction/service/
│       ├── auction/                # 竞拍服务
│       ├── bid/                    # 出价服务
│       ├── product/                # 商品服务
│       ├── order/                  # 订单服务
│       ├── websocket/              # WebSocket 服务
│       └── ai/                     # AI 服务 🤖
│           ├── SuggestionService.java      # 出价建议
│           ├── AutoBidAgent.java             # 自动出价
│           └── RiskDetectionService.java    # 风控检测
├── auction-domain/                 # 领域模型层
│   └── src/main/java/com/auction/domain/
│       ├── entity/                 # 实体类
│       ├── repository/             # 仓储接口
│       └── event/                  # 领域事件
└── auction-infrastructure/         # 基础设施层
    └── src/main/java/com/auction/infra/
        ├── persistence/            # 数据持久化
        ├── cache/                  # Redis 缓存
        └── external/               # 外部服务（豆包 API）
```

### 4.2 核心 API 接口

```
# 竞拍相关
GET    /api/auctions                    # 获取竞拍列表
GET    /api/auctions/{id}               # 获取竞拍详情
POST   /api/auctions                    # 创建竞拍（管理员）
PUT    /api/auctions/{id}/start         # 开始竞拍
PUT    /api/auctions/{id}/cancel        # 取消竞拍

# 出价相关
POST   /api/bids                        # 提交出价
GET    /api/bids/auction/{auctionId}/leaderboard  # 排行榜

# AI 功能 🤖
POST   /api/ai/suggest                  # 获取出价建议
POST   /api/autobid                     # 设置代理出价
DELETE /api/autobid/{auctionId}         # 取消代理出价

# WebSocket
WS     /ws/auction/{auctionId}          # 加入竞拍房间
```

---

## 5. 前端模块设计

### 5.1 项目结构

```
auction-frontend/
├── src/
│   ├── pages/
│   │   ├── mobile/                     # 移动端 H5
│   │   │   ├── LiveRoom.tsx            # 直播间竞拍页（核心）
│   │   │   ├── ProductList.tsx
│   │   │   └── OrderList.tsx
│   │   └── admin/                      # PC 管理后台
│   │       ├── Dashboard.tsx
│   │       ├── AuctionManage.tsx
│   │       └── RiskPanel.tsx           # 风控面板 🤖
│   ├── components/
│   │   ├── auction/                    # 竞拍组件
│   │   ├── ai/                         # AI 组件 🤖
│   │   └── common/                     # 通用组件
│   ├── hooks/                          # 自定义 Hooks
│   │   ├── useWebSocket.ts
│   │   ├── useAuction.ts
│   │   └── useAutoBid.ts
│   ├── services/                       # API 服务
│   │   ├── api.ts
│   │   ├── auctionService.ts
│   │   └── aiService.ts                # AI API 🤖
│   ├── store/                          # 状态管理
│   └── types/                          # TypeScript 类型
```

### 5.2 核心页面布局

**直播间竞拍页**：
- 模拟直播画面
- 当前价格 + 倒计时（毫秒级）
- 实时排行榜
- 出价输入 + 立即出价按钮
- AI 出价建议按钮
- 代理出价开关

---

## 6. WebSocket 实时通信

### 6.1 消息类型

```typescript
enum MessageType {
  // 连接相关
  CONNECT, DISCONNECT, PING, PONG,

  // 竞拍状态
  AUCTION_START, AUCTION_END, AUCTION_EXTENDED, AUCTION_CANCELLED,

  // 出价相关
  NEW_BID, BID_VALIDATED, BID_REJECTED,

  // 状态同步
  PRICE_UPDATE, LEADERBOARD_UPDATE, TIME_UPDATE,

  // 个人通知
  YOU_ARE_LEADING, YOU_WERE_OVERTAKEN, YOU_WON, YOU_LOST,

  // 错误
  ERROR
}
```

### 6.2 房间管理

- 每个 `auctionId` 对应一个 WebSocket 房间
- 房间内成员独立管理，支持在线人数统计
- 支持房间广播、单点推送

### 6.3 心跳保活

- 客户端每 30 秒发送 PING
- 服务端响应 PONG
- 断线自动重连，指数退避策略

---

## 7. 状态机设计

### 7.1 状态定义

```java
public enum AuctionStatus {
    PENDING("待开始"),
    ACTIVE("进行中"),
    PAUSED("已暂停"),
    CANCELLED("已取消"),
    COMPLETED("已结束");
}
```

### 7.2 状态转换

```
PENDING ──START──▶ ACTIVE ──COMPLETE──▶ COMPLETED
   │                   │
   │              PAUSE │
   │                   ▼
   │                PAUSED ──RESUME──▶ ACTIVE
   │                   │
   └───────CANCEL──────┴───────CANCEL──────▶ CANCELLED
```

### 7.3 自动延时机制

- 在结束前 N 秒内有出价，自动延长 N 秒
- 延时后广播所有在线用户
- 重新调度倒计时任务

---

## 8. 分布式锁与高并发

### 8.1 分布式锁实现

```java
// Redis SET NX EX 实现
public boolean tryLock(String key, String requestId) {
    return redisTemplate.opsForValue()
        .setIfAbsent(lockKey, requestId, 10, TimeUnit.SECONDS);
}

// Lua 脚本释放
public boolean unlock(String key, String requestId) {
    // 校验 requestId 后删除
}
```

### 8.2 出价幂等性保证

```
出价请求 → 获取分布式锁 → 校验 → 更新Redis → 写MySQL → 推送 → 释放锁
```

### 8.3 高并发优化

- Redis 缓存优先，MySQL 降级
- 异步写入数据库（可考虑事件驱动）
- 用户出价频率限制
- WebSocket 连接复用

---

## 9. AI 功能模块 🤖

### 9.1 智能出价建议

```
用户点击 → 收集竞拍上下文 → 调用豆包 API → 解析建议 → 展示给用户
```

**输入上下文**：
- 当前价格、剩余时间、出价次数
- 竞争人数、用户历史
- 商品信息

**输出**：
- 建议出价金额
- 建议理由
- 建议时机
- 胜率估计

### 9.2 自动出价 Agent

**策略**：
- `LAST_SEC`：最后 5 秒出价
- `SMART`：分析竞争情况智能决策
- `AGGRESSIVE`：被超越立即出价

**实现**：定时任务每秒扫描激活的代理出价配置

### 9.3 风控检测引擎

**检测维度**：
1. 频率检测：短时间出价次数
2. 金额模式：固定间隔（机器人特征）
3. 新用户检测：注册时间 + 高频行为
4. 支付历史：弃单率

**风险等级**：LOW / MEDIUM / HIGH / CRITICAL

**处置动作**：
- ALLOW：正常通过
- WARN：警告
- BLOCK：拒绝
- MANUAL_REVIEW：人工审核

---

## 10. 开发协作

### 10.1 团队分工

| 成员 | 负责模块 |
|------|---------|
| 前端 (1人) | H5 页面、管理后台、AI 组件 |
| 后端甲 (1人) | 竞拍核心、商品管理、订单服务 |
| 后端乙 (1人) | 出价服务、WebSocket、AI 服务 |

### 10.2 前后端协作流程

```
接口定义 → Mock 数据 → 并行开发 → 联调测试
```

### 10.3 Git 分支策略

```
main (生产)
  └─ develop (开发)
       ├─ feature/auction-core
       ├─ feature/bid-service
       ├─ feature/websocket
       ├─ feature/ai-risk
       ├─ feature/live-room
       └─ feature/admin-panel
```

### 10.4 API 规范

**统一响应格式**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1716254400000
}
```

---

## 11. 部署方案

### 11.1 服务器配置

- CPU: 4核
- 内存: 8GB
- 硬盘: 40GB SSD
- 系统: Ubuntu 22.04

### 11.2 软件环境

- JDK 17
- MySQL 8.0
- Redis 7.0
- Nginx（反向代理）

### 11.3 部署架构

```
Nginx :80
  ├── / (前端静态资源)
  ├── /api/ (后端 API 反向代理)
  └── /ws/ (WebSocket 反向代理)
      └── Spring Boot :8080
          ├── MySQL :3306
          └── Redis :6379
```

---

## 12. 开发时间规划

| 时间 | 内容 |
|------|------|
| 第一周 (5.21-5.27) | 环境搭建、项目脚手架、数据库设计 |
| 第二周 (5.28-6.03) | 核心功能开发（竞拍、出价、状态机） |
| 第三周 (6.04-6.10) | WebSocket + AI 功能 + 风控 |
| 6.11-6.12 | 测试、部署、演示准备 |

---

## 13. 风险与应对

| 风险 | 应对 |
|------|------|
| WebSocket 连接不稳定 | 心跳保活 + 断线重连 + HTTP 降级 |
| 并发出价冲突 | Redis 分布式锁 |
| Redis 不可用 | 降级到 MySQL（性能降低但可用） |
| AI API 调用失败 | 降级到规则引擎 |

---

## 附录

### A. 豆包 API 配置

```
模型：Doubao-Seed-2.0-lite
EP：ep-20260514111437-7crsm
APIKEY：ark-4126af52-1fda-4c17-8561-8db89e066502-95563
```

### B. 技术亮点总结

1. **WebSocket 房间级隔离**：支持多直播间互不干扰
2. **状态机驱动**：竞拍状态严格可控，转换可追溯
3. **分布式锁**：Redis SET NX 保证出价幂等性
4. **AI 智能建议**：调用豆包 API 分析竞拍态势
5. **自动出价 Agent**：多种策略自动代理出价
6. **风控检测引擎**：多维度异常识别
