# WebSocket消息统计报告

## 📊 概览

本报告统计了拍卖系统中所有WebSocket消息传输的地方，包括消息类型、数据结构和使用场景。

**消息服务类**: `WsMessageService.java`
**消息类型总数**: 14种
**传输方式**: 房间广播 (`broadcastToRoom`) + 用户定向 (`sendToUser`)

---

## 🏠 消息传输方式

### 1. 房间广播 (Broadcast)
- **目标**: `item:{auctionItemId}` 房间内所有在线用户
- **用途**: 公共事件通知，如出价、价格更新、拍品状态变化

### 2. 用户定向 (Unicast)  
- **目标**: 特定用户
- **用途**: 个人化通知，如领先/被超越、支付结果

---

## 📋 消息详细分类

### 1️⃣ 出价相关消息

#### 1.1 NEW_BID - 新出价广播
```java
// 方法: broadcastNewBid(AuctionItem item, Bid bid, String username, Integer rank)
// 传输方式: 房间广播
// 调用位置: AuctionItemService.doPlaceBid()
```
**消息数据结构**:
```json
{
  "type": "NEW_BID",
  "data": {
    "bidId": 123,                    // 出价记录ID
    "auctionItemId": 456,           // 拍品ID
    "auctionId": 789,               // 拍卖活动ID
    "userId": 1001,                 // 出价用户ID
    "username": "张三***",          // 脱敏用户名
    "amount": 1500.00,              // 出价金额
    "rank": 1,                       // 当前排名
    "isAutoBid": false,             // 是否自动出价
    "bidTime": "2026-06-10T10:30:00" // 出价时间
  },
  "timestamp": 1700000000000,
  "auctionId": 789
}
```

#### 1.2 PRICE_UPDATE - 价格更新广播
```java
// 方法: broadcastItemPriceUpdate(Long auctionItemId)
// 传输方式: 房间广播
// 调用位置: AuctionItemService.doPlaceBid()
```
**消息数据结构**:
```json
{
  "type": "PRICE_UPDATE",
  "data": {
    "auctionItemId": 456,           // 拍品ID
    "currentPrice": 1500.00,        // 当前最高价
    "highestBidder": 1001           // 当前最高出价者ID
  },
  "timestamp": 1700000000000,
  "auctionId": 456
}
```

#### 1.3 BID_FAILED - 出价失败通知
```java
// 方法: sendBidFailed(Long userId, Long auctionId, String reason)
// 传输方式: 用户定向
// 调用位置: NotificationService.notifyBidFailed()
```
**消息数据结构**:
```json
{
  "type": "BID_FAILED",
  "data": {
    "auctionId": 789,               // 拍品ID
    "reason": "出价金额过低",        // 失败原因
    "message": "出价失败：出价金额过低"
  },
  "timestamp": 1700000000000,
  "auctionId": 789
}
```

---

### 2️⃣ 用户状态通知消息

#### 2.1 YOU_ARE_LEADING - 领先通知
```java
// 方法: sendYouAreLeading(Long userId, Long auctionId, BigDecimal amount)
// 传输方式: 用户定向
// 调用位置: AuctionItemService.notifyBidSuccess()
```
**消息数据结构**:
```json
{
  "type": "YOU_ARE_LEADING",
  "data": {
    "auctionId": 789,               // 拍品ID
    "amount": 1500.00               // 当前领先价格
  },
  "timestamp": 1700000000000,
  "auctionId": 789
}
```

#### 2.2 YOU_WERE_OVERTAKEN - 被超越通知
```java
// 方法: sendYouWereOvertaken(Long userId, Long auctionId, BigDecimal currentPrice)
// 传输方式: 用户定向
// 调用位置: AuctionItemService.notifyBidSuccess()
```
**消息数据结构**:
```json
{
  "type": "YOU_WERE_OVERTAKEN",
  "data": {
    "auctionId": 789,               // 拍品ID
    "currentPrice": 1600.00         // 超越您的价格
  },
  "timestamp": 1700000000000,
  "auctionId": 789
}
```

#### 2.3 YOU_LOST - 未成交通知
```java
// 方法: sendYouLost(Long userId, Long auctionId)
// 传输方式: 用户定向
// 调用位置: NotificationService.notifyLost()
```
**消息数据结构**:
```json
{
  "type": "YOU_LOST",
  "data": {
    "auctionId": 789,               // 拍品ID
    "message": "很遗憾，您未成交"
  },
  "timestamp": 1700000000000,
  "auctionId": 789
}
```

#### 2.4 YOU_WON - 成交通知
```java
// 方法: sendYouWon(Long userId, Long auctionId, BigDecimal finalAmount)
// 传输方式: 用户定向
// 调用位置: 暂未使用 (预留)
```
**消息数据结构**:
```json
{
  "type": "YOU_WON",
  "data": {
    "auctionId": 789,               // 拍品ID
    "finalAmount": 1500.00,         // 成交金额
    "message": "恭喜您竞拍成功！请及时支付"
  },
  "timestamp": 1700000000000,
  "auctionId": 789
}
```

