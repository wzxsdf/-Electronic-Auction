package com.auction.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 拍卖活动开始事件
 * <p>
 * 当拍卖活动开始时发布此事件，触发：
 * 1. 设置延时通知任务（活动即将结束提醒）
 * 2. 其他业务逻辑（如统计、日志等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionStartedEvent {

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
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 操作者ID（启动活动的人）
     */
    private Long operatorId;

    /**
     * 事件创建时间
     */
    private LocalDateTime eventTime;

    /**
     * 创建拍卖活动开始事件
     */
    public static AuctionStartedEvent create(
            Long auctionId,
            String auctionTitle,
            Long merchantId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long operatorId
    ) {
        return AuctionStartedEvent.builder()
                .auctionId(auctionId)
                .auctionTitle(auctionTitle)
                .merchantId(merchantId)
                .startTime(startTime)
                .endTime(endTime)
                .operatorId(operatorId)
                .eventTime(LocalDateTime.now())
                .build();
    }
}