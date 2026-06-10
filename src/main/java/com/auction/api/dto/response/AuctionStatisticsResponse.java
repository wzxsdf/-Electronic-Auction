package com.auction.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖活动统计响应DTO（重构后）
 * <p>
 * 提供活动级别的统计数据和趋势分析
 */
@Data
@Builder
public class AuctionStatisticsResponse {

    /**
     * 活动ID
     */
    private Long auctionId;

    /**
     * 活动标题
     */
    private String title;

    /**
     * 统计时间
     */
    private LocalDateTime statisticsTime;

    /**
     * 拍品统计
     */
    private ItemStatistics itemStats;

    /**
     * 出价统计
     */
    private BidStatistics bidStats;

    /**
     * 参与者统计
     */
    private ParticipantStatistics participantStats;

    /**
     * 时间统计
     */
    private TimeStatistics timeStats;

    /**
     * 拍品统计
     */
    @Data
    @Builder
    public static class ItemStatistics {
        /**
         * 总拍品数
         */
        private Integer totalItems;

        /**
         * 进行中拍品数
         */
        private Integer activeItems;

        /**
         * 已成交拍品数
         */
        private Integer soldItems;

        /**
         * 流拍拍品数
         */
        private Integer unsoldItems;

        /**
         * 成交率
         */
        private BigDecimal successRate;
    }

    /**
     * 出价统计
     */
    @Data
    @Builder
    public static class BidStatistics {
        /**
         * 总出价次数
         */
        private Long totalBids;

        /**
         * 平均每次出价金额
         */
        private BigDecimal averageBidAmount;

        /**
         * 最高出价金额
         */
        private BigDecimal highestBidAmount;

        /**
         * 最低出价金额
         */
        private BigDecimal lowestBidAmount;

        /**
         * 平均每拍品出价次数
         */
        private Double averageBidsPerItem;
    }

    /**
     * 参与者统计
     */
    @Data
    @Builder
    public static class ParticipantStatistics {
        /**
         * 总参与人数
         */
        private Integer totalParticipants;

        /**
         * 当前在线人数
         */
        private Integer onlineParticipants;

        /**
         * 出价用户数
         */
        private Integer biddingUsers;

        /**
         * 观看用户数
         */
        private Integer watchingUsers;
    }

    /**
     * 时间统计
     */
    @Data
    @Builder
    public static class TimeStatistics {
        /**
         * 活动开始时间
         */
        private LocalDateTime startTime;

        /**
         * 活动结束时间
         */
        private LocalDateTime endTime;

        /**
         * 已进行时长（秒）
         */
        private Long elapsedSeconds;

        /**
         * 剩余时长（秒）
         */
        private Long remainingSeconds;

        /**
         * 是否已结束
         */
        private Boolean isEnded;

        /**
         * 延时次数
         */
        private Integer delayCount;
    }
}
