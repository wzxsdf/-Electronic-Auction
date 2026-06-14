package com.auction.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出价事件消息
 * <p>
 * 发送到 MQ，由消费者异步处理：
 * 1. 持久化出价记录到数据库
 * 2. 推送 WebSocket 实时消息
 * 3. 触发订单生成
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidEvent {

    /**
     * 事件ID（唯一标识）
     */
    private String eventId;

    /**
     * 拍品ID
     */
    private Long auctionItemId;

    /**
     * 活动ID
     */
    private Long auctionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称（用于广播显示）
     */
    private String username;

    /**
     * 出价金额
     */
    private BigDecimal amount;

    /**
     * 出价时间
     */
    private LocalDateTime bidTime;

    /**
     * 是否自动出价
     */
    private Boolean isAutoBid;

    /**
     * 出价后的当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 出价后的最高出价者ID
     */
    private Long highestBidder;

    /**
     * 出价后的结束时间（可能因延时变化）
     */
    private LocalDateTime endTime;

    /**
     * 延时次数
     */
    private Integer delayCount;

    /**
     * 出价次数统计
     */
    private Integer bidCount;

    /**
     * 是否达到封顶价成交
     */
    private Boolean maxPriceReached;

    /**
     * 事件创建时间戳
     */
    private Long timestamp;

    /**
     * 优先级（1-10，数字越大优先级越高）
     */
    private Integer priority;

    /**
     * 创建出价事件（完整参数）
     */
    public static BidEvent create(
            Long auctionItemId,
            Long auctionId,
            Long userId,
            String username,
            BigDecimal amount,
            Boolean isAutoBid,
            BigDecimal currentPrice,
            Long highestBidder,
            LocalDateTime endTime,
            Integer delayCount,
            Integer bidCount,
            Boolean maxPriceReached
    ) {
        return BidEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .auctionItemId(auctionItemId)
                .auctionId(auctionId)
                .userId(userId)
                .username(username)
                .amount(amount)
                .bidTime(LocalDateTime.now())
                .isAutoBid(isAutoBid)
                .currentPrice(currentPrice)
                .highestBidder(highestBidder)
                .endTime(endTime)
                .delayCount(delayCount)
                .bidCount(bidCount)
                .maxPriceReached(maxPriceReached)
                .timestamp(System.currentTimeMillis())
                .priority(isAutoBid ? 5 : 10)  // 人工出价优先级高于自动出价
                .build();
    }
}
