package com.auction.service.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WsRoomManager {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    public void joinRoom(String roomId, WebSocketSession session, Long userId) {
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionRoomMap.put(session.getId(), roomId);
        sessionUserMap.put(session.getId(), userId);

        log.info("用户加入房间: roomId={}, userId={}, sessionId={}", roomId, userId, session.getId());

        sendToSession(session, Map.of("type", "CONNECT", "roomId", roomId, "timestamp", System.currentTimeMillis()));
    }

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
    }

    public void broadcastToRoom(String roomId, Map<String, Object> message) {
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions == null) return;

        String json = toJson(message);
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
            roomId, message.get("type"), roomSessions.size());
    }

    public void sendToUser(Long userId, Map<String, Object> message) {
        sessionUserMap.entrySet().stream()
            .filter(e -> e.getValue().equals(userId))
            .forEach(e -> {
                WebSocketSession session = findSession(e.getKey());
                if (session != null && session.isOpen()) {
                    sendToSession(session, message);
                }
            });
    }

    private void sendToSession(WebSocketSession session, Map<String, Object> message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(toJson(message)));
            } catch (IOException e) {
                log.error("发送消息失败: sessionId={}", session.getId(), e);
            }
        }
    }

    public int getRoomSize(String roomId) {
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        return roomSessions == null ? 0 : roomSessions.size();
    }

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

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(value).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
