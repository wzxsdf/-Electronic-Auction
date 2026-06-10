package com.auction.service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Slf4j
@Component
public class AuctionWebSocketHandler extends TextWebSocketHandler {

    private final WsRoomManager roomManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuctionWebSocketHandler(WsRoomManager roomManager) {
        this.roomManager = roomManager;
    }

    /**
     * WebSocket连接建立后的处理
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 URI 路径中提取拍品ID
        // 连接格式: /api/ws/item/{itemId}?userId={userId}
        String uri = session.getUri().getPath();
        String[] parts = uri.split("/");

        if (parts.length < 5 || !"item".equals(parts[3])) {
            log.warn("无效的URI格式: uri={}, 期望格式: /api/ws/item/{{itemId}}?userId={{userId}}", uri);
            session.close();
            return;
        }

        String itemIdStr = parts[4];
        Long itemId = Long.parseLong(itemIdStr);
        Long userId = extractUserId(session);

        if (userId == null) {
            log.warn("无法提取 userId: uri={}", uri);
            session.close();
            return;
        }

        // 🎉 拍品详情页 WebSocket 连接建立
        log.info("🔗 [拍品WebSocket] 新连接建立");
        log.info("  └─ 拍品ID: {}", itemId);
        log.info("  └─ 用户ID: {}", userId);
        log.info("  └─ 会话ID: {}", session.getId());
        log.info("  └─ 房间KEY: item:{}", itemId);
        log.info("  └─ 连接时间: {}", java.time.LocalDateTime.now());
        log.info("====================================");

        // 统一使用 "item:" + itemId 格式作为roomKey
        String roomKey = "item:" + itemId;
        roomManager.joinRoom(roomKey, session, userId);
    }

    /**
     * 处理WebSocket文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            if ("PING".equals(type)) {
                session.sendMessage(new TextMessage(
                    objectMapper.writeValueAsString(Map.of("type", "PONG", "timestamp", System.currentTimeMillis()))
                ));
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * WebSocket连接关闭后的处理
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("🔌 [拍品WebSocket] 连接关闭");
        log.info("  └─ 会话ID: {}", session.getId());
        log.info("  └─ 关闭代码: {}", status.getCode());
        log.info("  └─ 关闭原因: {}", status.getReason());
        log.info("  └─ 关闭时间: {}", java.time.LocalDateTime.now());
        log.info("====================================");

        roomManager.leaveRoom(session);
    }

    /**
     * 处理WebSocket传输错误
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("❌ [拍品WebSocket] 传输错误");
        log.error("  └─ 会话ID: {}", session.getId());
        log.error("  └─ 错误信息: {}", exception.getMessage());
        log.error("  └─ 错误时间: {}", java.time.LocalDateTime.now());
        log.error("====================================");

        roomManager.leaveRoom(session);
    }

    /**
     * 从WebSocket会话中提取用户ID
     */
    private Long extractUserId(WebSocketSession session) {
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
