package com.auction.api.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拍卖活动详情响应DTO（重构后）
 * <p>
 * 包含活动基本信息和所有拍品列表
 * 支持实时统计数据展示
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuctionDetailResponse extends AuctionResponse {

    /**
     * 活动描述
     */
    private String description;

    /**
     * 创建者ID
     */
    private Long hostId;

    /**
     * 最低保证金要求
     */
    private BigDecimal minDeposit;

    /**
     * 最大拍品数量
     */
    private Integer maxItems;

    /**
     * 当前在线观看人数
     */
    private Integer viewerCount;

    /**
     * 拍品列表
     */
    private List<AuctionItemResponse> items;

    /**
     * 活动统计数据
     */
    private AuctionStatistics statistics;

    /**
     * 拍品详情响应DTO
     */
    @Data
    public static class AuctionItemResponse {
        /**
         * 拍品ID
         */
        private Long id;

        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 拍品标题
         */
        private String title;

        /**
         * 商品名称（来自关联商品）
         */
        private String productName;

        /**
         * 商品图片URL（来自关联商品）
         */
        private String productImageUrl;

        /**
         * 商品描述（来自关联商品）
         */
        private String productDescription;

        /**
         * 起拍价格
         */
        private BigDecimal startPrice;

        /**
         * 加价幅度
         */
        private BigDecimal bidIncrement;

        /**
         * 封顶价格
         */
        private BigDecimal maxPrice;

        /**
         * 延时秒数
         */
        private Integer delaySeconds;

        /**
         * 开始时间
         */
        private LocalDateTime startTime;

        /**
         * 结束时间
         */
        private LocalDateTime endTime;

        /**
         * 原始结束时间
         */
        private LocalDateTime originalEndTime;

        /**
         * 当前价格
         */
        private BigDecimal currentPrice;

        /**
         * 当前最高出价者ID
         */
        private Long highestBidder;

        /**
         * 拍品状态
         */
        private String status;

        /**
         * 出价次数
         */
        private Integer bidCount;

        /**
         * 显示顺序
         */
        private Integer displayOrder;

        /**
         * 延时次数
         */
        private Integer delayCount;

        /**
         * 是否可出价
         */
        private Boolean isBiddable;

        /**
         * 剩余时间（秒）
         */
        private Long remainingSeconds;
    }

    /**
     * 活动统计数据
     */
    @Data
    public static class AuctionStatistics {
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
         * 总出价次数
         */
        private Long totalBids;

        /**
         * 参与人数
         */
        private Integer participantCount;

        /**
         * 总成交金额
         */
        private BigDecimal totalAmount;

        /**
         * 平均出价金额
         */
        private BigDecimal averageAmount;
    }
}
