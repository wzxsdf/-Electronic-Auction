# 直播竞拍全栈系统 - Plan 3: 出价服务 + 实时通信

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现出价功能和 WebSocket 实时通信，包括分布式锁保证幂等性、出价校验、实时排行榜、房间隔离、心跳保活。

**Architecture:** 出价服务通过 Redis 分布式锁保证并发安全，WebSocket 长连接实现房间级消息推送，心跳机制维持连接活性。

**Tech Stack:** Spring WebSocket, Redis, Lettuce, Jackson

---

## 文件结构

```
auction-system/
├── src/main/java/com/auction/
│   ├── api/
│   │   ├── controller/
│   │   │   └── BidController.java
│   │   └── dto/
│   │       ├── request/
│   │       │   └── PlaceBidRequest.java
│   │       └── response/
│   │           └── BidResultResponse.java
│   ├── service/
│   │   ├── bid/
│   │   │   ├── BidService.java
│   │   │   ├── BidValidator.java
│   │   │   └── DistributedLock.java
│   │   └── websocket/
│   │       ├── AuctionWebSocketHandler.java
│   │       ├── WsRoomManager.java
│   │       ├── WsMessageService.java
│   │       └── WsSessionManager.java
│   └── domain/
│       └── enums/
│           └── MessageType.java
```

---

## Task 1: 添加 WebSocket 依赖和配置

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/auction/config/WebSocketConfig.java`

- [ ] **Step 1: 在 pom.xml 中添加 WebSocket 依赖**

```xml
<!-- 在 <dependencies> 中添加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

- [ ] **Step 2: 创建 WebSocketConfig**

```java
package com.auction.config;

import com.auction.service.websocket.AuctionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AuctionWebSocketHandler auctionWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(auctionWebSocketHandler, "/ws/auction/{auctionId}")
            .setAllowedOrigins("*")
            .addInterceptors(new HandshakeInterceptor() {
                @Override
                public boolean beforeHandshake(
                        org.springframework.http.server.ServerHttpRequest request,
                        org.springframework.http.server.ServerServerHttpResponse response,
                        org.springframework.web.socket.WebSocketHandler wsHandler,
                        Map<String, Object> attributes) throws Exception {

                    // 从 URL 获取 auctionId
                    String path = request.getURI().getPath();
                    String[] parts = path.split("/");
                    if (parts.length >= 4) {
                        try {
                            Long auctionId = Long.parseLong(parts[3]);
                            attributes.put("auctionId", auctionId);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public void afterHandshake(
                        org.springframework.http.server.ServerHttpRequest request,
                        org.springframework.http.server.ServerServerHttpResponse response,
                        org.springframework.web.socket.WebSocketHandler wsHandler,
                        Exception exception) {
                }
            });
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add pom.xml src/main/java/com/auction/config/WebSocketConfig.java
git commit -m "feat: 添加 WebSocket 配置"
```

---

## Task 2: 创建消息类型枚举

**Files:**
- Create: `src/main/java/com/auction/domain/enums/MessageType.java`

- [ ] **Step 1: 创建 MessageType 枚举**

```java
package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum MessageType {
    // 连接相关
    CONNECT("连接成功"),
    DISCONNECT("断开连接"),
    PING("心跳请求"),
    PONG("心跳响应"),

    // 竞拍状态
    AUCTION_START("竞拍开始"),
    AUCTION_END("竞拍结束"),
    AUCTION_EXTENDED("竞拍延时"),
    AUCTION_PAUSED("竞拍暂停"),
    AUCTION_CANCELLED("竞拍取消"),

    // 出价相关
    NEW_BID("新出价"),
    BID_VALIDATED("出价验证成功"),
    BID_REJECTED("出价被拒绝"),

    // 状态同步
    PRICE_UPDATE("价格更新"),
    LEADERBOARD_UPDATE("排行榜更新"),
    TIME_UPDATE("时间更新"),
    BID_COUNT_UPDATE("出价次数更新"),

    // 个人通知
    YOU_ARE_LEADING("你领先了"),
    YOU_WERE_OVERTAKEN("你被超越了"),
    YOU_WON("你赢了"),
    YOU_LOST("很遗憾，你未成交"),

    // 错误
    ERROR("错误消息");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加 MessageType 枚举"
```

---

## Task 3: 创建 WebSocket 消息模型

**Files:**
- Create: `src/main/java/com/auction/service/websocket/model/WsMessage.java`
- Create: `src/main/java/com/auction/service/websocket/model/BidMessage.java`

- [ ] **Step 1: 创建 WsMessage 类**

```java
package com.auction.service.websocket.model;

import com.auction.domain.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {
    private MessageType type;
    private Object data;
    private long timestamp;
    private Long auctionId;

    public static WsMessage of(MessageType type, Object data) {
        return WsMessage.builder()
            .type(type)
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static WsMessage of(MessageType type, Object data, Long auctionId) {
        return WsMessage.builder()
            .type(type)
            .data(data)
            .auctionId(auctionId)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public String toJson() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }
}
```

