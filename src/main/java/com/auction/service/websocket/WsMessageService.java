package com.auction.service.websocket;

import com.auction.domain.entity.Auction;
import com.auction.domain.entity.Bid;
import com.auction.domain.enums.MessageType;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.AuctionRepository;
import com.auction.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息服务
 * <p>
 * 负责处理系统中所有WebSocket实时消息的发送和广播，提供异步、高性能的消息推送能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsMessageService {

    private final WsRoomManager roomManager;
    private final RedisService redisService;

    /**
     * 异步广播新出价消息到竞拍房间
     *
     * 向竞拍房间内所有在线用户广播新的出价信息，包括：
     * <ul>
     * <li>出价ID和出价用户信息</li>
     * <li>出价金额和排名</li>
     * <li>是否为自动出价</li>
     * <li>出价时间</li>
     * </ul>
     *
     * @param auction 竞拍信息对象
     * @param bid 出价记录对象
     * @param username 出价用户名（会进行脱敏处理）
     * @param rank 当前出价排名
     */
    @Async("websocketTaskExecutor")
    public void broadcastNewBid(Auction auction, Bid bid, String username, Integer rank) {
        try {
            Map<String, Object> bidData = Map.of(
                "bidId", bid.getId(),
                "auctionId", auction.getId(),
                "userId", bid.getUserId(),
                "username", maskUsername(username),
                "amount", bid.getAmount(),
                "rank", rank,
                "isAutoBid", bid.getIsAutoBid(),
                "bidTime", bid.getCreatedAt().toString()
            );

            // 注意：此方法用于Auction类型，已废弃
            // 请使用 broadcastNewBid(AuctionItem item, ...) 方法
            roomManager.broadcastToRoom(
                "item:" + auction.getId(),
                createMessage(MessageType.NEW_BID, bidData, auction.getId())
            );
        } catch (Exception e) {
            log.error("广播新出价消息失败: auctionId={}, bidId={}", auction.getId(), bid.getId(), e);
        }
    }

    /**
     * 异步广播价格更新消息到竞拍房间
     *
     * 从Redis中获取最新的竞拍状态信息并广播给房间内所有用户：
     * <ul>
     * <li>当前价格：实时最新出价</li>
     * <li>最高出价者ID：当前领先的用户</li>
     * <li>出价次数：总的出价次数统计</li>
     * </ul>
     *
     * 该方法通常在每次出价后调用，确保前端界面显示最新状态
     *
     * @param auctionId 竞拍ID
     */
    @Async("websocketTaskExecutor")
    public void broadcastPriceUpdate(Long auctionId) {
        try {
            String auctionKey = "auction:" + auctionId;
            Object priceObj = redisService.get(auctionKey + ":current_price");
            Object bidderObj = redisService.get(auctionKey + ":highest_bidder");
            Object countObj = redisService.get(auctionKey + ":bid_count");

            // 使用HashMap以支持null值
            Map<String, Object> data = new HashMap<>();
            data.put("currentPrice", priceObj != null ? new BigDecimal(priceObj.toString()) : BigDecimal.ZERO);
            data.put("highestBidder", bidderObj != null ? Long.parseLong(bidderObj.toString()) : null);
            data.put("bidCount", countObj != null ? Integer.parseInt(countObj.toString()) : 0);

            // 注意：此方法用于Auction类型，已废弃
            // 请使用 broadcastItemPriceUpdate(auctionItemId) 方法
            roomManager.broadcastToRoom(
                "item:" + auctionId,
                createMessage(MessageType.PRICE_UPDATE, data, auctionId)
            );
        } catch (Exception e) {
            log.error("广播价格更新失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送领先通知给指定用户
     *
     * 当用户出价后成为最高出价者时，发送此通知告知其当前处于领先地位
     *
     * @param userId 用户ID
     * @param auctionId 竞拍ID
     * @param amount 当前领先价格
     */
    @Async("websocketTaskExecutor")
    public void sendYouAreLeading(Long userId, Long auctionId, BigDecimal amount) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "amount", amount
            );

            roomManager.sendToUser(userId,
                createMessage(MessageType.YOU_ARE_LEADING, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送领先通知失败: userId={}, auctionId={}", userId, auctionId, e);
        }
    }

    /**
     * 异步发送被超越通知给指定用户
     *
     * 当用户从最高出价者位置被其他用户超越时，发送此通知告知其已被超越
     *
     * @param userId 被超越的用户ID
     * @param auctionId 竞拍ID
     * @param currentPrice 当前最新价格
     */
    @Async("websocketTaskExecutor")
    public void sendYouWereOvertaken(Long userId, Long auctionId, BigDecimal currentPrice) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "currentPrice", currentPrice
            );

            roomManager.sendToUser(userId,
                createMessage(MessageType.YOU_WERE_OVERTAKEN, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送被超越通知失败: userId={}, auctionId={}", userId, auctionId, e);
        }
    }

    /**
     * 异步发送未成交通知给指定用户
     *
     * 竞拍结束后，向非最高出价者发送未成交通知，告知其未能成功竞得商品
     *
     * @param userId 用户ID
     * @param auctionId 竞拍ID
     */
    @Async("websocketTaskExecutor")
    public void sendYouLost(Long userId, Long auctionId) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "message", "很遗憾，您未成交"
            );

            roomManager.sendToUser(userId,
                createMessage(MessageType.YOU_LOST, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送未成交通知失败: userId={}, auctionId={}", userId, auctionId, e);
        }
    }

    /**
     * 异步发送竞拍结束通知到竞拍房间
     *
     * 竞拍结束时向房间内所有用户广播结束信息，包括：
     * <ul>
     * <li>成交者ID（如果有人出价）</li>
     * <li>最终成交价格</li>
     * <li>是否有人出价（区分正常成交和流拍）</li>
     * </ul>
     *
     * @param auctionId 竞拍ID
     * @param winnerId 成交者ID，流拍时为null
     * @param finalPrice 最终成交价格
     * @param hasBids 是否有人出价（true=正常成交，false=流拍）
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionEnded(Long auctionId, Long winnerId, java.math.BigDecimal finalPrice, boolean hasBids) {
        try {
            // 使用HashMap以支持null值
            Map<String, Object> data = new HashMap<>();
            data.put("auctionId", auctionId);
            data.put("winnerId", winnerId);
            data.put("finalPrice", finalPrice);
            data.put("hasBids", hasBids);
            data.put("message", hasBids ? "竞拍已结束，恭喜成交！" : "竞拍已结束，无人出价");

            // 注意：此方法用于Auction类型，已废弃
            // 请使用 sendAuctionItemEnded(auctionItemId, ...) 方法
            roomManager.broadcastToRoom(
                "item:" + auctionId,
                createMessage(MessageType.AUCTION_ENDED, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送竞拍结束通知失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送竞拍开始通知到竞拍房间
     * <p>
     * 竞拍从待开始状态变更为活跃状态时，通知房间内所有用户竞拍已正式开始
     *
     * @param auctionId 竞拍ID
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionStarted(Long auctionId) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "message", "竞拍已开始，快来出价吧！"
            );

            // 注意：此方法用于Auction类型，已废弃
            // 请使用 sendAuctionItemStarted(auctionItemId) 方法
            roomManager.broadcastToRoom(
                "item:" + auctionId,
                createMessage(MessageType.AUCTION_STARTED, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送竞拍开始通知失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送活动开始通知给关注者
     * <p>
     * 当用户关注的拍卖活动开始时，向该用户发送个性化通知
     *
     * @param userId 用户ID
     * @param auctionId 活动ID
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionStartedToFollower(Long userId, Long auctionId) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "message", "您关注的拍卖活动已开始，快来参与吧！"
            );

            roomManager.sendToUser(userId,
                createMessage(MessageType.AUCTION_STARTED_FOLLOWER, data, auctionId)
            );

            log.debug("发送活动开始通知给关注者成功: userId={}, auctionId={}", userId, auctionId);
        } catch (Exception e) {
            log.error("发送活动开始通知给关注者失败: userId={}, auctionId={}", userId, auctionId, e);
        }
    }

    /**
     * 异步发送竞拍取消通知到竞拍房间
     *
     * 竞拍被取消时（由管理员操作或系统自动取消），通知房间内所有用户竞拍已取消
     *
     * @param auctionId 竞拍ID
     * @param reason 取消原因，如果为null则显示默认消息
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionCancelled(Long auctionId, String reason) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "reason", reason != null ? reason : "竞拍已取消",
                "message", "竞拍已取消"
            );

            // 注意：此方法用于Auction类型，已废弃
            // 请使用 sendAuctionItemCancelled(auctionItemId, reason) 方法
            roomManager.broadcastToRoom(
                "item:" + auctionId,
                createMessage(MessageType.AUCTION_CANCELLED, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送竞拍取消通知失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送竞拍延时通知到竞拍房间
     *
     * 当竞拍临近结束时有人出价，触发自动延时机制，通知所有用户竞拍时间已延长
     * 延时目的是为了给其他出价者更多反应时间，提高成交价格
     *
     * @param auctionId 竞拍ID
     * @param newEndTime 新的结束时间
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionDelayed(Long auctionId, java.time.LocalDateTime newEndTime) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "newEndTime", newEndTime.toString(),
                "message", "竞拍时间延长！继续出价吧！"
            );

            // 注意：此方法用于Auction类型，已废弃
            // 请使用 sendAuctionItemDelay(auctionItemId, newEndTime, delayCount) 方法
            roomManager.broadcastToRoom(
                "item:" + auctionId,
                createMessage(MessageType.AUCTION_DELAYED, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送竞拍延时通知失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送成交通知给获胜者
     * <p>
     * 竞拍结束后，向最高出价者（获胜者）发送成交通知，告知其成功竞得商品
     * 同时提醒用户及时进行支付操作
     *
     * @param userId 获胜用户ID
     * @param auctionId 竞拍ID
     * @param finalAmount 最终成交金额
     */
    @Async("websocketTaskExecutor")
    public void sendYouWon(Long userId, Long auctionId, java.math.BigDecimal finalAmount) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "finalAmount", finalAmount,
                "message", "恭喜您竞拍成功！请及时支付"
            );

            roomManager.sendToUser(userId,
                createMessage(MessageType.YOU_WON, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送成交通知失败: userId={}, auctionId={}", userId, auctionId, e);
        }
    }

    /**
     * 异步发送出价失败通知给指定用户
     * <p>
     * 当用户出价失败时（如价格过低、频率限制、竞拍状态异常等），发送此通知告知失败原因
     *
     * @param userId 出价失败的用户ID
     * @param auctionId 竞拍ID
     * @param reason 失败原因描述
     */
    @Async("websocketTaskExecutor")
    public void sendBidFailed(Long userId, Long auctionId, String reason) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "reason", reason,
                "message", "出价失败：" + reason
            );

            roomManager.sendToUser(userId,
                createMessage(MessageType.BID_FAILED, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送出价失败通知失败: userId={}, auctionId={}", userId, auctionId, e);
        }
    }

    /**
     * 异步发送支付成功通知给指定用户
     * <p>
     * 订单支付成功后，向用户发送支付成功通知，确认支付完成
     *
     * @param userId 支付用户ID
     * @param orderId 订单ID
     * @param amount 支付金额
     */
    @Async("websocketTaskExecutor")
    public void sendPaymentSuccess(Long userId, Long orderId, java.math.BigDecimal amount) {
        try {
            Map<String, Object> data = Map.of(
                "orderId", orderId,
                "amount", amount,
                "message", "支付成功！感谢您的购买"
            );

            roomManager.sendToUser(userId,
                createMessage(MessageType.PAYMENT_SUCCESS, data, null)
            );
        } catch (Exception e) {
            log.error("发送支付成功通知失败: userId={}, orderId={}", userId, orderId, e);
        }
    }

    /**
     * 异步发送支付取消通知给指定用户
     * <p>
     * 订单支付被取消时（用户主动取消或超时取消），向用户发送支付取消通知
     *
     * @param userId 支付用户ID
     * @param orderId 订单ID
     */
    @Async("websocketTaskExecutor")
    public void sendPaymentCancelled(Long userId, Long orderId) {
        try {
            Map<String, Object> data = Map.of(
                "orderId", orderId,
                "message", "支付已取消"
            );

            roomManager.sendToUser(userId,
                createMessage(MessageType.PAYMENT_CANCELLED, data, null)
            );
        } catch (Exception e) {
            log.error("发送支付取消通知失败: userId={}, orderId={}", userId, orderId, e);
        }
    }

    /**
     * 异步发送拍卖活动即将结束通知给商家
     * <p>
     * 当拍卖活动距离结束时间不足指定时间（如10分钟）时，向活动创建者（商家）发送即将结束通知
     *
     * @param merchantId 商家用户ID（活动创建者）
     * @param auctionId 拍卖活动ID
     * @param auctionTitle 拍卖活动标题
     * @param endTime 结束时间
     * @param remainingMinutes 剩余分钟数
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionEndingSoonToMerchant(Long merchantId, Long auctionId, String auctionTitle, LocalDateTime endTime, int remainingMinutes) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "auctionTitle", auctionTitle,
                "endTime", endTime.toString(),
                "remainingMinutes", remainingMinutes,
                "message", "您的拍卖活动即将结束，请及时关注竞价情况"
            );

            roomManager.sendToUser(merchantId,
                createMessage(MessageType.AUCTION_ENDING_SOON, data, auctionId)
            );

            log.info("发送拍卖即将结束通知成功: merchantId={}, auctionId={}, remainingMinutes={}",
                    merchantId, auctionId, remainingMinutes);
        } catch (Exception e) {
            log.error("发送拍卖即将结束通知失败: merchantId={}, auctionId={}", merchantId, auctionId, e);
        }
    }

    /**
     * 创建标准化的WebSocket消息格式
     * <p>
     * 构造符合系统规范的WebSocket消息结构，包含：
     * <ul>
     * <li>type：消息类型枚举值</li>
     * <li>data：消息具体数据内容</li>
     * <li>timestamp：消息时间戳</li>
     * <li>auctionId：关联的竞拍ID（如果适用）</li>
     * </ul>
     *
     * @param type 消息类型枚举
     * @param data 消息数据内容
     * @param auctionId 竞拍ID，某些消息可能为null
     * @return 标准化的消息Map对象
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
     * 隐藏用户名以保护用户隐私
     * <p>
     * 对用户名进行脱敏处理，保留前两位字符，其余部分用星号替换
     * 例如："张三丰" -> "张三***"
     *
     * @param username 原始用户名
     * @return 脱敏后的用户名，如果用户名为null或长度小于等于2则返回"***"
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return "***";
        }
        return username.substring(0, 2) + "***";
    }

    // ==================== 新增方法：支持AuctionItem ====================

    /**
     * 异步广播新出价消息到拍品房间
     */
    @Async("websocketTaskExecutor")
    public void broadcastNewBid(com.auction.domain.entity.AuctionItem item, Bid bid, String username, Integer rank) {
        try {
            Map<String, Object> bidData = Map.of(
                "bidId", bid.getId(),
                "auctionItemId", item.getId(),
                "auctionId", item.getAuctionId(),
                "userId", bid.getUserId(),
                "username", maskUsername(username),
                "amount", bid.getAmount(),
                "rank", rank,
                "isAutoBid", bid.getIsAutoBid(),
                "bidTime", bid.getCreatedAt().toString()
            );

            // 统一使用 "item:" + auctionItemId 格式
            roomManager.broadcastToRoom(
                "item:" + item.getId(),
                createMessage(MessageType.NEW_BID, bidData, item.getAuctionId())
            );
        } catch (Exception e) {
            log.error("广播拍品出价消息失败: auctionItemId={}, bidId={}", item.getId(), bid.getId(), e);
        }
    }

    /**
     * 异步广播拍品价格更新消息
     */
    @Async("websocketTaskExecutor")
    public void broadcastItemPriceUpdate(Long auctionItemId) {
        try {
            String itemKey = "auction:item:price:" + auctionItemId;
            String bidderKey = "auction:item:bidder:" + auctionItemId;

            Object priceObj = redisService.get(itemKey);
            Object bidderObj = redisService.get(bidderKey);

            Map<String, Object> data = Map.of(
                "auctionItemId", auctionItemId,
                "currentPrice", priceObj != null ? new java.math.BigDecimal(priceObj.toString()) : java.math.BigDecimal.ZERO,
                "highestBidder", bidderObj != null ? Long.parseLong(bidderObj.toString()) : null
            );

            // 统一使用 "item:" + auctionItemId 格式
            roomManager.broadcastToRoom(
                "item:" + auctionItemId,
                createMessage(MessageType.PRICE_UPDATE, data, auctionItemId)
            );
        } catch (Exception e) {
            log.error("广播拍品价格更新失败: auctionItemId={}", auctionItemId, e);
        }
    }

    /**
     * 异步发送拍品开始通知
     */
    /**
     * 发送拍品开始通知（兼容旧代码）
     * @deprecated 使用 sendAuctionItemStarted(auctionItemId, endTime) 代替
     */
    @Deprecated
    @Async("websocketTaskExecutor")
    public void sendAuctionItemStarted(Long auctionItemId) {
        // 旧版本调用：由于没有endTime信息，通知客户端查询最新数据
        sendAuctionItemStarted(auctionItemId, java.time.LocalDateTime.now().plusHours(1));
    }

    @Async("websocketTaskExecutor")
    public void sendAuctionItemStarted(Long auctionItemId, java.time.LocalDateTime endTime) {
        try {
            // 转换为时间戳（毫秒）
            Long endTimeTimestamp = endTime.atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            Map<String, Object> data = Map.of(
                "auctionItemId", auctionItemId,
                "endTime", endTime.toString(), // ISO 8601格式
                "endTimeTimestamp", endTimeTimestamp, // 时间戳（毫秒）
                "message", "拍品竞拍已开始"
            );

            // 统一使用 "item:" + auctionItemId 格式
            roomManager.broadcastToRoom(
                "item:" + auctionItemId,
                createMessage(MessageType.AUCTION_STARTED, data, auctionItemId)
            );
        } catch (Exception e) {
            log.error("发送拍品开始通知失败: auctionItemId={}", auctionItemId, e);
        }
    }

    /**
     * 异步发送拍品结束通知
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionItemEnded(Long auctionItemId, Long winnerId, java.math.BigDecimal finalPrice, String winnerUsername, boolean hasBids) {
        try {
            // 使用HashMap以支持null值（Map.of不允许null）
            Map<String, Object> data = new HashMap<>();
            data.put("auctionItemId", auctionItemId);
            data.put("winnerId", winnerId);
            data.put("winnerUsername", winnerUsername != null ? maskUsername(winnerUsername) : null);
            data.put("finalPrice", finalPrice);
            data.put("hasBids", hasBids);
            data.put("message", hasBids ? ("拍品已成交，中标用户：" + (winnerUsername != null ? maskUsername(winnerUsername) : "未知")) : "拍品流拍");

            // 统一使用 "item:" + auctionItemId 格式
            roomManager.broadcastToRoom(
                "item:" + auctionItemId,
                createMessage(MessageType.AUCTION_ENDED, data, auctionItemId)
            );
        } catch (Exception e) {
            log.error("发送拍品结束通知失败: auctionItemId={}", auctionItemId, e);
        }
    }

    /**
     * 异步发送拍品延时通知
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionItemDelay(Long auctionItemId, java.time.LocalDateTime newEndTime, Integer delayCount) {
        try {
            // 转换为时间戳（毫秒）
            Long endTimeTimestamp = newEndTime.atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            Map<String, Object> data = Map.of(
                "auctionItemId", auctionItemId,
                "newEndTime", newEndTime.toString(), // ISO 8601格式
                "newEndTimeTimestamp", endTimeTimestamp, // 时间戳（毫秒）
                "delayCount", delayCount,
                "message", "拍品竞拍时间延长！"
            );

            // 统一使用 "item:" + auctionItemId 格式
            roomManager.broadcastToRoom(
                "item:" + auctionItemId,
                createMessage(MessageType.AUCTION_DELAYED, data, auctionItemId)
            );
        } catch (Exception e) {
            log.error("发送拍品延时通知失败: auctionItemId={}", auctionItemId, e);
        }
    }

    /**
     * 异步发送封顶价成交通知
     * <p>
     * 当有出价达到封顶价时，通知所有用户该拍品已自动成交
     *
     * @param auctionItemId 拍品ID
     * @param finalPrice 最终成交价格
     * @param winnerId 成功者ID
     * @param winnerName 成功者用户名
     */
    @Async("websocketTaskExecutor")
    public void sendMaxPriceReached(Long auctionItemId, java.math.BigDecimal finalPrice,
                                   Long winnerId, String winnerName) {
        try {
            // 转换为时间戳（毫秒）
            Long endTimeTimestamp = java.time.LocalDateTime.now()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            Map<String, Object> data = Map.of(
                "auctionItemId", auctionItemId,
                "finalPrice", finalPrice,
                "winnerId", winnerId,
                "winnerName", maskUsername(winnerName),
                "endTimeTimestamp", endTimeTimestamp,
                "message", "🎉 达到封顶价，自动成交！"
            );

            // 统一使用 "item:" + auctionItemId 格式
            roomManager.broadcastToRoom(
                "item:" + auctionItemId,
                createMessage(MessageType.MAX_PRICE_REACHED, data, auctionItemId)
            );

            log.info("封顶价成交通知已发送: auctionItemId={}, winnerId={}, finalPrice={}",
                    auctionItemId, winnerId, finalPrice);
        } catch (Exception e) {
            log.error("发送封顶价成交通知失败: auctionItemId={}", auctionItemId, e);
        }
    }

    /**
     * 异步推送排行榜更新消息
     * <p>
     * 当排行榜发生变化时（新出价、排名更新等），向房间内所有用户推送最新的排行榜数据
     *
     * @param auctionItemId 拍品ID
     * @param rankingData 排行榜数据
     */
    @Async("websocketTaskExecutor")
    public void sendRankingUpdate(Long auctionItemId, java.util.Map<String, Object> rankingData) {
        try {
            Map<String, Object> data = Map.of(
                "auctionItemId", auctionItemId,
                "totalParticipants", rankingData.getOrDefault("totalParticipants", 0),
                "currentPrice", rankingData.getOrDefault("currentPrice", 0),
                "bidIncrement", rankingData.getOrDefault("bidIncrement", 0),
                "topRankings", rankingData.getOrDefault("topRankings", java.util.List.of()),
                "message", "排行榜已更新"
            );

            // 统一使用 "item:" + auctionItemId 格式
            roomManager.broadcastToRoom(
                "item:" + auctionItemId,
                createMessage(MessageType.LEADERBOARD_UPDATE, data, auctionItemId)
            );

            log.debug("排行榜更新已推送: auctionItemId={}", auctionItemId);
        } catch (Exception e) {
            log.error("推送排行榜更新失败: auctionItemId={}", auctionItemId, e);
        }
    }

    /**
     * 异步发送排行榜大幅变动通知
     * <p>
     * 当排行榜发生显著变化时（如领先者变更、价格突破等），发送重要通知
     *
     * @param auctionItemId 拍品ID
     * @param changeType 变化类型（LEADER_CHANGE=领先者变更，PRICE_BREAKTHROUGH=价格突破，MILESTONE=里程碑）
     * @param description 变化描述
     */
    @Async("websocketTaskExecutor")
    public void sendRankingMajorChange(Long auctionItemId, String changeType, String description) {
        try {
            Map<String, Object> data = Map.of(
                "auctionItemId", auctionItemId,
                "changeType", changeType,
                "description", description,
                "message", "排行榜重要变化",
                "timestamp", System.currentTimeMillis()
            );

            // 统一使用 "item:" + auctionItemId 格式
            roomManager.broadcastToRoom(
                "item:" + auctionItemId,
                createMessage(MessageType.LEADERBOARD_UPDATE, data, auctionItemId)
            );

            log.info("排行榜重要变化已推送: auctionItemId={}, changeType={}", auctionItemId, changeType);
        } catch (Exception e) {
            log.error("推送排行榜重要变化失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
        }
    }
}
