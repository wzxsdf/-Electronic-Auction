package com.auction.api.controller;

import com.auction.annotation.RateLimit;
import com.auction.api.dto.request.SendMessageRequest;
import com.auction.api.dto.response.ChatHistoryResponse;
import com.auction.api.dto.response.ChatMessageResponse;
import com.auction.api.dto.response.OnlineUsersResponse;
import com.auction.common.Result;
import com.auction.infrastructure.security.CurrentUser;
import com.auction.infrastructure.security.UserPrincipal;
import com.auction.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 聊天控制器
 * <p>
 * 提供直播间聊天相关的REST API接口，补充WebSocket实时通信功能
 * <p>
 * 主要功能：
 * <ul>
 * <li>发送聊天消息（通过WebSocket实时广播）</li>
 * <li>查询聊天历史记录（支持分页）</li>
 * <li>获取在线用户列表</li>
 * <li>获取在线人数统计</li>
 * <li>删除聊天消息</li>
 * </ul>
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送聊天消息
     * <p>
     * 发送消息到指定拍卖活动的直播间，消息会通过WebSocket实时广播给所有在线用户
     * <p>
     * 限流：每分钟最多60次请求
     * <p>
     * WebSocket连接：ws://host/ws/live/{auctionId}?userId={userId}&username={username}
     *
     * @param request    发送消息请求
     * @param currentUser 当前登录用户
     * @return 发送的消息响应
     * @apiNote 使用示例：
     * <pre>{@code
     * POST /chat/send
     * {
     *   "auctionId": 123,
     *   "content": "这件拍品真不错！"
     * }
     * }</pre>
     */
    @PostMapping("/send")
    @RateLimit(key = "chat_send", time = 60, count = 60, message = "发送消息过于频繁，请稍后再试")
    public Result<ChatMessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @CurrentUser UserPrincipal currentUser) {
        try {
            ChatMessageResponse response = chatService.sendMessage(request, currentUser.getUserId());
            return Result.ok(response);
        } catch (Exception e) {
            log.error("发送聊天消息失败: userId={}, auctionId={}",
                    currentUser.getUserId(), request.getAuctionId(), e);
            return Result.fail(500, "发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取聊天历史记录
     * <p>
     * 分页查询指定拍卖活动的聊天消息，支持按消息类型筛选
     * <p>
     * 返回的消息按时间倒序排列，用户名已脱敏处理
     *
     * @param auctionId   拍卖活动ID
     * @param page        页码（从1开始，默认1）
     * @param size        每页大小（默认20，最大100）
     * @param messageType 消息类型（可选，1-用户消息，2-系统消息）
     * @return 聊天历史响应
     * @apiNote 使用示例：
     * <pre>{@code
     * GET /chat/history/123?page=1&size=20&messageType=1
     * }</pre>
     */
    @GetMapping("/history/{auctionId}")
    public Result<ChatHistoryResponse> getChatHistory(
            @PathVariable Long auctionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer messageType) {
        try {
            ChatHistoryResponse response = chatService.getChatHistory(auctionId, page, size, messageType);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("查询聊天历史失败: auctionId={}", auctionId, e);
            return Result.fail(500, "查询聊天历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取在线用户列表
     * <p>
     * 查询指定拍卖活动的当前在线用户信息
     *
     * @param auctionId 拍卖活动ID
     * @return 在线用户响应
     * @apiNote 使用示例：
     * <pre>{@code
     * GET /chat/online-users/123
     * }</pre>
     */
    @GetMapping("/online-users/{auctionId}")
    public Result<OnlineUsersResponse> getOnlineUsers(@PathVariable Long auctionId) {
        try {
            OnlineUsersResponse response = chatService.getOnlineUsers(auctionId);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("获取在线用户失败: auctionId={}", auctionId, e);
            return Result.fail(500, "获取在线用户失败: " + e.getMessage());
        }
    }

    /**
     * 获取在线人数
     * <p>
     * 快速查询指定拍卖活动的在线用户数量
     *
     * @param auctionId 拍卖活动ID
     * @return 在线人数
     * @apiNote 使用示例：
     * <pre>{@code
     * GET /chat/online-count/123
     * }</pre>
     */
    @GetMapping("/online-count/{auctionId}")
    public Result<Integer> getOnlineCount(@PathVariable Long auctionId) {
        try {
            int count = chatService.getOnlineCount(auctionId);
            return Result.ok(count);
        } catch (Exception e) {
            log.error("获取在线人数失败: auctionId={}", auctionId, e);
            return Result.fail(500, "获取在线人数失败: " + e.getMessage());
        }
    }

    /**
     * 删除聊天消息
     * <p>
     * 软删除指定的聊天消息，只有消息发送者可以删除自己的消息
     *
     * @param messageId   消息ID
     * @param currentUser 当前登录用户
     * @return 是否删除成功
     * @apiNote 使用示例：
     * <pre>{@code
     * DELETE /chat/messages/456
     * }</pre>
     */
    @DeleteMapping("/messages/{messageId}")
    public Result<Boolean> deleteMessage(
            @PathVariable Long messageId,
            @CurrentUser UserPrincipal currentUser) {
        try {
            boolean deleted = chatService.deleteMessage(messageId, currentUser.getUserId());
            return Result.ok(deleted);
        } catch (Exception e) {
            log.error("删除聊天消息失败: messageId={}, userId={}",
                    messageId, currentUser.getUserId(), e);
            return Result.fail(500, "删除消息失败: " + e.getMessage());
        }
    }
}