- [ ] **Step 2: 创建 BidMessage 类（出价消息数据）**

```java
package com.auction.service.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidMessage {
    private Long bidId;
    private Long auctionId;
    private Long userId;
    private String username;  // 脱敏后的用户名
    private BigDecimal amount;
    private Integer rank;
    private LocalDateTime bidTime;
    private Boolean isAutoBid;
}
```

- [ ] **Step 3: 创建 PriceUpdateMessage 类**

```java
package com.auction.service.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdateMessage {
    private BigDecimal currentPrice;
    private Long highestBidder;
    private String highestBidderName;
    private Integer bidCount;
    private Long remainingMs;
}
```

- [ ] **Step 4: 创建 AuctionExtendedMessage 类**

```java
package com.auction.service.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionExtendedMessage {
    private Long auctionId;
    private LocalDateTime oldEndTime;
    private LocalDateTime newEndTime;
    private Integer extendedSeconds;
    private String reason;
}
```

- [ ] **Step 5: 提交**

```bash
git add .
git commit -m "feat: 添加 WebSocket 消息模型"
```

---

## Task 4: 创建分布式锁服务

**Files:**
- Create: `src/main/java/com/auction/service/bid/DistributedLockService.java`

- [ ] **Step 1: 创建 DistributedLockService**

```java
package com.auction.service.bid;

import com.auction.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private static final String LOCK_PREFIX = "lock:auction:";
    private static final long LOCK_EXPIRE_TIME = 10; // 秒

    private final RedisService redisService;

    /**
     * 尝试获取锁
     */
    public boolean tryLock(String key, String requestId) {
        String lockKey = LOCK_PREFIX + key;
        Boolean acquired = redisService.get(
            lockKey + ":" + requestId
        ) == null;

        if (acquired) {
            redisService.set(lockKey + ":" + requestId, "1", LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    /**
     * 使用 Redis SET NX EX 实现
     */
    public boolean tryLockV2(String key, String requestId) {
        String lockKey = LOCK_PREFIX + key;
        // 使用 SETNX
        Long result = redisService.increment(lockKey + ":counter");
        if (result == 1) {
            redisService.set(lockKey + ":owner", requestId, LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    /**
     * 释放锁
     */
    public boolean unlock(String key, String requestId) {
        String lockKey = LOCK_PREFIX + key;
        String ownerKey = lockKey + ":owner";
        String owner = (String) redisService.get(ownerKey);

        if (requestId.equals(owner)) {
            redisService.delete(lockKey + ":owner");
            redisService.delete(lockKey + ":counter");
            return true;
        }
        return false;
    }

    /**
     * 带锁执行（模板方法）
     */
    public <T> T executeWithLock(String auctionId, LockCallback<T> callback) {
        String lockKey = "bid:" + auctionId;
        String requestId = UUID.randomUUID().toString();

        try {
            // 尝试获取锁（重试3次）
            boolean locked = false;
            for (int i = 0; i < 3; i++) {
                if (tryLockV2(lockKey, requestId)) {
                    locked = true;
                    break;
                }
                Thread.sleep(50);
            }

            if (!locked) {
                throw new com.auction.common.BizException(
                    com.auction.common.ErrorCode.BID_FREQUENCY_HIGH,
                    "系统繁忙，请稍后再试"
                );
            }

            // 执行业务逻辑
            return callback.execute();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new com.auction.common.BizException(
                com.auction.common.ErrorCode.INTERNAL_ERROR,
                "出价处理被中断"
            );
        } finally {
            unlock(lockKey, requestId);
        }
    }

    @FunctionalInterface
    public interface LockCallback<T> {
        T execute();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加分布式锁服务"
```

---

## Task 5: 创建出价校验器

**Files:**
- Create: `src/main/java/com/auction/service/bid/BidValidator.java`

- [ ] **Step 1: 创建 BidValidator**

