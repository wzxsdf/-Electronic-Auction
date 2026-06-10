package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 拍品实时价格响应DTO
 * 返回拍品的当前价格和竞价信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionItemPriceResponse {

    /**
     * 拍品ID
     */
    private Long auctionItemId;

    /**
     * 活动ID
     */
    private Long auctionId;

    /**
     * 当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 最高出价者ID
     */
    private Long highestBidder;

    /**
     * 出价次数
     */
    private Integer bidCount;

    /**
     * 状态
     */
    private String status;

    /**
     * 结束时间（ISO 8601格式）
     * 前端应基于此绝对时间计算剩余时间，而非使用remainingSeconds
     */
    private String endTime;

    /**
     * 结束时间戳（毫秒，兼容性字段）
     * 推荐使用endTime字段，此字段仅为老旧客户端兼容
     */
    private Long endTimeTimestamp;

    /**
     * 剩余秒数（已废弃，请使用endTime计算）
     * @deprecated 使用 endTime 字段代替
     */
    @Deprecated
    private Long remainingSeconds;

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
     * 是否可出价
     */
    private Boolean isBiddable;
}
