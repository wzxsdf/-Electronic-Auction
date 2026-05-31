package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出价历史响应DTO
 * 用于返回出价历史记录，包含用户脱敏信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidHistoryResponse {

    /**
     * 出价记录ID
     */
    private Long bidId;

    /**
     * 竞拍ID
     */
    private Long auctionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称（脱敏处理）
     */
    private String username;

    /**
     * 出价金额
     */
    private BigDecimal amount;

    /**
     * 是否为自动出价
     */
    private Boolean isAutoBid;

    /**
     * 出价时间
     */
    private LocalDateTime bidTime;

    /**
     * 出价排名（可选字段）
     */
    private Integer rank;
}