```java
package com.auction.service.bid;

import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Auction;
import com.auction.domain.enums.AuctionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidValidator {

    private final com.auction.infrastructure.redis.RedisService redisService;

    /**
     * 校验出价请求
     */
    public void validate(Auction auction, Long userId, BigDecimal amount) {
        // 1. 检查竞拍状态
        AuctionStatus status = auction.getStatusEnum();
        if (status != AuctionStatus.ACTIVE) {
            if (status == AuctionStatus.PENDING) {
                throw new BizException(ErrorCode.AUCTION_NOT_STARTED);
            } else if (status == AuctionStatus.COMPLETED) {
                throw new BizException(ErrorCode.AUCTION_ALREADY_ENDED);
            } else if (status == AuctionStatus.CANCELLED) {
                throw new BizException(ErrorCode.AUCTION_CANCELLED);
            } else if (status == AuctionStatus.PAUSED) {
                throw new BizException(ErrorCode.BAD_REQUEST, "竞拍已暂停");
            }
        }

        // 2. 检查出价金额
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BID_AMOUNT_INVALID, "出价金额必须大于0");
        }

        // 3. 获取当前价格
        BigDecimal currentPrice = getCurrentPrice(auction);
        if (amount.compareTo(currentPrice) <= 0) {
            throw new BizException(ErrorCode.BID_AMOUNT_TOO_LOW,
                String.format("出价必须高于当前价格 %s", currentPrice));
        }

        // 4. 检查加价幅度
        BigDecimal minIncrement = auction.getBidIncrement();
        BigDecimal minValidPrice = currentPrice.add(minIncrement);
        if (amount.compareTo(minValidPrice) < 0) {
            throw new BizException(ErrorCode.BID_AMOUNT_INVALID,
                String.format("出价必须按 %s 的幅度递增，最低有效出价为 %s",
                    minIncrement, minValidPrice));
        }

        // 5. 检查封顶价
        if (auction.getMaxPrice() != null && amount.compareTo(auction.getMaxPrice()) > 0) {
            throw new BizException(ErrorCode.BID_EXCEED_MAX_PRICE,
                String.format("出价不能超过封顶价 %s", auction.getMaxPrice()));
        }

        // 6. 检查是否是当前最高出价者
        Long highestBidder = getHighestBidder(auction);
        if (userId.equals(highestBidder)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "您当前是最高出价者，无需再次出价");
        }

        // 7. 检查出价频率（限流）
        checkBidFrequency(userId, auction.getId());
    }

    /**
     * 获取当前价格（优先从 Redis）
     */
    private BigDecimal getCurrentPrice(Auction auction) {
        String priceKey = "auction:" + auction.getId() + ":current_price";
        Object priceObj = redisService.get(priceKey);
        if (priceObj != null) {
            return new BigDecimal(priceObj.toString());
        }
        return auction.getCurrentPrice();
    }

    /**
     * 获取当前最高出价者
     */
    private Long getHighestBidder(Auction auction) {
        String bidderKey = "auction:" + auction.getId() + ":highest_bidder";
        Object bidderObj = redisService.get(bidderKey);
        if (bidderObj != null) {
            return Long.parseLong(bidderObj.toString());
        }
        return auction.getHighestBidder();
    }

    /**
     * 检查出价频率
     */
    private void checkBidFrequency(Long userId, Long auctionId) {
        String freqKey = "bid:count:" + userId + ":1min";
        Object countObj = redisService.get(freqKey);

        int count = countObj != null ? Integer.parseInt(countObj.toString()) : 0;

        if (count >= 20) { // 每分钟最多20次
            throw new BizException(ErrorCode.BID_FREQUENCY_HIGH);
        }

        // 递增计数
        redisService.increment(freqKey);
        redisService.expire(freqKey, 60, java.util.concurrent.TimeUnit.SECONDS);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加出价校验器"
```

---

## Task 6: 创建出价 DTO

**Files:**
- Create: `src/main/java/com/auction/api/dto/request/PlaceBidRequest.java`
- Create: `src/main/java/com/auction/api/dto/response/BidResultResponse.java`

- [ ] **Step 1: 创建 PlaceBidRequest**

```java
package com.auction.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlaceBidRequest {

    @NotNull(message = "竞拍ID不能为空")
    private Long auctionId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "出价金额不能为空")
    @Positive(message = "出价金额必须大于0")
    private BigDecimal amount;

    private Boolean isAutoBid = false;  // 是否是自动出价
}
```

- [ ] **Step 2: 创建 BidResultResponse**

```java
package com.auction.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class BidResultResponse {
    private Long bidId;
    private BigDecimal currentPrice;
    private Integer yourRank;
    private Boolean isLeading;
    private Long remainingMs;
    private Boolean wasExtended;  // 是否触发了延时
    private Integer newEndTime;   // 新的结束时间（秒）
    private String message;
}
```

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "feat: 添加出价 DTO"
```

---

## Task 7: 创建 WebSocket 房间管理器

**Files:**
- Create: `src/main/java/com/auction/service/websocket/WsRoomManager.java`

- [ ] **Step 1: 创建 WsRoomManager**

```java
package com.auction.service.websocket;

import com.auction.service.websocket.model.WsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

@Slf4j
@Component
public class WsRoomManager {

    // 房间 -> Session 集合
    private final ConcurrentHashMap<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // Session ID -> 房间映射
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();

    // Session ID -> User ID 映射
    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * 加入房间
     */
    public void joinRoom(String roomId, WebSocketSession session, Long userId) {
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionRoomMap.put(session.getId(), roomId);
        sessionUserMap.put(session.getId(), userId);

        log.info("用户加入房间: roomId={}, userId={}, sessionId={}",
            roomId, userId, session.getId());

        // 发送加入成功消息
        sendToSession(session, WsMessage.of(
            com.auction.domain.enums.MessageType.CONNECT,
            roomId
        ));
    }

