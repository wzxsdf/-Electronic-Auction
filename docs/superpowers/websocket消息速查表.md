# WebSocket消息速查表

## 快速参考

| 消息类型 | 传输方式 | 触发场景 | 调用位置 | 频率 |
|---------|---------|---------|---------|------|
| **NEW_BID** | 房间广播 | 用户出价 | AuctionItemService.doPlaceBid() | 高频 |
| **PRICE_UPDATE** | 房间广播 | 价格变化 | AuctionItemService.doPlaceBid() | 高频 |
| **YOU_ARE_LEADING** | 用户定向 | 成为最高出价者 | AuctionItemService.notifyBidSuccess() | 高频 |
| **YOU_WERE_OVERTAKEN** | 用户定向 | 被其他用户超越 | AuctionItemService.notifyBidSuccess() | 高频 |
| **AUCTION_STARTED** | 房间广播 | 拍品开始 | AuctionItemService.startItem() | 中频 |
| **AUCTION_ENDED** | 房间广播 | 拍品结束 ⭐ | AuctionItemService.endItem() | 中频 |
| **AUCTION_DELAYED** | 房间广播 | 延时触发 | AuctionItemService.triggerDelay() | 中频 |
| **PAYMENT_SUCCESS** | 用户定向 | 支付成功 | PaymentService.completePayment() | 低频 |
| **PAYMENT_CANCELLED** | 用户定向 | 支付取消 | PaymentService.cancelPayment() | 低频 |
| **BID_FAILED** | 用户定向 | 出价失败 | NotificationService.notifyBidFailed() | 低频 |
| **YOU_LOST** | 用户定向 | 未成交 | NotificationService.notifyLost() | 低频 |
| **AUCTION_CANCELLED** | 房间广播 | 拍品取消 | NotificationService (已废弃) | 低频 |
| **YOU_WON** | 用户定向 | 竞拍成功 | 未使用 | - |
| **AUCTION_STARTED_FOLLOWER** | 用户定向 | 关注活动开始 | NotificationService (已废弃) | 低频 |

## 消息数据结构速查

### 出价相关
```json
// NEW_BID
{
  "bidId": 123,
  "auctionItemId": 456,
  "userId": 1001,
  "username": "张三***",
  "amount": 1500.00,
  "rank": 1,
  "isAutoBid": false,
  "bidTime": "2026-06-10T10:30:00"
}

// PRICE_UPDATE
{
  "auctionItemId": 456,
  "currentPrice": 1500.00,
  "highestBidder": 1001
}
```

### 用户通知
```json
// YOU_ARE_LEADING
{
  "auctionId": 789,
  "amount": 1500.00
}

// YOU_WERE_OVERTAKEN
{
  "auctionId": 789,
  "currentPrice": 1600.00
}

// BID_FAILED
{
  "auctionId": 789,
  "reason": "出价金额过低",
  "message": "出价失败：出价金额过低"
}
```

### 拍品状态
```json
// AUCTION_STARTED
{
  "auctionItemId": 456,
  "endTime": "2026-06-10T12:00:00",
  "endTimeTimestamp": 1700005200000,
  "message": "拍品竞拍已开始"
}

// AUCTION_ENDED ⭐ (最新实现，含中标者信息)
{
  "auctionItemId": 456,
  "winnerId": 1001,
  "winnerUsername": "张三***",
  "finalPrice": 1500.00,
  "hasBids": true,
  "message": "拍品已成交，中标用户：张三***"
}

// AUCTION_DELAYED
{
  "auctionItemId": 456,
  "newEndTime": "2026-06-10T12:00:15",
  "newEndTimeTimestamp": 1700005215000,
  "delayCount": 1,
  "message": "拍品竞拍时间延长！"
}
```

### 支付相关
```json
// PAYMENT_SUCCESS
{
  "orderId": 2001,
  "amount": 1500.00,
  "message": "支付成功！感谢您的购买"
}

// PAYMENT_CANCELLED
{
  "orderId": 2001,
  "message": "支付已取消"
}
```

## 房间 vs 用户定向

### 🏠 房间广播 (`broadcastToRoom`)
- 目标: `item:{auctionItemId}` 房间所有用户
- 适用: 公共事件、状态变化
- 消息类型: NEW_BID, PRICE_UPDATE, AUCTION_STARTED, AUCTION_ENDED, AUCTION_DELAYED, AUCTION_CANCELLED

### 👤 用户定向 (`sendToUser`)
- 目标: 特定用户
- 适用: 个人化通知、敏感信息
- 消息类型: YOU_ARE_LEADING, YOU_WERE_OVERTAKEN, YOU_LOST, YOU_WON, BID_FAILED, PAYMENT_SUCCESS, PAYMENT_CANCELLED, AUCTION_STARTED_FOLLOWER

## 最新更新 ✨

### AUCTION_ENDED 消息增强 (2026-06-10)
- ✅ 新增 `winnerUsername` 字段 - 显示中标者用户名(脱敏)
- ✅ 改进 `message` 字段 - 明确显示中标者信息
- ✅ 优化广播范围 - 直播间所有人可见

**实现位置**: `AuctionItemService.endItem()` + `WsMessageService.sendAuctionItemEnded()`

## 完整业务流程消息序列

```
┌─────────────────────────────────────────────────────────────┐
│                    拍品生命周期消息流                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. [开始] AUCTION_STARTED → "拍品竞拍已开始"               │
│     └── 房间广播 (item:456)                                 │
│                                                             │
│  2. [出价] NEW_BID → "用户 张三*** 出价 1500.00"           │
│     ├── PRICE_UPDATE → "当前价格 1500.00, 最高出价者 1001"  │
│     └── YOU_ARE_LEADING → 用户1001 (定向)                   │
│                                                             │
│  3. [超越] NEW_BID → "用户 李四*** 出价 1600.00"           │
│     ├── PRICE_UPDATE → "当前价格 1600.00, 最高出价者 1002"  │
│     ├── YOU_ARE_LEADING → 用户1002 (定向)                   │
│     └── YOU_WERE_OVERTAKEN → 用户1001 (定向)                │
│                                                             │
│  4. [延时] AUCTION_DELAYED → "延时15秒, 剩余2次"            │
│     └── 房间广播 (item:456)                                 │
│                                                             │
│  5. [结束] AUCTION_ENDED → "拍品已成交, 中标用户: 李四***"  │
│     └── 房间广播 (item:456) ⭐                              │
│                                                             │
│  6. [订单] (数据库操作: 订单创建)                           │
│                                                             │
│  7. [支付] PAYMENT_SUCCESS → "支付成功! 感谢您的购买"       │
│     └── 用户1002 (定向)                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 技术细节

### 异步执行
```java
@Async("websocketTaskExecutor")
public void methodName(...) {
    // 所有WebSocket消息都是异步发送，不阻塞主线程
}
```

### 用户名脱敏
```java
private String maskUsername(String username) {
    if (username == null || username.length() <= 2) {
        return "***";
    }
    return username.substring(0, 2) + "***"; // "张三丰" → "张三***"
}
```

### 消息格式标准化
```java
private Map<String, Object> createMessage(MessageType type, Object data, Long auctionId) {
    return Map.of(
        "type", type.name(),           // "NEW_BID"
        "data", data,                  // 业务数据
        "timestamp", System.currentTimeMillis(), // 时间戳
        "auctionId", auctionId         // 关联ID (可为null)
    );
}
```

---

**快速参考版本**: v1.0  
**更新时间**: 2026-06-10  
**用途**: 开发调试、API对接、问题排查