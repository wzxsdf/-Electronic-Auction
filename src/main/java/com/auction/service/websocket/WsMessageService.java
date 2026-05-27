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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WsMessageService {

    private final WsRoomManager roomManager;
    private final RedisService redisService;

    /**
     * 异步广播新出价消息
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

            roomManager.broadcastToRoom(
                auction.getId().toString(),
                createMessage(MessageType.NEW_BID, bidData, auction.getId())
            );
        } catch (Exception e) {
            log.error("广播新出价消息失败: auctionId={}, bidId={}", auction.getId(), bid.getId(), e);
        }
    }

    /**
     * 异步广播价格更新
     */
    @Async("websocketTaskExecutor")
    public void broadcastPriceUpdate(Long auctionId) {
        try {
            String auctionKey = "auction:" + auctionId;
            Object priceObj = redisService.get(auctionKey + ":current_price");
            Object bidderObj = redisService.get(auctionKey + ":highest_bidder");
            Object countObj = redisService.get(auctionKey + ":bid_count");

            Map<String, Object> data = Map.of(
                "currentPrice", priceObj != null ? new BigDecimal(priceObj.toString()) : BigDecimal.ZERO,
                "highestBidder", bidderObj != null ? Long.parseLong(bidderObj.toString()) : null,
                "bidCount", countObj != null ? Integer.parseInt(countObj.toString()) : 0
            );

            roomManager.broadcastToRoom(
                auctionId.toString(),
                createMessage(MessageType.PRICE_UPDATE, data, auctionId)
            );
        } catch (Exception e) {
            log.error("广播价格更新失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送领先通知
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
     * 异步发送被超越通知
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
     * 异步发送未成交通知
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
     * 异步发送竞拍结束通知
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionEnded(Long auctionId, Long winnerId, java.math.BigDecimal finalPrice, boolean hasBids) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "winnerId", winnerId,
                "finalPrice", finalPrice,
                "hasBids", hasBids,
                "message", hasBids ? "竞拍已结束，恭喜成交！" : "竞拍已结束，无人出价"
            );

            roomManager.broadcastToRoom(
                auctionId.toString(),
                createMessage(MessageType.AUCTION_ENDED, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送竞拍结束通知失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送竞拍开始通知
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionStarted(Long auctionId) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "message", "竞拍已开始，快来出价吧！"
            );

            roomManager.broadcastToRoom(
                auctionId.toString(),
                createMessage(MessageType.AUCTION_STARTED, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送竞拍开始通知失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送竞拍取消通知
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionCancelled(Long auctionId, String reason) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "reason", reason != null ? reason : "竞拍已取消",
                "message", "竞拍已取消"
            );

            roomManager.broadcastToRoom(
                auctionId.toString(),
                createMessage(MessageType.AUCTION_CANCELLED, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送竞拍取消通知失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送竞拍延时通知
     */
    @Async("websocketTaskExecutor")
    public void sendAuctionDelayed(Long auctionId, java.time.LocalDateTime newEndTime) {
        try {
            Map<String, Object> data = Map.of(
                "auctionId", auctionId,
                "newEndTime", newEndTime.toString(),
                "message", "竞拍时间延长！继续出价吧！"
            );

            roomManager.broadcastToRoom(
                auctionId.toString(),
                createMessage(MessageType.AUCTION_DELAYED, data, auctionId)
            );
        } catch (Exception e) {
            log.error("发送竞拍延时通知失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 异步发送成交通知（获胜者）
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
     * 异步发送出价失败通知
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
     * 异步发送支付成功通知
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
     * 异步发送支付取消通知
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

    private Map<String, Object> createMessage(MessageType type, Object data, Long auctionId) {
        return Map.of(
            "type", type.name(),
            "data", data,
            "timestamp", System.currentTimeMillis(),
            "auctionId", auctionId
        );
    }

    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return "***";
        }
        return username.substring(0, 2) + "***";
    }
}