    /**
     * 离开房间
     */
    public void leaveRoom(WebSocketSession session) {
        String roomId = sessionRoomMap.remove(session.getId());
        if (roomId != null) {
            Set<WebSocketSession> roomSessions = rooms.get(roomId);
            if (roomSessions != null) {
                roomSessions.remove(session);
                if (roomSessions.isEmpty()) {
                    rooms.remove(roomId);
                }
            }
        }
        sessionUserMap.remove(session.getId());

        log.info("用户离开房间: roomId={}, sessionId={}", roomId, session.getId());
    }

    /**
     * 广播消息到房间内所有人
     */
    public void broadcastToRoom(String roomId, WsMessage message) {
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions == null) {
            return;
        }

        String json = message.toJson();
        TextMessage textMessage = new TextMessage(json);

        roomSessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("发送消息失败: sessionId={}", session.getId(), e);
                }
            }
        });

        log.debug("广播消息到房间: roomId={}, type={}, onlineCount={}",
            roomId, message.getType(), roomSessions.size());
    }

    /**
     * 广播消息到房间内所有人（除了发送者）
     */
    public void broadcastToRoomExclude(String roomId, WsMessage message, String excludeSessionId) {
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions == null) {
            return;
        }

        String json = message.toJson();
        TextMessage textMessage = new TextMessage(json);

        roomSessions.forEach(session -> {
            if (!session.getId().equals(excludeSessionId) && session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("发送消息失败: sessionId={}", session.getId(), e);
                }
            }
        });
    }

    /**
     * 发送消息到指定用户
     */
    public void sendToUser(Long userId, WsMessage message) {
        sessionUserMap.entrySet().stream()
            .filter(e -> e.getValue().equals(userId))
            .forEach(e -> {
                WebSocketSession session = findSession(e.getKey());
                if (session != null && session.isOpen()) {
                    sendToSession(session, message);
                }
            });
    }

    /**
     * 发送消息到指定会话
     */
    public void sendToSession(WebSocketSession session, WsMessage message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message.toJson()));
            } catch (IOException e) {
                log.error("发送消息失败: sessionId={}", session.getId(), e);
            }
        }
    }

    /**
     * 获取房间在线人数
     */
    public int getRoomSize(String roomId) {
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        return roomSessions == null ? 0 : roomSessions.size();
    }

    /**
     * 获取房间内所有用户ID
     */
    public Set<Long> getRoomUsers(String roomId) {
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions == null) {
            return Set.of();
        }

        return roomSessions.stream()
            .map(s -> sessionUserMap.get(s.getId()))
            .filter(id -> id != null)
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 查找会话
     */
    private WebSocketSession findSession(String sessionId) {
        for (Set<WebSocketSession> sessions : rooms.values()) {
            for (WebSocketSession session : sessions) {
                if (session.getId().equals(sessionId)) {
                    return session;
                }
            }
        }
        return null;
    }

    /**
     * 获取用户所在的房间
     */
    public String getUserRoom(Long userId) {
        return sessionUserMap.entrySet().stream()
            .filter(e -> e.getValue().equals(userId))
            .map(e -> sessionRoomMap.get(e.getKey()))
            .findFirst()
            .orElse(null);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加 WebSocket 房间管理器"
```

---

## Task 8: 创建 WebSocket 处理器

**Files:**
- Create: `src/main/java/com/auction/service/websocket/AuctionWebSocketHandler.java`

- [ ] **Step 1: 创建 AuctionWebSocketHandler**

```java
package com.auction.service.websocket;

import com.auction.service.websocket.model.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionWebSocketHandler extends TextWebSocketHandler {

    private final WsRoomManager roomManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long auctionId = (Long) session.getAttributes().get("auctionId");
        Long userId = extractUserId(session);

        if (auctionId == null || userId == null) {
            session.close();
            return;
        }

        roomManager.joinRoom(auctionId.toString(), session, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            if ("PING".equals(type)) {
                // 心跳响应
                session.sendMessage(new TextMessage(
                    objectMapper.writeValueAsString(
                        Map.of("type", "PONG", "timestamp", System.currentTimeMillis())
                    )
                ));
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败: sessionId={}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        roomManager.leaveRoom(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);
        roomManager.leaveRoom(session);
    }

    private Long extractUserId(WebSocketSession session) {
        // 从查询参数获取 userId（简化版，实际应从 token 获取）
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            try {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                return Long.parseLong(userIdStr);
            } catch (Exception e) {
                log.error("解析 userId 失败: query={}", query, e);
            }
        }
        return null;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加 WebSocket 处理器"
```

---

## Task 9: 创建 WebSocket 消息服务

**Files:**
- Create: `src/main/java/com/auction/service/websocket/WsMessageService.java`

- [ ] **Step 1: 创建 WsMessageService**

```java
package com.auction.service.websocket;

import com.auction.domain.entity.Auction;
import com.auction.domain.entity.Bid;
import com.auction.domain.enums.MessageType;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.UserRepository;
import com.auction.service.websocket.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WsMessageService {

    private final WsRoomManager roomManager;
    private final RedisService redisService;

    /**
     * 广播新出价
     */
    public void broadcastNewBid(Auction auction, Bid bid, String username, Integer rank) {
        BidMessage bidMessage = BidMessage.builder()
            .bidId(bid.getId())
            .auctionId(auction.getId())
            .userId(bid.getUserId())
            .username(maskUsername(username))
            .amount(bid.getAmount())
            .rank(rank)
            .bidTime(bid.getCreatedAt())
            .isAutoBid(bid.getIsAutoBid())
            .build();

        roomManager.broadcastToRoom(
            auction.getId().toString(),
            WsMessage.of(MessageType.NEW_BID, bidMessage, auction.getId())
        );
    }

    /**
     * 广播价格更新
     */
    public void broadcastPriceUpdate(Long auctionId) {
        String auctionKey = "auction:" + auctionId;
        Object priceObj = redisService.get(auctionKey + ":current_price");
        Object bidderObj = redisService.get(auctionKey + ":highest_bidder");
        Object countObj = redisService.get(auctionKey + ":bid_count");

        PriceUpdateMessage message = PriceUpdateMessage.builder()
            .currentPrice(priceObj != null ? new BigDecimal(priceObj.toString()) : BigDecimal.ZERO)
            .highestBidder(bidderObj != null ? Long.parseLong(bidderObj.toString()) : null)
            .bidCount(countObj != null ? Integer.parseInt(countObj.toString()) : 0)
            .build();

        roomManager.broadcastToRoom(
            auctionId.toString(),
            WsMessage.of(MessageType.PRICE_UPDATE, message, auctionId)
        );
    }

    /**
     * 广播排行榜更新
     */
    public void broadcastLeaderboard(Long auctionId) {
        String leaderboardKey = "auction:" + auctionId + ":leaderboard";
        // 获取排行榜前10名
        Set<Object> topUsers = redisService.get(leaderboardKey + ":top10");

        List<LeaderboardEntry> leaderboard = topUsers.stream()
            .map(obj -> {
                String[] parts = obj.toString().split(":");
                return new LeaderboardEntry(
                    Long.parseLong(parts[0]),
                    maskUsername(parts[1]),
                    new BigDecimal(parts[2])
                );
            })
            .collect(Collectors.toList());

        roomManager.broadcastToRoom(
            auctionId.toString(),
            WsMessage.of(MessageType.LEADERBOARD_UPDATE, leaderboard, auctionId)
        );
    }

    /**
     * 广播竞拍延时
     */
    public void broadcastAuctionExtended(Auction auction, Integer extendedSeconds) {
        AuctionExtendedMessage message = AuctionExtendedMessage.builder()
            .auctionId(auction.getId())
            .oldEndTime(auction.getEndTime().minusSeconds(extendedSeconds))
            .newEndTime(auction.getEndTime())
            .extendedSeconds(extendedSeconds)
            .reason("有新出价，自动延长")
            .build();

        roomManager.broadcastToRoom(
            auction.getId().toString(),
            WsMessage.of(MessageType.AUCTION_EXTENDED, message, auction.getId())
        );
    }

    /**
     * 广播竞拍开始
     */
    public void broadcastAuctionStart(Auction auction) {
        roomManager.broadcastToRoom(
            auction.getId().toString(),
            WsMessage.of(MessageType.AUCTION_START,
                Map.of("auctionId", auction.getId(), "startTime", auction.getStartTime()),
                auction.getId()
            )
        );
    }

    /**
     * 广播竞拍结束
     */
    public void broadcastAuctionEnd(Auction auction, Long winnerId, String winnerName) {
        roomManager.broadcastToRoom(
            auction.getId().toString(),
            WsMessage.of(MessageType.AUCTION_END,
                Map.of(
                    "auctionId", auction.getId(),
                    "finalPrice", auction.getCurrentPrice(),
                    "winnerId", winnerId,
                    "winnerName", maskUsername(winnerName)
                ),
                auction.getId()
            )
        );
    }

    /**
     * 发送个人消息：你领先了
     */
    public void sendYouAreLeading(Long userId, Long auctionId, BigDecimal amount) {
        roomManager.sendToUser(userId, WsMessage.of(
            MessageType.YOU_ARE_LEADING,
            Map.of("auctionId", auctionId, "amount", amount),
            auctionId
        ));
    }

    /**
     * 发送个人消息：你被超越了
     */
    public void sendYouWereOvertaken(Long userId, Long auctionId, BigDecimal currentPrice) {
        roomManager.sendToUser(userId, WsMessage.of(
            MessageType.YOU_WERE_OVERTAKEN,
            Map.of("auctionId", auctionId, "currentPrice", currentPrice),
            auctionId
        ));
    }

    /**
     * 发送个人消息：你赢了
     */
    public void sendYouWon(Long userId, Long auctionId, BigDecimal finalAmount) {
        roomManager.sendToUser(userId, WsMessage.of(
            MessageType.YOU_WON,
            Map.of("auctionId", auctionId, "finalAmount", finalAmount),
            auctionId
        ));
    }

    /**
     * 发送个人消息：你输了
     */
    public void sendYouLost(Long userId, Long auctionId) {
        roomManager.sendToUser(userId, WsMessage.of(
            MessageType.YOU_LOST,
            Map.of("auctionId", auctionId, "message", "很遗憾，您未成交"),
            auctionId
        ));
    }

    /**
     * 脱敏用户名
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return "***";
        }
        return username.substring(0, 2) + "***";
    }

    /**
     * 排行榜条目
     */
    private record LeaderboardEntry(Long userId, String username, BigDecimal amount) {}
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加 WebSocket 消息服务"
```

---

## Task 10: 实现出价服务

**Files:**
- Create: `src/main/java/com/auction/service/bid/BidService.java`
- Create: `src/main/java/com/auction/repository/BidRepository.java`

- [ ] **Step 1: 创建 BidRepository**

```java
package com.auction.repository;

import com.auction.domain.entity.Bid;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.auction.infrastructure.mapper.BidMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BidRepository {

    private final BidMapper bidMapper;

    public Bid save(Bid bid) {
        bidMapper.insert(bid);
        return bid;
    }

    public Bid findById(Long id) {
        return bidMapper.selectById(id);
    }

    public List<Bid> findByAuctionId(Long auctionId) {
        return bidMapper.selectList(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionId, auctionId)
                .orderByDesc(Bid::getAmount)
        );
    }

    public List<Bid> findByAuctionIdAndUserId(Long auctionId, Long userId) {
        return bidMapper.selectList(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionId, auctionId)
                .eq(Bid::getUserId, userId)
                .orderByDesc(Bid::getCreatedAt)
        );
    }

    public Bid findHighestByAuctionId(Long auctionId) {
        return bidMapper.selectOne(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionId, auctionId)
                .eq(Bid::getStatus, "ACTIVE")
                .orderByDesc(Bid::getAmount)
                .last("LIMIT 1")
        );
    }

    public Bid findHighestByAuctionId(Long auctionId, LocalDateTime since) {
        return bidMapper.selectOne(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionId, auctionId)
                .eq(Bid::getStatus, "ACTIVE")
                .ge(Bid::getCreatedAt, since)
                .orderByDesc(Bid::getAmount)
                .last("LIMIT 1")
        );
    }

    public IPage<Bid> findByAuctionIdPage(Long auctionId, int pageNum, int pageSize) {
        Page<Bid> page = new Page<>(pageNum, pageSize);
        return bidMapper.selectPage(page,
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionId, auctionId)
                .orderByDesc(Bid::getCreatedAt)
        );
    }

    public Long countByAuctionId(Long auctionId) {
        return bidMapper.selectCount(
            new LambdaQueryWrapper<Bid>().eq(Bid::getAuctionId, auctionId)
        );
    }

    public void updateById(Bid bid) {
        bidMapper.updateById(bid);
    }

    public List<Bid> findRecentByAuctionId(Long auctionId, int limit) {
        return bidMapper.selectList(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionId, auctionId)
                .orderByDesc(Bid::getCreatedAt)
                .last("LIMIT " + limit)
        );
    }
}
```

- [ ] **Step 2: 创建 BidService**

```java
package com.auction.service.bid;

