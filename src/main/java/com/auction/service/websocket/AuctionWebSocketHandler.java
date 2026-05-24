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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 URI 路径中提取 ID
        // 支持: /ws/item/{itemId} 或 /ws/room/{roomId}
        String uri = session.getUri().getPath();
        String[] parts = uri.split("/");
        String idStr = null;
        String type = null;

        for (int i = 0; i < parts.length; i++) {
            if ("item".equals(parts[i]) || ("room".equals(parts[i]))) {
                type = parts[i];
                if (i + 1 < parts.length) {
                    idStr = parts[i + 1];
                }
                break;
            }
        }

        if (idStr == null) {
            log.warn("无法从 URI 中提取 ID: uri={}", uri);
            session.close();
            return;
        }

        Long id = Long.parseLong(idStr);
        Long userId = extractUserId(session);

        if (userId == null) {
            log.warn("无法提取 userId: uri={}", uri);
            session.close();
            return;
        }

        log.info("WebSocket 连接建立: type={}, id={}, userId={}, sessionId={}", type, id, userId, session.getId());

        // 根据类型加入不同的房间
        String roomKey = type + ":" + id;
        roomManager.joinRoom(roomKey, session, userId);
    }

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
