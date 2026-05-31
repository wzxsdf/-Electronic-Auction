package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 出价统计信息响应DTO
 * 用于返回竞拍的出价统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidStatisticsResponse {

    /**
     * 总出价次数
     */
    private Long totalBids;

    /**
     * 参与人数（去重后的用户数量）
     */
    private Long participantCount;

    /**
     * 自动出价次数
     */
    private Long autoBidCount;

    /**
     * 当前最高价格
     */
    private java.math.BigDecimal currentHighestPrice;

    /**
     * 当前最低价格
     */
    private java.math.BigDecimal currentLowestPrice;

    /**
     * 平均出价金额
     */
    private java.math.BigDecimal averagePrice;
}