import com.auction.api.dto.response.BidResultResponse;
import com.auction.api.dto.request.PlaceBidRequest;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.Bid;
import com.auction.domain.entity.User;
import com.auction.domain.enums.AuctionStatus;
import com.auction.domain.enums.BidStatus;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.AuctionRepository;
import com.auction.repository.BidRepository;
import com.auction.repository.UserRepository;
import com.auction.service.auction.AuctionScheduler;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final BidValidator bidValidator;
    private final DistributedLockService lockService;
    private final WsMessageService wsMessageService;
    private final AuctionScheduler auctionScheduler;
    private final RedisService redisService;

    /**
     * 提交出价
     */
    @Transactional
    public BidResultResponse placeBid(PlaceBidRequest request) {
        // 使用分布式锁执行
        return lockService.executeWithLock(
            request.getAuctionId().toString(),
            () -> doPlaceBid(request)
        );
    }

    /**
     * 实际出价逻辑（锁内执行）
     */
    private BidResultResponse doPlaceBid(PlaceBidRequest request) {
        // 1. 获取竞拍信息
        Auction auction = auctionRepository.findById(request.getAuctionId());
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 2. 获取用户信息
        User user = userRepository.findById(request.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. 校验出价
        bidValidator.validate(auction, request.getUserId(), request.getAmount());

        // 4. 获取之前的价格（用于通知被超越者）
        Long previousHighestBidder = getHighestBidder(auction);
        BigDecimal previousPrice = getCurrentPrice(auction);

        // 5. 更新 Redis
        updateAuctionState(auction, request.getAmount(), request.getUserId());

        // 6. 更新排行榜
        updateLeaderboard(auction.getId(), request.getUserId(), request.getAmount());

        // 7. 保存出价记录
        Bid bid = new Bid();
        bid.setAuctionId(request.getAuctionId());
        bid.setUserId(request.getUserId());
        bid.setAmount(request.getAmount());
        bid.setStatus(BidStatus.ACTIVE.name());
        bid.setIsAutoBid(request.getIsAutoBid() != null ? request.getIsAutoBid() : false);
        bid = bidRepository.save(bid);

        // 8. 更新竞拍当前价格
        auction.setCurrentPrice(request.getAmount());
        auction.setHighestBidder(request.getUserId());
        auctionRepository.updateById(auction);

        // 9. 检查自动延时
        boolean wasExtended = auctionScheduler.checkAndExtend(auction);

        // 10. 计算排名
        Integer rank = calculateRank(auction.getId(), request.getUserId());

        // 11. WebSocket 推送
        wsMessageService.broadcastNewBid(auction, bid, user.getNickname(), rank);
        wsMessageService.broadcastPriceUpdate(request.getAuctionId());

        if (wasExtended) {
            wsMessageService.broadcastAuctionExtended(auction, auction.getDelaySeconds());
        }

        // 12. 通知被超越者
        if (previousHighestBidder != null && !previousHighestBidder.equals(request.getUserId())) {
            wsMessageService.sendYouWereOvertaken(
                previousHighestBidder,
                auction.getId(),
                request.getAmount()
            );
        }

        // 13. 通知当前领先者
        wsMessageService.sendYouAreLeading(
            request.getUserId(),
            auction.getId(),
            request.getAmount()
        );

        // 14. 更新用户统计
        user.setTotalBids(user.getTotalBids() + 1);
        userRepository.updateById(user);

        // 15. 计算剩余时间
        Long remainingMs = calculateRemainingMs(auction);

        log.info("出价成功: auctionId={}, userId={}, amount={}, rank={}",
            auction.getId(), request.getUserId(), request.getAmount(), rank);

        // 16. 构建响应
        return BidResultResponse.builder()
            .bidId(bid.getId())
            .currentPrice(request.getAmount())
            .yourRank(rank)
            .isLeading(true)
            .remainingMs(remainingMs)
            .wasExtended(wasExtended)
            .newEndTime(wasExtended ? Duration.between(
                LocalDateTime.now(), auction.getEndTime()
            ).toSeconds() : null)
            .message("出价成功")
            .build();
    }

    /**
     * 更新竞拍状态到 Redis
     */
    private void updateAuctionState(Auction auction, BigDecimal newPrice, Long userId) {
        String auctionKey = "auction:" + auction.getId();

        redisService.set(auctionKey + ":current_price", newPrice.toString());
        redisService.set(auctionKey + ":highest_bidder", userId.toString());
        redisService.increment(auctionKey + ":bid_count");
    }

    /**
     * 更新排行榜
     */
    private void updateLeaderboard(Long auctionId, Long userId, BigDecimal amount) {
        String leaderboardKey = "auction:" + auctionId + ":leaderboard";
        redisService.zAdd(leaderboardKey, userId.toString(), amount.doubleValue());

        // 更新用户最后出价
        redisService.set("bid:" + auctionId + ":user:" + userId + ":last", amount.toString());
    }

    /**
     * 获取当前最高出价者
     */
    private Long getHighestBidder(Auction auction) {
        String key = "auction:" + auction.getId() + ":highest_bidder";
        Object obj = redisService.get(key);
        return obj != null ? Long.parseLong(obj.toString()) : auction.getHighestBidder();
    }

    /**
     * 获取当前价格
     */
    private BigDecimal getCurrentPrice(Auction auction) {
        String key = "auction:" + auction.getId() + ":current_price";
        Object obj = redisService.get(key);
        return obj != null ? new BigDecimal(obj.toString()) : auction.getCurrentPrice();
    }

    /**
     * 计算用户排名
     */
    private Integer calculateRank(Long auctionId, Long userId) {
        String leaderboardKey = "auction:" + auctionId + ":leaderboard";
        Long rank = redisService.zReverseRank(leaderboardKey, userId.toString());
        return rank != null ? rank.intValue() + 1 : null;
    }

    /**
     * 计算剩余时间
     */
    private Long calculateRemainingMs(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return 0L;
        }
        return Duration.between(now, auction.getEndTime()).toMillis();
    }

    /**
     * 获取竞拍出价记录
     */
    public List<Bid> getBidHistory(Long auctionId) {
        return bidRepository.findRecentByAuctionId(auctionId, 50);
    }

    /**
     * 获取用户在竞拍中的出价记录
     */
    public List<Bid> getUserBids(Long auctionId, Long userId) {
        return bidRepository.findByAuctionIdAndUserId(auctionId, userId);
    }
}
```

- [ ] **Step 3: 创建 UserRepository（如果还没有）**

```java
package com.auction.repository;

