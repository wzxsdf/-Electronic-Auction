package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 当前活跃拍卖响应DTO
 * <p>
 * 返回所有正在进行的拍卖活动及拍品信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveAuctionsResponse {

    /**
     * 活跃的拍卖活动列表
     */
    private List<AuctionInfo> auctions;

    /**
     * 拍卖活动信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionInfo {

        /**
         * 活动ID
         */
        private Long auctionId;

        /**
         * 活动标题
         */
        private String auctionTitle;

        /**
         * 活动描述
         */
        private String auctionDescription;

        /**
         * 正在进行的拍品列表
         */
        private List<ActiveItemInfo> items;
    }

    /**
     * 正在进行的拍品信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveItemInfo {

        /**
         * 拍品ID
         */
        private Long itemId;

        /**
         * 拍品标题
         */
        private String title;

        /**
         * 商品名称
         */
        private String productName;

        /**
         * 商品图片URL
         */
        private String productImageUrl;

        /**
         * 商品描述
         */
        private String description;

        /**
         * 当前价格
         */
        private BigDecimal currentPrice;

        /**
         * 起拍价
         */
        private BigDecimal startPrice;

        /**
         * 加价幅度
         */
        private BigDecimal bidIncrement;

        /**
         * 封顶价
         */
        private BigDecimal maxPrice;

        /**
         * 当前最高出价者ID
         */
        private Long highestBidder;

        /**
         * 出价次数
         */
        private Integer bidCount;

        /**
         * 结束时间（ISO 8601格式）
         */
        private String endTime;

        /**
         * 结束时间戳（毫秒）
         */
        private Long endTimeTimestamp;

        /**
         * 剩余秒数（已废弃，保留兼容性）
         */
        @Deprecated
        private Long remainingSeconds;

        /**
         * 是否可出价
         */
        private Boolean isBiddable;

        /**
         * 延时次数
         */
        private Integer delayCount;
    }
}
