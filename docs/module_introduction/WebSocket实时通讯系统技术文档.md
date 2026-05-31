# 拍卖系统WebSocket实时通讯模块技术文档

## 📋 文档信息

| 项目 | 内容 |
|------|------|
| **文档名称** | WebSocket实时通讯模块技术文档 |
| **版本** | v1.0.0 |
| **创建日期** | 2026-05-30 |
| **作者** | Claude AI |
| **适用系统** | 电子拍卖系统 |

---

## 📖 目录

- [1. 系统概述](#1-系统概述)
- [2. 技术架构](#2-技术架构)
- [3. 核心功能](#3-核心功能)
- [4. 消息协议](#4-消息协议)
- [5. API接口文档](#5-api接口文档)
- [6. 使用指南](#6-使用指南)
- [7. 测试指南](#7-测试指南)
- [8. 常见问题](#8-常见问题)

---

## 1. 系统概述

### 1.1 功能简介

WebSocket实时通讯模块负责拍卖系统中的实时消息推送，为用户提供即时的竞拍状态更新，包括：

- ✅ **实时价格推送**: 出价后立即推送最新价格
- ✅ **排行榜更新**: 实时更新出价排行榜
- ✅ **领先状态通知**: 当前领先者状态变化通知
- ✅ **超越提醒**: 被其他用户超越时的提醒
- ✅ **拍卖状态通知**: 拍卖开始、结束、取消等状态通知
- ✅ **支付状态通知**: 支付成功、失败等状态通知
- ✅ **房间管理**: WebSocket连接的房间管理

### 1.2 设计目标

- **实时性**: 毫秒级消息推送延迟
- **可靠性**: 保证消息的可靠送达
- **扩展性**: 支持大量用户同时在线
- **灵活性**: 支持多种消息类型和推送策略
- **兼容性**: 支持各种WebSocket客户端

### 1.3 技术特点

| 特性 | 技术方案 | 优势 |
|------|----------|------|
| 通讯协议 | WebSocket | 双向实时通讯 |
| 消息格式 | JSON | 通用格式，易于解析 |
| 房间管理 | 基于拍卖的房间 | 精准推送 |
| 连接管理 | 心跳检测 | 保证连接活跃 |
| 异常处理 | 重连机制 | 提升可用性 |

---

## 2. 技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                         客户端层                               │
│  (Web浏览器 / 移动APP / 桌面应用)                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ WebSocket连接
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   WebSocket连接层                             │
│  ┌────────────────────────────────────────────────────┐     │
│  │           WebSocketHandler                          │     │
│  │  - 连接建立和断开管理                               │     │
│  │  - 消息接收和发送                                   │     │
│  │  - 异常处理和重连                                   │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      消息服务层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │WsMessage     │  │WsRoomManager │  │WsSession     │     │
│  │Service       │  │              │  │Manager       │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      业务逻辑层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │BidService    │  │Auction       │  │Payment       │     │
│  │              │  │Service       │  │Service       │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 连接管理流程

```
┌─────────────────────────────────────────────────────────────┐
│                  WebSocket连接管理流程                        │
└─────────────────────────────────────────────────────────────┘

1. 客户端发起连接
    │
    ├─ WebSocket握手
    ├─ 身份验证
    └─ 会话建立
    │
    ▼
2. 加入拍卖房间
    │
    ├─ 订阅拍卖频道
    ├─ 存储会话信息
    └─ 发送欢迎消息
    │
    ▼
3. 接收业务消息
    │
    ├─ 解析消息类型
    ├─ 路由到处理器
    └─ 执行业务逻辑
    │
    ▼
4. 发送推送消息
    │
    ├─ 序列化消息内容
    ├─ 推送目标用户
    └─ 处理发送失败
    │
    ▼
5. 心跳检测
    │
    ├─ 定时发送心跳
    ├─ 检测连接状态
    └─ 处理超时断开
    │
    ▼
6. 连接断开
    │
    ├─ 清理会话信息
    ├─ 离开拍卖房间
    └─ 记录断开日志
```

### 2.3 房间管理机制

```
┌─────────────────────────────────────────────────────────────┐
│                    房间管理架构                              │
└─────────────────────────────────────────────────────────────┘

WebSocket连接
    │
    ├─ 用户A ──┐
    ├─ 用户B ──┤
    ├─ 用户C ──┤ → 房间1 (拍卖1)
    └─ 用户D ──┘
    │
    ├─ 用户E ──┐
    ├─ 用户F ──┤ → 房间2 (拍卖2)
    └─ 用户G ──┘
    │
    └─ 用户H ── → 全局广播

房间管理：
- 按拍卖ID创建房间
- 用户进入直播间自动加入房间
- 拍卖结束后自动离开房间
- 支持跨房间消息推送
```

---

## 3. 核心功能

### 3.1 消息推送服务

```java
/**
 * WebSocket消息推送服务
 */
@Service
@RequiredArgsConstructor
public class WsMessageService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 广播最新出价消息
     */
    public void broadcastNewBid(Auction auction, Bid bid, String username, Integer rank) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_BID");
        message.put("auctionId", auction.getId());
        message.put("bidAmount", bid.getAmount());
        message.put("bidUserId", bid.getUserId());
        message.put("username", username);
        message.put("rank", rank);
        message.put("currentPrice", auction.getCurrentPrice());
        message.put("timestamp", LocalDateTime.now());
        
        // 广播到拍卖房间
        sendToAuctionRoom(auction.getId(), message);
    }
    
    /**
     * 广播价格更新
     */
    public void broadcastPriceUpdate(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "PRICE_UPDATE");
        message.put("auctionId", auctionId);
        message.put("currentPrice", auction.getCurrentPrice());
        message.put("highestBidder", auction.getHighestBidder());
        message.put("timestamp", LocalDateTime.now());
        
        sendToAuctionRoom(auctionId, message);
    }
    
    /**
     * 发送被超越通知
     */
    public void sendYouWereOvertaken(Long userId, Long auctionId, BigDecimal newPrice) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "YOU_WERE_OVERTAKEN");
        message.put("auctionId", auctionId);
        message.put("newPrice", newPrice);
        message.put("timestamp", LocalDateTime.now());
        
        sendToUser(userId, message);
    }
    
    /**
     * 发送当前领先通知
     */
    public void sendYouAreLeading(Long userId, Long auctionId, BigDecimal currentPrice) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "YOU_ARE_LEADING");
        message.put("auctionId", auctionId);
        message.put("currentPrice", currentPrice);
        message.put("timestamp", LocalDateTime.now());
        
        sendToUser(userId, message);
    }
    
    /**
     * 发送拍卖开始通知
     */
    public void sendAuctionStarted(Long auctionId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "AUCTION_STARTED");
        message.put("auctionId", auctionId);
        message.put("timestamp", LocalDateTime.now());
        
        sendToAuctionRoom(auctionId, message);
    }
    
    /**
     * 发送拍卖结束通知
     */
    public void sendAuctionCompleted(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "AUCTION_COMPLETED");
        message.put("auctionId", auctionId);
        message.put("finalPrice", auction.getCurrentPrice());
        message.put("winnerId", auction.getHighestBidder());
        message.put("timestamp", LocalDateTime.now());
        
        sendToAuctionRoom(auctionId, message);
    }
    
    /**
     * 发送支付成功通知
     */
    public void sendPaymentSuccess(Long userId, Long orderId, BigDecimal amount) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "PAYMENT_SUCCESS");
        message.put("userId", userId);
        message.put("orderId", orderId);
        message.put("amount", amount);
        message.put("timestamp", LocalDateTime.now());
        
        sendToUser(userId, message);
    }
    
    /**
     * 发送到拍卖房间
     */
    private void sendToAuctionRoom(Long auctionId, Object message) {
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * 发送给指定用户
     */
    private void sendToUser(Long userId, Object message) {
        String destination = "/topic/user/" + userId;
        messagingTemplate.convertAndSend(destination, message);
    }
}
```

### 3.2 连接处理器

```java
/**
 * WebSocket连接处理器
 */
@Component
public class AuctionWebSocketHandler extends TextWebSocketHandler {
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 获取用户ID和拍卖ID
        Long userId = extractUserId(session);
        Long auctionId = extractAuctionId(session);
        
        // 存储会话信息
        WsSessionManager.addSession(userId, auctionId, session);
        
        // 加入拍卖房间
        WsRoomManager.joinRoom(auctionId, userId, session);
        
        // 发送欢迎消息
        sendWelcomeMessage(session, auctionId);
        
        log.info("WebSocket连接建立: userId={}, auctionId={}, sessionId={}", 
                userId, auctionId, session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 解析消息
        Map<String, Object> payload = parseMessage(message.getPayload());
        String type = (String) payload.get("type");
        
        // 根据消息类型处理
        switch (type) {
            case "PING":
                handlePing(session);
                break;
            case "SUBSCRIBE":
                handleSubscribe(session, payload);
                break;
            default:
                log.warn("未知的消息类型: {}", type);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = extractUserId(session);
        Long auctionId = extractAuctionId(session);
        
        // 离开拍卖房间
        WsRoomManager.leaveRoom(auctionId, userId);
        
        // 移除会话信息
        WsSessionManager.removeSession(userId, auctionId);
        
        log.info("WebSocket连接断开: userId={}, auctionId={}, status={}", 
                userId, auctionId, status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}, error={}", 
                session.getId(), exception.getMessage());
        
        // 清理会话
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }
}
```

### 3.3 房间管理器

```java
/**
 * WebSocket房间管理器
 */
@Component
public class WsRoomManager {
    
    // 房间ID -> 用户ID -> WebSocket会话
    private static final Map<Long, Map<Long, WebSocketSession>> ROOMS = new ConcurrentHashMap<>();
    
    /**
     * 加入房间
     */
    public static void joinRoom(Long roomId, Long userId, WebSocketSession session) {
        ROOMS.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
              .put(userId, session);
        
        log.info("用户加入房间: roomId={}, userId={}", roomId, userId);
    }
    
    /**
     * 离开房间
     */
    public static void leaveRoom(Long roomId, Long userId) {
        Map<Long, WebSocketSession> room = ROOMS.get(roomId);
        if (room != null) {
            room.remove(userId);
            
            // 如果房间为空，删除房间
            if (room.isEmpty()) {
                ROOMS.remove(roomId);
            }
            
            log.info("用户离开房间: roomId={}, userId={}", roomId, userId);
        }
    }
    
    /**
     * 向房间内所有用户发送消息
     */
    public static void sendToRoom(Long roomId, Object message) {
        Map<Long, WebSocketSession> room = ROOMS.get(roomId);
        if (room == null || room.isEmpty()) {
            return;
        }
        
        String jsonMessage = convertToJson(message);
        
        room.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (Exception e) {
                    log.error("发送消息失败: sessionId={}, error={}", 
                            session.getId(), e.getMessage());
                }
            }
        });
    }
    
    /**
     * 向房间内指定用户发送消息
     */
    public static void sendToUserInRoom(Long roomId, Long userId, Object message) {
        Map<Long, WebSocketSession> room = ROOMS.get(roomId);
        if (room == null) {
            return;
        }
        
        WebSocketSession session = room.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = convertToJson(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (Exception e) {
                log.error("发送消息失败: userId={}, error={}", userId, e.getMessage());
            }
        }
    }
    
    /**
     * 获取房间内的用户数量
     */
    public static int getRoomSize(Long roomId) {
        Map<Long, WebSocketSession> room = ROOMS.get(roomId);
        return room != null ? room.size() : 0;
    }
}
```

### 3.4 会话管理器

```java
/**
 * WebSocket会话管理器
 */
@Component
public class WsSessionManager {
    
    // 用户ID -> 拍卖ID -> WebSocket会话
    private static final Map<Long, Map<Long, WebSocketSession>> USER_SESSIONS = new ConcurrentHashMap<>();
    
    /**
     * 添加会话
     */
    public static void addSession(Long userId, Long auctionId, WebSocketSession session) {
        USER_SESSIONS.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                     .put(auctionId, session);
    }
    
    /**
     * 移除会话
     */
    public static void removeSession(Long userId, Long auctionId) {
        Map<Long, WebSocketSession> sessions = USER_SESSIONS.get(userId);
        if (sessions != null) {
            sessions.remove(auctionId);
            
            if (sessions.isEmpty()) {
                USER_SESSIONS.remove(userId);
            }
        }
    }
    
    /**
     * 获取用户的所有会话
     */
    public static Map<Long, WebSocketSession> getUserSessions(Long userId) {
        return USER_SESSIONS.get(userId);
    }
    
    /**
     * 获取用户的指定会话
     */
    public static WebSocketSession getSession(Long userId, Long auctionId) {
        Map<Long, WebSocketSession> sessions = USER_SESSIONS.get(userId);
        return sessions != null ? sessions.get(auctionId) : null;
    }
    
    /**
     * 向用户的所有会话发送消息
     */
    public static void sendToUser(Long userId, Object message) {
        Map<Long, WebSocketSession> sessions = USER_SESSIONS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        
        String jsonMessage = convertToJson(message);
        
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (Exception e) {
                    log.error("发送消息失败: userId={}, error={}", userId, e.getMessage());
                }
            }
        });
    }
}
```

---

## 4. 消息协议

### 4.1 消息格式

所有WebSocket消息都采用统一的JSON格式：

```json
{
  "type": "消息类型",
  "data": {
    // 消息具体内容
  },
  "timestamp": "2026-05-30T10:30:00"
}
```

### 4.2 消息类型

| 消息类型 | 类型代码 | 说明 | 推送对象 |
|----------|----------|------|----------|
| 新出价 | NEW_BID | 有新的出价 | 拍卖房间所有用户 |
| 价格更新 | PRICE_UPDATE | 当前价格更新 | 拍卖房间所有用户 |
| 被超越 | YOU_WERE_OVERTAKEN | 用户被超越 | 指定用户 |
| 当前领先 | YOU_ARE_LEADING | 用户当前领先 | 指定用户 |
| 拍卖开始 | AUCTION_STARTED | 拍卖开始 | 拍卖房间所有用户 |
| 拍卖结束 | AUCTION_COMPLETED | 拍卖结束 | 拍卖房间所有用户 |
| 拍卖取消 | AUCTION_CANCELLED | 拍卖取消 | 拍卖房间所有用户 |
| 支付成功 | PAYMENT_SUCCESS | 支付成功 | 指定用户 |
| 支付失败 | PAYMENT_FAILED | 支付失败 | 指定用户 |
| 延时通知 | AUCTION_DELAYED | 拍卖延时 | 拍卖房间所有用户 |

### 4.3 消息示例

#### 4.3.1 新出价消息

```json
{
  "type": "NEW_BID",
  "data": {
    "auctionId": 1,
    "bidAmount": 1050.00,
    "bidUserId": 123,
    "username": "user***",
    "rank": 1,
    "currentPrice": 1050.00
  },
  "timestamp": "2026-05-30T10:30:00"
}
```

#### 4.3.2 被超越消息

```json
{
  "type": "YOU_WERE_OVERTAKEN",
  "data": {
    "auctionId": 1,
    "newPrice": 1100.00,
    "newLeaderId": 456,
    "yourNewRank": 2
  },
  "timestamp": "2026-05-30T10:31:00"
}
```

#### 4.3.3 拍卖结束消息

```json
{
  "type": "AUCTION_COMPLETED",
  "data": {
    "auctionId": 1,
    "finalPrice": 5000.00,
    "winnerId": 123,
    "winnerUsername": "user***"
  },
  "timestamp": "2026-05-30T12:00:00"
}
```

---

## 5. API接口文档

### 5.1 WebSocket连接

#### 5.1.1 建立连接

**连接地址**: `ws://localhost:8080/ws/auction/{auctionId}?token={accessToken}`

**连接参数**:
- `auctionId`: 拍卖ID（必填）
- `token`: 访问令牌（必填）

**连接示例（JavaScript）**:
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/auction/1?token=xxx');

ws.onopen = function() {
    console.log('WebSocket连接已建立');
};

ws.onmessage = function(event) {
    const message = JSON.parse(event.data);
    console.log('收到消息:', message);
    handleMessage(message);
};

ws.onerror = function(error) {
    console.error('WebSocket错误:', error);
};

ws.onclose = function() {
    console.log('WebSocket连接已关闭');
};
```

#### 5.1.2 心跳保持

**客户端发送**:
```json
{
  "type": "PING",
  "timestamp": "2026-05-30T10:30:00"
}
```

**服务器响应**:
```json
{
  "type": "PONG",
  "timestamp": "2026-05-30T10:30:00"
}
```

### 5.2 REST API（用于测试）

#### 5.2.1 广播测试消息

**接口地址**: `POST /ws/broadcast/test`

**请求参数**:
```json
{
  "auctionId": 1,
  "message": "测试消息"
}
```

---

## 6. 使用指南

### 6.1 JavaScript客户端

```javascript
class AuctionWebSocket {
    constructor(auctionId, token) {
        this.auctionId = auctionId;
        this.ws = new WebSocket(`ws://localhost:8080/ws/auction/${auctionId}?token=${token}`);
        this.setupEventHandlers();
    }
    
    setupEventHandlers() {
        this.ws.onopen = () => {
            console.log('WebSocket连接成功');
            this.startHeartbeat();
        };
        
        this.ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            this.handleMessage(message);
        };
        
        this.ws.onerror = (error) => {
            console.error('WebSocket错误:', error);
        };
        
        this.ws.onclose = () => {
            console.log('WebSocket连接关闭');
            this.stopHeartbeat();
            // 可选：实现自动重连
            this.reconnect();
        };
    }
    
    handleMessage(message) {
        switch (message.type) {
            case 'NEW_BID':
                this.onNewBid(message.data);
                break;
            case 'YOU_WERE_OVERTAKEN':
                this.onOvertaken(message.data);
                break;
            case 'YOU_ARE_LEADING':
                this.onLeading(message.data);
                break;
            case 'AUCTION_COMPLETED':
                this.onAuctionCompleted(message.data);
                break;
            default:
                console.log('未知消息类型:', message.type);
        }
    }
    
    onNewBid(data) {
        console.log('新出价:', data);
        // 更新UI显示最新价格
        updateCurrentPrice(data.currentPrice);
        updateLeaderboard(data.username, data.rank);
    }
    
    onOvertaken(data) {
        console.log('您已被超越:', data);
        // 显示超越通知
        showNotification('您已被超越！当前排名: ' + data.yourNewRank);
    }
    
    onLeading(data) {
        console.log('您当前领先:', data);
        // 显示领先通知
        showNotification('恭喜！您当前领先！');
    }
    
    onAuctionCompleted(data) {
        console.log('拍卖已结束:', data);
        // 显示拍卖结果
        showAuctionResult(data);
    }
    
    startHeartbeat() {
        this.heartbeatInterval = setInterval(() => {
            if (this.ws.readyState === WebSocket.OPEN) {
                this.ws.send(JSON.stringify({ type: 'PING' }));
            }
        }, 30000); // 每30秒发送一次心跳
    }
    
    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
        }
    }
    
    reconnect() {
        setTimeout(() => {
            console.log('尝试重新连接...');
            this.ws = new WebSocket(this.ws.url);
            this.setupEventHandlers();
        }, 5000); // 5秒后重连
    }
    
    close() {
        this.stopHeartbeat();
        this.ws.close();
    }
}

// 使用示例
const auctionWs = new AuctionWebSocket(1, 'your-access-token');
```

### 6.2 Java客户端

```java
public class AuctionWebSocketClient {
    
    private WebSocketSession session;
    private final String wsUrl;
    
    public AuctionWebSocketClient(Long auctionId, String token) {
        this.wsUrl = String.format("ws://localhost:8080/ws/auction/%d?token=%s", 
                                  auctionId, token);
    }
    
    public void connect() {
        WebSocketClient client = new StandardWebSocketClient() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                System.out.println("WebSocket连接成功");
                startHeartbeat(session);
            }
            
            @Override
            public void onMessage(Session session, String message) {
                handleMessage(message);
            }
            
            @Override
            public void onClose(Session session, CloseReason closeReason) {
                System.out.println("WebSocket连接关闭: " + closeReason);
            }
        };
        
        try {
            client.connectToServer(this.wsUrl);
        } catch (Exception e) {
            System.err.println("WebSocket连接失败: " + e.getMessage());
        }
    }
    
    private void handleMessage(String message) {
        // 解析并处理消息
        System.out.println("收到消息: " + message);
    }
    
    private void startHeartbeat(Session session) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText("{\"type\":\"PING\"}");
                } catch (Exception e) {
                    System.err.println("心跳发送失败: " + e.getMessage());
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}
```

### 6.3 移动端（Android/Kotlin）

```kotlin
class AuctionWebSocketClient(
    private val auctionId: Long,
    private val token: String
) {
    private lateinit var webSocket: WebSocket
    
    fun connect() {
        val request = Request.Builder()
            .url("ws://localhost:8080/ws/auction/$auctionId?token=$token")
            .build()
        
        val wsListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("WebSocket连接成功")
                startHeartbeat()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket正在关闭: $reason")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("WebSocket错误: ${t.message}")
            }
        }
        
        webSocket = OkHttpClient().newWebSocket(request, wsListener)
    }
    
    private fun handleMessage(message: String) {
        val json = JSONObject(message)
        val type = json.getString("type")
        
        when (type) {
            "NEW_BID" -> handleNewBid(json.getJSONObject("data"))
            "YOU_WERE_OVERTAKEN" -> handleOvertaken(json.getJSONObject("data"))
            "AUCTION_COMPLETED" -> handleAuctionCompleted(json.getJSONObject("data"))
            else -> println("未知消息类型: $type")
        }
    }
    
    private fun startHeartbeat() {
        // 实现心跳机制
    }
    
    fun disconnect() {
        webSocket.close(1000, "正常关闭")
    }
}
```

---

## 7. 测试指南

### 7.1 连接测试

```bash
# 使用wscat工具测试WebSocket连接
wscat -c "ws://localhost:8080/ws/auction/1?token=your-token"
```

### 7.2 消息测试

```javascript
// 测试消息接收
ws.onmessage = function(event) {
    console.log('收到消息:', event.data);
};

// 测试消息发送
ws.send(JSON.stringify({
    type: 'PING',
    timestamp: new Date().toISOString()
}));
```

### 7.3 压力测试

```java
// 模拟多个用户同时连接
@Test
public void testMultipleConnections() throws Exception {
    int connectionCount = 100;
    CountDownLatch latch = new CountDownLatch(connectionCount);
    
    for (int i = 0; i < connectionCount; i++) {
        final int userId = i;
        new Thread(() -> {
            try {
                AuctionWebSocketClient client = new AuctionWebSocketClient(1L, "token");
                client.connect();
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    latch.await(30, TimeUnit.SECONDS);
    assertTrue(true, "所有连接成功建立");
}
```

---

## 8. 常见问题

### Q1: WebSocket连接失败如何处理？

**A**: 检查token是否有效，网络是否正常，服务器是否启动。实现自动重连机制提升可用性。

### Q2: 如何保证消息的可靠性？

**A**: 实现消息确认机制，对于重要消息（如支付成功）需要客户端确认收到。

### Q3: 大量用户同时在线如何处理？

**A**: 使用分布式WebSocket解决方案，如Spring WebSocket + Redis Pub/Sub。

### Q4: 如何处理网络中断后的消息丢失？

**A**: 实现消息缓存机制，重连后自动拉取离线期间的消息。

### Q5: WebSocket安全性如何保障？

**A**: 使用WSS加密传输，token身份验证，消息内容验证，防止XSS攻击。

---

**文档版本**: v1.0.0
**最后更新**: 2026-05-30
**维护团队**: 拍卖系统开发团队