import com.auction.domain.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.auction.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public User save(User user) {
        userMapper.insert(user);
        return user;
    }

    public User updateById(User user) {
        userMapper.updateById(user);
        return user;
    }

    public User findByUsername(String username) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add .
git commit -m "feat: 添加出价服务"
```

---

## Task 11: 创建出价 Controller

**Files:**
- Create: `src/main/java/com/auction/api/controller/BidController.java`

- [ ] **Step 1: 创建 BidController**

```java
package com.auction.api.controller;

import com.auction.api.dto.request.PlaceBidRequest;
import com.auction.api.dto.response.BidResultResponse;
import com.auction.common.Result;
import com.auction.domain.entity.Bid;
import com.auction.service.bid.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping
    public Result<BidResultResponse> placeBid(@Valid @RequestBody PlaceBidRequest request) {
        return Result.ok(bidService.placeBid(request));
    }

    @GetMapping("/auction/{auctionId}")
    public Result<List<Bid>> getBidHistory(@PathVariable Long auctionId) {
        return Result.ok(bidService.getBidHistory(auctionId));
    }

    @GetMapping("/auction/{auctionId}/user/{userId}")
    public Result<List<Bid>> getUserBids(
            @PathVariable Long auctionId,
            @PathVariable Long userId) {
        return Result.ok(bidService.getUserBids(auctionId, userId));
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加出价 Controller"
```

---

## Task 12: 测试出价和 WebSocket

- [ ] **Step 1: 启动应用**

```bash
mvn spring-boot:run
```

- [ ] **Step 2: 创建测试商品和竞拍**

```bash
# 创建商品
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"测试商品","category":"测试"}'

