package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 出价结果响应DTO
 * 用于返回用户出价后的即时反馈信息
 */
@Data
@Builder
@AllArgsConstructor
public class BidResultResponse {

    /**
     * 出价记录ID
     */
    private Long bidId;

    /**
     * 出价后的当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 当前用户排名
     */
    private Integer yourRank;

    /**
     * 是否领先（当前最高出价者）
     */
    private Boolean isLeading;

    /**
     * 距离结束剩余毫秒数
     */
    private Long remainingMs;

    /**
     * 拍卖是否被延长
     */
    private Boolean wasExtended;

    /**
     * 新的结束时间戳（秒）
     */
    private Integer newEndTime;

    /**
     * 提示消息
     */
    private String message;
}
