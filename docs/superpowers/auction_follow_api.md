# 拍卖活动关注功能 API 文档

## 功能概述

本功能允许用户关注待开始的拍卖活动，当活动开始时系统会自动向所有关注者发送WebSocket推送通知。

## 数据库表结构

```sql
CREATE TABLE auction_follows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    auction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auction_user (auction_id, user_id)
);
```

## API接口

### 1. 关注活动

**请求：**
```http
POST /auctions/{auctionId}/follow
Authorization: Bearer {token}
```

**响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": null
}
```

**业务规则：**
- 只有状态为 `PENDING` 的活动可以关注
- 同一用户对同一活动只能关注一次
- 关注成功后记录关注时间

### 2. 取消关注活动

**请求：**
```http
DELETE /auctions/{auctionId}/follow
Authorization: Bearer {token}
```

**响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": null
}
```

### 3. 获取关注状态

**请求：**
```http
GET /auctions/{auctionId}/follow/status
Authorization: Bearer {token}
```

**响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "following": true,
        "followedAt": "2026-06-09T10:30:00",
        "totalFollowers": 42
    }
}
```

### 4. 获取我关注的活动列表

**请求：**
```http
GET /auctions/my/following
Authorization: Bearer {token}
```

**响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 123,
            "title": "艺术品拍卖会",
            "status": "PENDING",
            "startTime": "2026-06-10T14:00:00",
            "endTime": "2026-06-10T16:00:00"
        }
    ]
}
```

## WebSocket推送通知

### 活动开始通知

当商家启动活动时，所有关注者会收到以下WebSocket消息：

```json
{
    "type": "AUCTION_STARTED_FOLLOWER",
    "data": {
        "auctionId": 123,
        "message": "您关注的拍卖活动已开始，快来参与吧！"
    },
    "timestamp": 1234567890,
    "auctionId": 123
}
```

## 完整使用流程

### 用户侧

1. **浏览活动**：用户在活动列表中看到待开始的拍卖活动
2. **关注活动**：点击"关注"按钮，调用 `POST /auctions/{id}/follow`
3. **等待通知**：活动开始前，用户可以正常使用其他功能
4. **接收通知**：活动开始时，收到WebSocket推送通知
5. **参与竞拍**：点击通知跳转到活动详情页，开始出价

### 商家侧

1. **创建活动**：创建拍卖活动，状态为 `PENDING`
2. **等待用户关注**：用户开始关注活动
3. **启动活动**：调用 `POST /auctions/{id}/start`
4. **系统自动通知**：系统自动向所有关注者发送推送通知
5. **正常竞拍**：活动进入竞拍阶段

## 技术特性

### 性能优化

- **异步推送**：所有WebSocket通知都使用异步发送，不阻塞主业务流程
- **索引优化**：数据库表包含完整的索引，查询性能高效
- **缓存机制**：活动详情使用Redis缓存，减少数据库压力

### 容错机制

- **通知失败不影响主流程**：即使关注者通知发送失败，活动启动仍正常进行
- **重复关注保护**：数据库唯一索引防止重复关注
- **状态校验**：只有待开始的活动可以关注

### 扩展性

- **模块化设计**：关注功能独立模块，不影响现有功能
- **接口统一**：遵循项目现有的API设计规范
- **消息类型扩展**：预留给未来更多通知类型

## 测试建议

### 单元测试

1. **关注功能测试**：验证关注、取消关注的基本功能
2. **权限测试**：验证只有待开始活动可以关注
3. **重复关注测试**：验证重复关注的保护机制
4. **查询功能测试**：验证关注状态和列表查询功能

### 集成测试

1. **通知流程测试**：验证从活动启动到用户收到通知的完整流程
2. **WebSocket连接测试**：验证用户在线时能正常收到通知
3. **并发测试**：验证多用户同时关注的并发处理

### 性能测试

1. **大量关注者测试**：验证活动有数千关注者时的通知性能
2. **数据库压力测试**：验证大量关注记录的查询性能

## 注意事项

1. **数据库迁移**：首次部署需要执行SQL脚本创建表
2. **权限配置**：确保所有已登录用户都可以访问关注API
3. **WebSocket连接**：用户需要保持WebSocket连接才能收到实时通知
4. **现有功能兼容**：本功能完全独立，不影响现有拍卖流程