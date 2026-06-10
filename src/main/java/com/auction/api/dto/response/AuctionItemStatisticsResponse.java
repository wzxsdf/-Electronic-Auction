package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拍品统计响应DTO
 * 返回拍品的详细统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionItemStatisticsResponse {

    /**
     * 拍品ID
     */
    private Long auctionItemId;

    /**
     * 活动ID
     */
    private Long auctionId;

    /**
     * 总出价次数
     */
    private Long totalBids;

    /**
     * 参与人数
     */
    private Long participantCount;

    /**
     * 自动出价数量
     */
    private Long autoBidCount;

    /**
     * 起拍价
     */
    private BigDecimal startPrice;

    /**
     * 当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 最高价
     */
    private BigDecimal highestPrice;

    /**
     * 最低价
     */
    private BigDecimal lowestPrice;

    /**
     * 平均价
     */
    private BigDecimal averagePrice;

    /**
     * 价格走势（最近10次出价）
     */
    private List<BigDecimal> priceTrend;

    /**
     * 出价次数
     */
    private Integer bidCount;

    /**
     * 状态
     */
    private String status;
}
