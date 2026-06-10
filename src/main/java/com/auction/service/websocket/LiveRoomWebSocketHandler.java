package com.auction.service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLDecoder;
import java.util.Map;

/**
 * 直播间WebSocket处理器
 * <p>
 * 处理直播间WebSocket连接，功能包括：
 * - 聊天消息收发
 * - 用户加入/离开管理
 * - 在线人数统计
 * - 多拍品状态同步
 * <p>
 * 连接格式: ws://host/ws/live/{auctionId}?userId={userId}&username={username}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveRoomWebSocketHandler extends TextWebSocketHandler {

    private final WsRoomManager roomManager;
    private final WsLiveRoomService liveRoomService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从URI提取参数: /api/ws/live/{auctionId}?userId={userId}&username={username}
        String uri = session.getUri().getPath();
        String[] parts = uri.split("/");

        if (parts.length < 5) {
            log.warn("无效的URI格式: uri={}, 期望格式: /api/ws/live/{{auctionId}}?userId={{userId}}&username={{username}}", uri);
            session.close();
            return;
        }

        try {
            Long auctionId = Long.parseLong(parts[4]);
            Long userId = extractUserId(session);
            String username = extractUsername(session);

            if (userId == null) {
                log.warn("无法提取userId: uri={}", uri);
                session.close();
                return;
            }

            // 用户名默认值
            if (username == null || username.trim().isEmpty()) {
                username = "用户" + userId;
            }

            log.info("🎉 [直播间WebSocket] 新用户加入直播间");
            log.info("  └─ 拍卖活动ID: {}", auctionId);
            log.info("  └─ 用户ID: {}", userId);
            log.info("  └─ 用户名: {}", username);
            log.info("  └─ 会话ID: {}", session.getId());
            log.info("  └─ 房间KEY: live:{}", auctionId);
            log.info("  └─ 连接时间: {}", java.time.LocalDateTime.now());
            log.info("====================================");

            // 加入直播间
            String roomKey = "live:" + auctionId;
            roomManager.joinRoom(roomKey, session, userId);

            // 处理用户加入逻辑
            liveRoomService.handleUserJoin(auctionId, userId, username, session);

        } catch (NumberFormatException e) {
            log.error("解析auctionId失败: uri={}", uri, e);
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            if (type == null) {
                log.warn("消息类型为空: sessionId={}", session.getId());
                return;
            }

            Long auctionId = extractAuctionId(session);
            Long userId = extractUserId(session);

            if (auctionId == null || userId == null) {
                log.warn("无法提取auctionId或userId: sessionId={}", session.getId());
                return;
            }

            switch (type) {
                case "CHAT":
                    // 处理聊天消息
                    String content = (String) payload.get("content");
                    if (content != null && !content.trim().isEmpty()) {
                        liveRoomService.handleChatMessage(auctionId, userId, content.trim());
                    } else {
                        log.warn("聊天内容为空: userId={}, auctionId={}", userId, auctionId);
                    }
                    break;

                case "PING":
                    // 心跳响应
                    session.sendMessage(new TextMessage(
                            objectMapper.writeValueAsString(Map.of(
                                    "type", "PONG",
                                    "timestamp", System.currentTimeMillis()
                            ))
                    ));
                    break;

                case "GET_USER_LIST":
                    // 获取用户列表
                    liveRoomService.sendUserList(auctionId, session);
                    break;

                default:
                    log.warn("未知消息类型: type={}, sessionId={}", type, session.getId());
            }

        } catch (Exception e) {
            log.error("处理直播间消息失败: sessionId={}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = extractUserId(session);
        Long auctionId = extractAuctionId(session);

        if (userId != null && auctionId != null) {
            log.info("👋 [直播间WebSocket] 用户离开直播间");
            log.info("  └─ 拍卖活动ID: {}", auctionId);
            log.info("  └─ 用户ID: {}", userId);
            log.info("  └─ 会话ID: {}", session.getId());
            log.info("  └─ 关闭代码: {}", status.getCode());
            log.info("  └─ 关闭原因: {}", status.getReason());
            log.info("  └─ 离开时间: {}", java.time.LocalDateTime.now());
            log.info("====================================");

            // 处理用户离开逻辑
            liveRoomService.handleUserLeave(auctionId, userId);
        }

        // 离开房间
        roomManager.leaveRoom(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("❌ [直播间WebSocket] 传输错误");
        log.error("  └─ 会话ID: {}", session.getId());
        log.error("  └─ 错误类型: {}", exception.getClass().getSimpleName());
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
                log.error("解析userId失败: query={}", query, e);
            }
        }
        return null;
    }

    /**
     * 从WebSocket会话中提取用户名
     */
    private String extractUsername(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("username=")) {
            try {
                String username = query.split("username=")[1].split("&")[0];
                return URLDecoder.decode(username, "UTF-8");
            } catch (Exception e) {
                log.error("解析username失败: query={}", query, e);
            }
        }
        return null;
    }

    /**
     * 从WebSocket会话中提取拍卖活动ID
     */
    private Long extractAuctionId(WebSocketSession session) {
        String uri = session.getUri().getPath();
        String[] parts = uri.split("/");
        if (parts.length >= 5) {
            try {
                return Long.parseLong(parts[4]);  // parts[4]才是auctionId
            } catch (NumberFormatException e) {
                log.error("解析auctionId失败: uri={}", uri, e);
            }
        }
        return null;
    }
}