---

### 3️⃣ 拍品状态变化消息

#### 3.1 AUCTION_STARTED - 拍品开始通知
```java
// 方法: sendAuctionItemStarted(Long auctionItemId, LocalDateTime endTime)
// 传输方式: 房间广播
// 调用位置: AuctionItemService.startItem()
```
**消息数据结构**:
```json
{
  "type": "AUCTION_STARTED",
  "data": {
    "auctionItemId": 456,           // 拍品ID
    "endTime": "2026-06-10T12:00:00",      // ISO 8601格式结束时间
    "endTimeTimestamp": 1700005200000,     // 时间戳(毫秒)
    "message": "拍品竞拍已开始"
  },
  "timestamp": 1700000000000,
  "auctionId": 456
}
```

#### 3.2 AUCTION_ENDED - 拍品结束通知 ⭐
```java
// 方法: sendAuctionItemEnded(Long auctionItemId, Long winnerId, BigDecimal finalPrice, String winnerUsername, boolean hasBids)
// 传输方式: 房间广播
// 调用位置: AuctionItemService.endItem()
// 特点: 包含中标者信息，直播间所有人可见
```
**消息数据结构**:
```json
{
  "type": "AUCTION_ENDED",
  "data": {
    "auctionItemId": 456,           // 拍品ID
    "winnerId": 1001,               // 中标者ID
    "winnerUsername": "张三***",    // 脱敏中标者用户名
    "finalPrice": 1500.00,          // 成交价格
    "hasBids": true,                // 是否成交
    "message": "拍品已成交，中标用户：张三***"
  },
  "timestamp": 1700000000000,
  "auctionId": 456
}
```

#### 3.3 AUCTION_DELAYED - 延时通知
```java
// 方法: sendAuctionItemDelay(Long auctionItemId, LocalDateTime newEndTime, Integer delayCount)
// 传输方式: 房间广播
// 调用位置: AuctionItemService.triggerDelay()
```
**消息数据结构**:
```json
{
  "type": "AUCTION_DELAYED",
  "data": {
    "auctionItemId": 456,           // 拍品ID
    "newEndTime": "2026-06-10T12:00:15",     // ISO 8601格式新结束时间
    "newEndTimeTimestamp": 1700005215000,   // 时间戳(毫秒)
    "delayCount": 1,                // 延时次数
    "message": "拍品竞拍时间延长！"
  },
  "timestamp": 1700000000000,
  "auctionId": 456
}
```

#### 3.4 AUCTION_CANCELLED - 取消通知
```java
// 方法: sendAuctionCancelled(Long auctionId, String reason)
// 传输方式: 房间广播
// 调用位置: NotificationService.notifyAuctionStatusChanged()
// 状态: 已废弃 (Auction级别方法)
```
**消息数据结构**:
```json
{
  "type": "AUCTION_CANCELLED",
  "data": {
    "auctionId": 789,               // 拍品ID
    "reason": "竞拍已取消",         // 取消原因
    "message": "竞拍已取消"
  },
  "timestamp": 1700000000000,
  "auctionId": 789
}
```

---

### 4️⃣ 支付相关消息

#### 4.1 PAYMENT_SUCCESS - 支付成功通知
```java
// 方法: sendPaymentSuccess(Long userId, Long orderId, BigDecimal amount)
// 传输方式: 用户定向
// 调用位置: PaymentService.completePayment()
```
**消息数据结构**:
```json
{
  "type": "PAYMENT_SUCCESS",
  "data": {
    "orderId": 2001,                // 订单ID
    "amount": 1500.00,              // 支付金额
    "message": "支付成功！感谢您的购买"
  },
  "timestamp": 1700000000000,
  "auctionId": null
}
```

#### 4.2 PAYMENT_CANCELLED - 支付取消通知
```java
// 方法: sendPaymentCancelled(Long userId, Long orderId)
// 传输方式: 用户定向
// 调用位置: PaymentService.cancelPayment()
```
**消息数据结构**:
```json
{
  "type": "PAYMENT_CANCELLED",
  "data": {
    "orderId": 2001,                // 订单ID
    "message": "支付已取消"
  },
  "timestamp": 1700000000000,
  "auctionId": null
}
```

---

### 5️⃣ 关注者通知消息

#### 5.1 AUCTION_STARTED_FOLLOWER - 活动开始通知
```java
// 方法: sendAuctionStartedToFollower(Long userId, Long auctionId)
// 传输方式: 用户定向
// 调用位置: NotificationService.notifyAuctionStartedToFollower()
// 状态: 已废弃 (Auction级别方法)
```
**消息数据结构**:
```json
{
  "type": "AUCTION_STARTED_FOLLOWER",
  "data": {
    "auctionId": 789,               // 活动ID
    "message": "您关注的拍卖活动已开始，快来参与吧！"
  },
  "timestamp": 1700000000000,
  "auctionId": 789
}
```

---

## 🔧 废弃方法列表

以下方法是Auction级别的遗留方法，建议不再使用：

