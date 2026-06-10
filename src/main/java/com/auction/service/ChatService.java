package com.auction.service;

import com.auction.api.dto.request.SendMessageRequest;
import com.auction.api.dto.response.ChatHistoryResponse;
import com.auction.api.dto.response.ChatMessageResponse;
import com.auction.api.dto.response.OnlineUsersResponse;
import com.auction.common.BizException;
import com.auction.domain.entity.ChatMessage;
import com.auction.domain.entity.LiveRoomUser;
import com.auction.repository.ChatMessageRepository;
import com.auction.repository.LiveRoomUserRepository;
import com.auction.service.websocket.WsLiveRoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天服务
 * <p>
 * 提供直播间聊天功能的业务逻辑层，包括：
 * <ul>
 * <li>发送聊天消息（通过WebSocket广播）</li>
 * <li>查询聊天历史记录</li>
 * <li>获取在线用户列表</li>
 * <li>消息管理（删除、举报等）</li>
 * </ul>
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final LiveRoomUserRepository liveRoomUserRepository;
    private final WsLiveRoomService wsLiveRoomService;

    /**
     * 发送聊天消息
     * <p>
     * 通过WebSocket实时广播消息到直播间，同时保存到数据库
     *
     * @param request   发送消息请求
     * @param userId   当前登录用户ID
     * @return 发送的消息响应
     * @throws BizException 如果消息内容为空或用户不在直播间
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageResponse sendMessage(SendMessageRequest request, Long userId) {
        // 1. 参数校验
        if (request.getAuctionId() == null) {
            throw new BizException(400, "拍卖活动ID不能为空");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BizException(400, "消息内容不能为空");
        }
        if (request.getContent().length() > 500) {
            throw new BizException(400, "消息内容不能超过500字");
        }

        // 2. 检查用户是否在直播间（可选校验）
        LiveRoomUser roomUser = liveRoomUserRepository.selectOne(
                new LambdaQueryWrapper<LiveRoomUser>()
                        .eq(LiveRoomUser::getAuctionId, request.getAuctionId())
                        .eq(LiveRoomUser::getUserId, userId)
                        .eq(LiveRoomUser::getIsOnline, true)
        );

        if (roomUser == null) {
            log.warn("用户不在直播间: auctionId={}, userId={}", request.getAuctionId(), userId);
            // 可以选择抛出异常或允许发送
            // throw new BizException(400, "您还未加入直播间");
        }

        // 3. 通过WebSocket服务发送消息（会自动广播和保存）
        wsLiveRoomService.handleChatMessage(request.getAuctionId(), userId, request.getContent().trim());

        // 4. 查询刚保存的消息记录
        ChatMessage lastMessage = chatMessageRepository.selectOne(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getAuctionId, request.getAuctionId())
                        .eq(ChatMessage::getUserId, userId)
                        .orderByDesc(ChatMessage::getCreatedAt)
                        .last("LIMIT 1")
        );

        if (lastMessage != null) {
            return convertToResponse(lastMessage);
        }

        // 如果查询失败，返回基本信息
        return ChatMessageResponse.builder()
                .auctionId(request.getAuctionId())
                .userId(userId)
                .content(request.getContent())
                .messageType(1)
                .build();
    }

    /**
     * 获取聊天历史记录
     * <p>
     * 分页查询指定拍卖活动的聊天消息，支持按消息类型筛选
     *
     * @param auctionId  拍卖活动ID
     * @param page       页码（从1开始）
     * @param size       每页大小（最大100）
     * @param messageType 消息类型（可选，1-用户消息，2-系统消息）
     * @return 聊天历史响应
     */
    public ChatHistoryResponse getChatHistory(Long auctionId, int page, int size, Integer messageType) {
        // 1. 参数校验和修正
        if (auctionId == null) {
            throw new BizException(400, "拍卖活动ID不能为空");
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 20;
        }

        // 2. 构建查询条件
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getAuctionId, auctionId)
                .eq(ChatMessage::getIsDeleted, false);

        if (messageType != null && (messageType == 1 || messageType == 2)) {
            queryWrapper.eq(ChatMessage::getMessageType, messageType);
        }

        // 3. 查询总数
        Long total = chatMessageRepository.selectCount(queryWrapper);

        // 4. 分页查询
        int offset = (page - 1) * size;
        queryWrapper.orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + offset);

        List<ChatMessage> messages = chatMessageRepository.selectList(queryWrapper);

        // 5. 转换为响应DTO
        List<ChatMessageResponse> messageResponses = messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ChatHistoryResponse.builder()
                .auctionId(auctionId)
                .total(total.intValue())
                .messages(messageResponses)
                .build();
    }

    /**
     * 获取在线用户列表
     * <p>
     * 查询指定拍卖活动的当前在线用户信息
     *
     * @param auctionId 拍卖活动ID
     * @return 在线用户响应
     */
    public OnlineUsersResponse getOnlineUsers(Long auctionId) {
        // 1. 参数校验
        if (auctionId == null) {
            throw new BizException(400, "拍卖活动ID不能为空");
        }

        // 2. 查询在线用户
        List<LiveRoomUser> onlineUsers = liveRoomUserRepository
                .findAllByAuctionIdAndIsOnlineOrderByLastActiveTimeDesc(auctionId, true);

        // 3. 转换为响应DTO
        List<OnlineUsersResponse.OnlineUser> userResponses = onlineUsers.stream()
                .map(user -> OnlineUsersResponse.OnlineUser.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .avatar(user.getAvatar() != null ? user.getAvatar() : "")
                        .joinTime(user.getJoinTime() != null ? user.getJoinTime().toString() : "")
                        .build())
                .collect(Collectors.toList());

        return OnlineUsersResponse.builder()
                .auctionId(auctionId)
                .total(userResponses.size())
                .users(userResponses)
                .build();
    }

    /**
     * 获取在线人数
     * <p>
     * 快速查询指定拍卖活动的在线用户数量
     *
     * @param auctionId 拍卖活动ID
     * @return 在线人数
     */
    public int getOnlineCount(Long auctionId) {
        if (auctionId == null) {
            throw new BizException(400, "拍卖活动ID不能为空");
        }
        return wsLiveRoomService.getOnlineCount(auctionId);
    }

    /**
     * 删除聊天消息（软删除）
     * <p>
     * 将消息标记为已删除，实际数据仍保留在数据库中
     *
     * @param messageId 消息ID
     * @param userId    当前登录用户ID（用于权限校验）
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMessage(Long messageId, Long userId) {
        // 1. 参数校验
        if (messageId == null) {
            throw new BizException(400, "消息ID不能为空");
        }

        // 2. 查询消息
        ChatMessage message = chatMessageRepository.selectById(messageId);
        if (message == null) {
            throw new BizException(404, "消息不存在");
        }

        // 3. 权限校验：只有消息发送者可以删除自己的消息
        if (!message.getUserId().equals(userId)) {
            throw new BizException(403, "您只能删除自己的消息");
        }

        // 4. 软删除
        message.setIsDeleted(true);
        return chatMessageRepository.updateById(message) > 0;
    }

    /**
     * 转换为消息响应DTO
     * <p>
     * 包含用户名脱敏处理
     */
    private ChatMessageResponse convertToResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .auctionId(message.getAuctionId())
                .userId(message.getUserId())
                .username(maskUsername(message.getUsername()))
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * 用户名脱敏处理
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return "***";
        }
        return username.substring(0, 2) + "***";
    }
}