# 创建竞拍
curl -X POST http://localhost:8080/api/auctions \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "title": "测试竞拍",
    "startPrice": 100,
    "bidIncrement": 10,
    "startTime": "2026-05-22T10:00:00",
    "endTime": "2026-12-31T23:59:59"
  }'

# 开始竞拍
curl -X POST http://localhost:8080/api/auctions/start \
  -H "Content-Type: application/json" \
  -d '{"auctionId": 1}'
```

- [ ] **Step 3: 测试出价 API**

```bash
# 用户1出价
curl -X POST http://localhost:8080/api/bids \
  -H "Content-Type: application/json" \
  -d '{"auctionId": 1, "userId": 1, "amount": 110}'

# 用户2出价
curl -X POST http://localhost:8080/api/bids \
  -H "Content-Type: application/json" \
  -d '{"auctionId": 1, "userId": 2, "amount": 120}'
```

Expected: 返回出价成功响应

- [ ] **Step 4: 测试 WebSocket 连接**

使用 WebSocket 客户端（如浏览器控制台）：

```javascript
// 连接 WebSocket
const ws = new WebSocket('ws://localhost:8080/api/ws/auction/1?userId=1');

ws.onopen = () => console.log('Connected');

ws.onmessage = (event) => {
  const msg = JSON.parse(event.data);
  console.log('Received:', msg);
};