1. `broadcastNewBid(Auction auction, ...)` → 使用 `broadcastNewBid(AuctionItem item, ...)`
2. `broadcastPriceUpdate(Long auctionId)` → 使用 `broadcastItemPriceUpdate(Long auctionItemId)`
3. `sendAuctionEnded(Long auctionId, ...)` → 使用 `sendAuctionItemEnded(Long auctionItemId, ...)`
4. `sendAuctionStarted(Long auctionId)` → 使用 `sendAuctionItemStarted(Long auctionItemId, ...)`
5. `sendAuctionCancelled(Long auctionId, ...)` → 待实现AuctionItem版本
6. `sendAuctionDelayed(Long auctionId, ...)` → 使用 `sendAuctionItemDelay(Long auctionItemId, ...)`
7. `sendAuctionStartedToFollower(...)` → 待实现AuctionItem版本

---

## 📈 使用频率统计

### 高频消息 (每次出价触发)
1. **NEW_BID** - 新出价广播
2. **PRICE_UPDATE** - 价格更新
3. **YOU_ARE_LEADING** - 领先通知
4. **YOU_WERE_OVERTAKEN** - 被超越通知

### 中频消息 (状态变化触发)
1. **AUCTION_STARTED** - 拍品开始
2. **AUCTION_ENDED** - 拍品结束
3. **AUCTION_DELAYED** - 延时通知

### 低频消息 (支付/异常触发)
1. **PAYMENT_SUCCESS** - 支付成功
2. **PAYMENT_CANCELLED** - 支付取消
3. **BID_FAILED** - 出价失败
4. **AUCTION_CANCELLED** - 拍品取消

---

## 🛠️ 消息格式标准

所有WebSocket消息遵循统一格式：

```json
{
  "type": "MESSAGE_TYPE",           // 枚举类型，全大写下划线分隔
  "data": {                        // 具体数据内容
    // ... 业务相关字段
  },
  "timestamp": 1700000000000,       // Unix时间戳(毫秒)
  "auctionId": 789                 // 关联的拍品ID(可能为null)
}
```

### 数据规范
- **时间格式**: ISO 8601 (`2026-06-10T10:30:00`) + 时间戳(毫秒)
- **金额**: `BigDecimal`，保留2位小数
- **用户名**: 自动脱敏 (`张三丰` → `张三***`)
- **房间标识**: `item:{auctionItemId}`

---

## 🎯 业务场景覆盖

### 完整拍卖流程的消息流

```
1. 拍品开始 → AUCTION_STARTED (房间广播)
   ↓
2. 用户出价 → NEW_BID (房间广播)
             PRICE_UPDATE (房间广播)  
             YOU_ARE_LEADING (用户定向)
   ↓
3. 被超越 → YOU_WERE_OVERTAKEN (用户定向)
   ↓
4. 延时触发 → AUCTION_DELAYED (房间广播)
   ↓
5. 拍品结束 → AUCTION_ENDED (房间广播，含中标者)
   ↓
6. 生成订单 → (订单创建)
   ↓
7. 用户支付 → PAYMENT_SUCCESS (用户定向)
```

### 异常流程的消息流

```
1. 出价失败 → BID_FAILED (用户定向)
2. 支付取消 → PAYMENT_CANCELLED (用户定向)
3. 拍品取消 → AUCTION_CANCELLED (房间广播)
```

---

## 📝 待实现功能

基于当前消息类型，以下功能可以扩展：

1. **实时排行榜** - 基于 `LEADERBOARD_UPDATE` 消息类型
2. **聊天功能** - 基于 `CHAT_MESSAGE` 消息类型  
3. **在线人数** - 基于 `ONLINE_COUNT_UPDATE` 消息类型
4. **用户加入/离开** - 基于 `USER_JOIN`/`USER_LEAVE` 消息类型
5. **系统公告** - 基于 `SYSTEM_MESSAGE` 消息类型

---

## 🔍 关键发现

### ✅ 优点
1. **消息分类清晰**: 出价、状态、支付、用户通知分离明确
2. **实时性好**: 使用异步执行器 `@Async("websocketTaskExecutor")`
3. **错误处理完善**: 每个方法都有try-catch和日志记录
4. **用户隐私保护**: 用户名自动脱敏处理

### ⚠️ 改进建议
1. **废弃方法清理**: 移除Auction级别的废弃方法
2. **消息类型冗余**: `YOU_WON` 等方法暂未使用，建议实现或移除
3. **缺少流拍通知**: 流拍时可以单独给关注用户发送通知
4. **消息版本控制**: 建议添加消息版本号，便于后续兼容性处理

---

## 📊 统计总结

- **消息类型总数**: 14种
- **房间广播消息**: 7种
- **用户定向消息**: 7种  
- **高频消息**: 4种 (出价相关)
- **已实现方法**: 20个
- **废弃方法**: 7个
- **待实现消息类型**: 5个

---

**生成时间**: 2026-06-10
**统计版本**: v1.0
**统计范围**: 完整项目代码库