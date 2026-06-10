package com.auction.service.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket房间管理器
 * <p>
 * 负责管理WebSocket连接的房间、会话和用户映射关系，提供消息广播和单点推送功能
 */
@Slf4j
@Service
public class WsRoomManager {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * 用户加入房间
     *
     * @param roomId 房间ID（如竞拍ID）
     * @param session WebSocket会话
     * @param userId 用户ID
     */
    public void joinRoom(String roomId, WebSocketSession session, Long userId) {
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionRoomMap.put(session.getId(), roomId);
        sessionUserMap.put(session.getId(), userId);

        int roomSize = rooms.get(roomId).size();
        log.info("🏠 [房间管理] 用户加入房间");
        log.info("  └─ 房间ID: {}", roomId);
        log.info("  └─ 用户ID: {}", userId);
        log.info("  └─ 会话ID: {}", session.getId());
        log.info("  └─ 当前房间人数: {}", roomSize);
        log.info("  └─ 总房间数: {}", rooms.size());
        log.info("----------------------------------");

        sendToSession(session, Map.of("type", "CONNECT", "roomId", roomId, "timestamp", System.currentTimeMillis()));
    }

    /**
     * 用户离开房间
     * <p>
     * 清理会话相关的所有映射关系，如果房间为空则自动删除房间
     *
     * @param session WebSocket会话
     */
    public void leaveRoom(WebSocketSession session) {
        String roomId = sessionRoomMap.remove(session.getId());
        if (roomId != null) {
            Set<WebSocketSession> roomSessions = rooms.get(roomId);
            if (roomSessions != null) {
                roomSessions.remove(session);
                int remainingUsers = roomSessions.size();
                log.info("🏠 [房间管理] 用户离开房间");
                log.info("  └─ 房间ID: {}", roomId);
                log.info("  └─ 会话ID: {}", session.getId());
                log.info("  └─ 剩余人数: {}", remainingUsers);

                if (roomSessions.isEmpty()) {
                    rooms.remove(roomId);
                    log.info("  └─ 房间已删除（无用户）");
                }
                log.info("----------------------------------");
            }
        }
        sessionUserMap.remove(session.getId());
    }

    /**
     * 广播消息到房间内所有在线用户
     * <p>
     * 向指定房间的所有活跃WebSocket会话发送消息，自动跳过已关闭的连接
     *
     * @param roomId 房间ID
     * @param message 消息内容Map对象
     */
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

    /**
     * 发送消息给指定用户（支持多设备同时推送）
     * <p>
     * 根据用户ID查找所有活跃会话并推送消息，适用于用户在多个设备登录的场景
     *
     * @param userId 用户ID
     * @param message 消息内容Map对象
     */
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

    /**
     * 发送消息给指定会话
     * <p>
     * 内部方法，仅向会话状态为活跃的连接发送消息
     *
     * @param session WebSocket会话
     * @param message 消息内容Map对象
     */
    private void sendToSession(WebSocketSession session, Map<String, Object> message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(toJson(message)));
            } catch (IOException e) {
                log.error("发送消息失败: sessionId={}", session.getId(), e);
            }
        }
    }

    /**
     * 获取房间当前在线人数
     *
     * @param roomId 房间ID
     * @return 在线人数，房间不存在时返回0
     */
    public int getRoomSize(String roomId) {
        Set<WebSocketSession> roomSessions = rooms.get(roomId);
        return roomSessions == null ? 0 : roomSessions.size();
    }

    /**
     * 根据会话ID查找WebSocket会话
     * <p>
     * 遍历所有房间查找指定ID的会话，性能较低，仅在必要时使用
     *
     * @param sessionId 会话ID
     * @return WebSocket会话对象，未找到返回null
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
     * 将Map对象转换为JSON字符串
     * <p>
     * 简单的JSON序列化实现，支持String、Number、Boolean类型
     *
     * @param map 待转换的Map对象
     * @return JSON字符串
     */
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