// 发送心跳
setInterval(() => {
  ws.send(JSON.stringify({ type: 'PING' }));
}, 30000);
```

- [ ] **Step 5: 验证并发出价**

使用多个终端同时发送出价请求：

```bash
# 终端1
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/bids \
    -H "Content-Type: application/json" \
    -d "{\"auctionId\": 1, \"userId\": 1, \"amount\": $((130 + i * 10))}"
done

# 终端2（同时执行）
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/bids \
    -H "Content-Type: application/json" \
    -d "{\"auctionId\": 1, \"userId\": 2, \"amount\": $((230 + i * 10))}"
done
```

Expected: 所有出价正确处理，数据一致

- [ ] **Step 6: 验证 Redis 数据**

```bash
redis-cli
> GET auction:1:current_price
> GET auction:1:bid_count
> ZREVRANGE auction:1:leaderboard 0 4 WITHSCORES
```

Expected: 数据正确更新

- [ ] **Step 7: 提交**

```bash
git add .
git commit -m "test: 验证出价和 WebSocket 功能"
```

---

## 验收标准

完成本计划后，应该能够：

1. ✅ 用户可以成功出价
2. ✅ 出价校验正确（金额、频率、状态等）
3. ✅ 分布式锁保证并发安全
4. ✅ WebSocket 连接正常
5. ✅ 新出价实时推送到所有在线用户
6. ✅ 排行榜正确更新
7. ✅ 心跳保活机制正常
8. ✅ 自动延时功能正常
9. ✅ 被超越者收到通知

---

## 下一步

完成本计划后，继续 **Plan 4: 前端框架 + 核心页面**，实现移动端 H5 和管理后台。
