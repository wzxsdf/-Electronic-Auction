package com.auction.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 拍卖活动即将结束消息
 * <p>
 * 用于延时队列，在指定时间后通知商家活动即将结束
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionEndingSoonMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动ID
     */
    private Long auctionId;

    /**
     * 活动标题
     */
    private String auctionTitle;

    /**
     * 商家用户ID（活动创建者）
     */
    private Long merchantId;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 剩余分钟数（用于通知）
     */
    private Integer remainingMinutes;

    /**
     * 消息创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 消息计划执行时间
     */
    private LocalDateTime scheduledTime;

    /**
     * 创建即将结束消息
     */
    public static AuctionEndingSoonMessage create(
            Long auctionId,
            String auctionTitle,
            Long merchantId,
            LocalDateTime endTime,
            Integer remainingMinutes
    ) {
        LocalDateTime now = LocalDateTime.now();
        return AuctionEndingSoonMessage.builder()
                .auctionId(auctionId)
                .auctionTitle(auctionTitle)
                .merchantId(merchantId)
                .endTime(endTime)
                .remainingMinutes(remainingMinutes)
                .createdTime(now)
                .scheduledTime(now.plusMinutes(remainingMinutes))
                .build();
    }
}