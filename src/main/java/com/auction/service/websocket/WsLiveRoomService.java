package com.auction.service.websocket;

import com.auction.domain.entity.ChatMessage;
import com.auction.domain.entity.LiveRoomUser;
import com.auction.domain.enums.MessageType;
import com.auction.infrastructure.redis.RedisService;

import java.util.concurrent.TimeUnit;
import com.auction.repository.ChatMessageRepository;
import com.auction.repository.LiveRoomUserRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 直播间服务
 * <p>
 * 处理直播间核心功能：聊天、用户管理、在线统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsLiveRoomService {

    private final WsRoomManager roomManager;
    private final RedisService redisService;
    private final ChatMessageRepository chatMessageRepository;
    private final LiveRoomUserRepository liveRoomUserRepository;

    // 内存缓存：直播间在线用户
    private final Map<Long, Set<Long>> liveRoomUsers = new ConcurrentHashMap<>();
    // 内存缓存：用户所在直播间
    private final Map<Long, Long> userRoomMap = new ConcurrentHashMap<>();

    /**
     * 处理用户加入直播间
     */
    @Async("websocketTaskExecutor")
    public void handleUserJoin(Long auctionId, Long userId, String username, WebSocketSession session) {
        try {
            // 1. 添加到内存缓存
            liveRoomUsers.computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet()).add(userId);
            userRoomMap.put(userId, auctionId);

            // 2. 保存或更新数据库记录
            LiveRoomUser roomUser = liveRoomUserRepository.selectOne(
                    new LambdaQueryWrapper<LiveRoomUser>()
                            .eq(LiveRoomUser::getAuctionId, auctionId)
                            .eq(LiveRoomUser::getUserId, userId)
            );

            if (roomUser == null) {
                // 新用户，插入记录
                roomUser = LiveRoomUser.builder()
                        .auctionId(auctionId)
                        .userId(userId)
                        .username(username)
                        .joinTime(LocalDateTime.now())
                        .lastActiveTime(LocalDateTime.now())
                        .isOnline(true)
                        .build();
                liveRoomUserRepository.insert(roomUser);
            } else {
                // 已存在用户，更新状态
                roomUser.setIsOnline(true);
                roomUser.setLastActiveTime(LocalDateTime.now());
                liveRoomUserRepository.updateById(roomUser);
            }

            // 3. 更新Redis在线计数
            String onlineKey = "live:online:" + auctionId;
            redisService.increment(onlineKey);
            redisService.expire(onlineKey, 3600, TimeUnit.SECONDS); // 1小时过期

            // 4. 广播用户加入消息
            Map<String, Object> joinData = Map.of(
                    "auctionId", auctionId,
                    "userId", userId,
                    "username", username,
                    "onlineCount", getOnlineCount(auctionId),
                    "timestamp", System.currentTimeMillis()
            );

            roomManager.broadcastToRoom(
                    "live:" + auctionId,
                    createMessage(MessageType.USER_JOIN, joinData, auctionId)
            );

            // 5. 发送欢迎消息
            sendWelcomeMessage(auctionId, username);

            // 6. 发送初始数据（最近聊天记录）
            sendRecentMessages(auctionId, session);

            log.info("✅ [直播间服务] 用户加入处理完成");
            log.info("  └─ 拍卖活动ID: {}", auctionId);
            log.info("  └─ 用户ID: {}", userId);
            log.info("  └─ 用户名: {}", username);
            log.info("  └─ 当前在线人数: {}", getOnlineCount(auctionId));
            log.info("  └─ 内存缓存用户数: {}", liveRoomUsers.getOrDefault(auctionId, Collections.emptySet()).size());
            log.info("  └─ 处理时间: {}", java.time.LocalDateTime.now());
            log.info("====================================");

        } catch (Exception e) {
            log.error("处理用户加入失败: auctionId={}, userId={}", auctionId, userId, e);
        }
    }

    /**
     * 处理用户离开直播间
     */
    @Async("websocketTaskExecutor")
    public void handleUserLeave(Long auctionId, Long userId) {
        try {
            // 1. 从内存缓存移除
            Set<Long> users = liveRoomUsers.get(auctionId);
            if (users != null) {
                users.remove(userId);
                if (users.isEmpty()) {
                    liveRoomUsers.remove(auctionId);
                }
            }
            userRoomMap.remove(userId);

            // 2. 更新数据库状态
            LiveRoomUser roomUser = liveRoomUserRepository.selectOne(
                    new LambdaQueryWrapper<LiveRoomUser>()
                            .eq(LiveRoomUser::getAuctionId, auctionId)
                            .eq(LiveRoomUser::getUserId, userId)
            );

            if (roomUser != null) {
                roomUser.setIsOnline(false);
                roomUser.setLastActiveTime(LocalDateTime.now());
                liveRoomUserRepository.updateById(roomUser);
            }

            // 3. 更新Redis在线计数
            String onlineKey = "live:online:" + auctionId;
            Long currentCount = redisService.get(onlineKey) != null ?
                    Long.parseLong(redisService.get(onlineKey).toString()) : 0L;
            if (currentCount > 0) {
                redisService.decrement(onlineKey);
            }

            // 4. 广播用户离开消息
            Map<String, Object> leaveData = Map.of(
                    "auctionId", auctionId,
                    "userId", userId,
                    "onlineCount", getOnlineCount(auctionId),
                    "timestamp", System.currentTimeMillis()
            );

            roomManager.broadcastToRoom(
                    "live:" + auctionId,
                    createMessage(MessageType.USER_LEAVE, leaveData, auctionId)
            );

            log.info("用户离开直播间: auctionId={}, userId={}, onlineCount={}",
                    auctionId, userId, getOnlineCount(auctionId));

        } catch (Exception e) {
            log.error("处理用户离开失败: auctionId={}, userId={}", auctionId, userId, e);
        }
    }

    /**
     * 处理聊天消息
     */
    @Async("websocketTaskExecutor")
    public void handleChatMessage(Long auctionId, Long userId, String content) {
        try {
            // 1. 验证用户是否在直播间
            if (!isUserInLiveRoom(auctionId, userId)) {
                log.warn("用户不在直播间: auctionId={}, userId={}", auctionId, userId);
                return;
            }

            // 2. 验证消息长度
            if (content.length() > 500) {
                log.warn("消息长度超过限制: auctionId={}, userId={}, length={}", auctionId, userId, content.length());
                return;
            }

            // 3. 敏感词过滤
            String filteredContent = filterSensitiveWords(content);

            // 4. 获取用户信息
            String username = getUsername(userId);

            // 5. 保存到数据库
            ChatMessage message = ChatMessage.builder()
                    .auctionId(auctionId)
                    .userId(userId)
                    .username(username)
                    .content(filteredContent)
                    .messageType(1) // 用户消息
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            chatMessageRepository.insert(message);

            // 6. 更新用户活跃时间
            updateUserActiveTime(auctionId, userId);

            // 7. 广播到直播间
            Map<String, Object> chatData = Map.of(
                    "auctionId", auctionId,
                    "messageId", message.getId(),
                    "userId", userId,
                    "username", maskUsername(username),
                    "content", filteredContent,
                    "timestamp", System.currentTimeMillis()
            );

            roomManager.broadcastToRoom(
                    "live:" + auctionId,
                    createMessage(MessageType.CHAT_MESSAGE, chatData, auctionId)
            );

            log.debug("聊天消息已广播: auctionId={}, userId={}, messageId={}",
                    auctionId, userId, message.getId());

        } catch (Exception e) {
            log.error("处理聊天消息失败: auctionId={}, userId={}", auctionId, userId, e);
        }
    }

    /**
     * 发送用户列表
     */
    @Async("websocketTaskExecutor")
    public void sendUserList(Long auctionId, WebSocketSession session) {
        try {
            // 从数据库查询在线用户
            List<LiveRoomUser> onlineUsers = liveRoomUserRepository
                    .findAllByAuctionIdAndIsOnlineOrderByLastActiveTimeDesc(auctionId, true);

            List<Map<String, Object>> userList = onlineUsers.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("userId", user.getUserId());
                        userMap.put("username", user.getUsername());
                        userMap.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
                        userMap.put("joinTime", user.getJoinTime().toString());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> listData = Map.of(
                    "auctionId", auctionId,
                    "users", userList,
                    "total", userList.size(),
                    "timestamp", System.currentTimeMillis()
            );

            // 发送给请求用户
            Long userId = extractUserIdFromSession(session);
            if (userId != null) {
                roomManager.sendToUser(userId, createMessage(MessageType.USER_LIST_UPDATE, listData, auctionId));
                log.debug("发送用户列表: auctionId={}, userId={}, count={}", auctionId, userId, userList.size());
            }

        } catch (Exception e) {
            log.error("发送用户列表失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 广播所有拍品状态更新
     * <p>
     * 当直播间有拍品状态变化时，批量更新所有拍品的状态
     */
    @Async("websocketTaskExecutor")
    public void broadcastAllItemsUpdate(Long auctionId, List<Map<String, Object>> itemsData) {
        try {
            Map<String, Object> updateData = Map.of(
                    "auctionId", auctionId,
                    "items", itemsData,
                    "timestamp", System.currentTimeMillis()
            );

            roomManager.broadcastToRoom(
                    "live:" + auctionId,
                    createMessage(MessageType.ALL_ITEMS_UPDATE, updateData, auctionId)
            );

            log.debug("广播所有拍品更新: auctionId={}, itemCount={}", auctionId, itemsData.size());

        } catch (Exception e) {
            log.error("广播所有拍品更新失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(Long auctionId, String username) {
        try {
            Map<String, Object> welcomeData = Map.of(
                    "auctionId", auctionId,
                    "message", String.format("欢迎 %s 加入直播间！", username),
                    "timestamp", System.currentTimeMillis()
            );

            roomManager.broadcastToRoom(
                    "live:" + auctionId,
                    createMessage(MessageType.SYSTEM_MESSAGE, welcomeData, auctionId)
            );

        } catch (Exception e) {
            log.error("发送欢迎消息失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 发送最近聊天记录
     */
    private void sendRecentMessages(Long auctionId, WebSocketSession session) {
        try {
            // 获取最近20条聊天记录
            List<ChatMessage> recentMessages = chatMessageRepository
                    .findRecentMessages(auctionId, 1, 20);

            // 反转顺序（最新的在最后）
            Collections.reverse(recentMessages);

            List<Map<String, Object>> messages = recentMessages.stream()
                    .map(msg -> {
                        Map<String, Object> msgMap = new HashMap<>();
                        msgMap.put("messageId", msg.getId());
                        msgMap.put("userId", msg.getUserId());
                        msgMap.put("username", maskUsername(msg.getUsername()));
                        msgMap.put("content", msg.getContent());
                        msgMap.put("timestamp", msg.getCreatedAt().toString());
                        return msgMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> historyData = Map.of(
                    "auctionId", auctionId,
                    "messages", messages,
                    "total", messages.size()
            );

            // 发送给新加入用户
            Long userId = extractUserIdFromSession(session);
            if (userId != null) {
                roomManager.sendToUser(userId, createMessage(MessageType.CHAT_HISTORY, historyData, auctionId));
                log.debug("发送聊天历史: auctionId={}, userId={}, count={}", auctionId, userId, messages.size());
            }

        } catch (Exception e) {
            log.error("发送聊天历史失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 获取在线人数
     */
    public int getOnlineCount(Long auctionId) {
        String onlineKey = "live:online:" + auctionId;
        Object count = redisService.get(onlineKey);
        return count != null ? Integer.parseInt(count.toString()) : 0;
    }

    /**
     * 检查用户是否在直播间
     */
    private boolean isUserInLiveRoom(Long auctionId, Long userId) {
        Set<Long> users = liveRoomUsers.get(auctionId);
        return users != null && users.contains(userId);
    }

    /**
     * 更新用户活跃时间
     */
    private void updateUserActiveTime(Long auctionId, Long userId) {
        try {
            LiveRoomUser user = liveRoomUserRepository.selectOne(
                    new LambdaQueryWrapper<LiveRoomUser>()
                            .eq(LiveRoomUser::getAuctionId, auctionId)
                            .eq(LiveRoomUser::getUserId, userId)
            );

            if (user != null) {
                user.setLastActiveTime(LocalDateTime.now());
                liveRoomUserRepository.updateById(user);
            }
        } catch (Exception e) {
            log.error("更新用户活跃时间失败: auctionId={}, userId={}", auctionId, userId, e);
        }
    }

    /**
     * 获取用户名（TODO: 从用户服务获取）
     */
    private String getUsername(Long userId) {
        // TODO: 从用户服务或缓存获取
        return "用户" + userId;
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
     * 敏感词过滤（TODO: 实现具体的过滤逻辑）
     */
    private String filterSensitiveWords(String content) {
        // TODO: 实现敏感词过滤逻辑
        // 可以使用DFA算法或调用第三方API
        return content;
    }

    /**
     * 创建标准消息格式
     */
    private Map<String, Object> createMessage(MessageType type, Object data, Long auctionId) {
        return Map.of(
                "type", type.name(),
                "data", data,
                "timestamp", System.currentTimeMillis(),
                "auctionId", auctionId
        );
    }

    /**
     * 从会话中提取用户ID
     */
    private Long extractUserIdFromSession(WebSocketSession session) {
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
}