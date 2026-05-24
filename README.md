# 🎯 直播竞拍全栈系统

实时竞拍大师 —— 抖音电商直播竞拍全栈系统设计与实现

## 📋 项目简介

本项目是一个完整的高并发实时竞拍系统，支持：
- 🔄 WebSocket 毫秒级实时同步
- 🔒 Redis 分布式锁保证并发安全
- 🎮 状态机管理竞拍状态
- 🤖 AI 智能出价建议（豆包 API）
- 🤖 自动出价 Agent
- 🛡️ 多维度风控检测

## 🏗️ 技术栈

| 层级 | 技术栈 |
|------|--------|
| 后端 | Java 17 + Spring Boot 3.2 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7.0 |
| 前端 | React 18 + TypeScript + Vite |
| 实时通信 | WebSocket |
| AI | 豆包 API (Doubao-Seed-2.0-lite) |

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- Node.js 18+ (前端)

### 后端启动

1. **创建数据库**
```bash
mysql -u root -p < src/main/resources/schema.sql
```

2. **修改配置**
编辑 `src/main/resources/application-dev.yml`，修改数据库和 Redis 连接信息

3. **启动项目**
```bash
mvn spring-boot:run
```

4. **验证启动**
```bash
curl http://localhost:8080/api/test/ping
```

### 前端启动

```bash
cd auction-frontend
npm install
npm run dev
```

访问 http://localhost:3000

## 📁 项目结构

```
auction-system/
├── src/main/java/com/auction/
│   ├── api/                    # API 接口层
│   ├── common/                 # 公共组件
│   ├── config/                 # 配置类
│   ├── domain/                 # 领域模型
│   ├── infrastructure/         # 基础设施
│   ├── repository/             # 仓储层
│   └── service/                # 业务服务
└── src/main/resources/
    ├── application.yml         # 主配置
    ├── application-dev.yml      # 开发环境配置
    └── schema.sql              # 数据库脚本
```

## 📝 开发文档

- [技术设计文档](docs/superpowers/specs/2026-05-21-auction-system-design.md)
- [Plan 1: 基础设施](docs/superpowers/plans/2026-05-21-plan-01-infrastructure-backend-framework.md)
- [Plan 2: 竞拍核心](docs/superpowers/plans/2026-05-21-plan-02-auction-core.md)
- [Plan 3: 出价+WebSocket](docs/superpowers/plans/2026-05-21-plan-03-bid-websocket.md)
- [Plan 4: 前端页面](docs/superpowers/plans/2026-05-21-plan-04-frontend.md)
- [Plan 5: AI+风控](docs/superpowers/plans/2026-05-21-plan-05-ai-risk.md)

## 🔑 API 示例

```bash
# 健康检查
GET /api/test/ping

# 获取竞拍列表
GET /api/auctions/active

# 获取竞拍详情
GET /api/auctions/{id}

# 提交出价
POST /api/bids
{
  "auctionId": 1,
  "userId": 1,
  "amount": 1000
}

# WebSocket 连接
WS /api/ws/auction/{auctionId}?userId={userId}
```

## 📄 开源协议

MIT License
