package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 竞拍排行榜响应DTO
 * 用于展示拍品的实时竞拍排行榜
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidRankingResponse {

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 用户ID（可选，根据权限决定是否返回）
     */
    private Long userId;

    /**
     * 用户名（脱敏处理）
     */
    private String maskedUsername;

    /**
     * 最高出价金额
     */
    private BigDecimal highestBidAmount;

    /**
     * 出价次数
     */
    private Integer bidCount;

    /**
     * 最后出价时间
     */
    private LocalDateTime lastBidTime;

    /**
     * 是否为当前最高出价者
     */
    private Boolean isHighestBidder;

    /**
     * 自动出价是否启用
     */
    private Boolean hasAutoBidEnabled;
}
