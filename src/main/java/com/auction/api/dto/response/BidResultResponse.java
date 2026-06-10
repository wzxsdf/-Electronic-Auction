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
     * 结束时间（ISO 8601格式）
     * 前端应基于此绝对时间计算剩余时间
     */
    private String endTime;

    /**
     * 结束时间戳（毫秒）
     * 推荐使用 endTime 字段
     */
    private Long endTimeTimestamp;

    /**
     * 距离结束剩余毫秒数（已废弃）
     * @deprecated 使用 endTime 字段代替
     */
    @Deprecated
    private Long remainingMs;

    /**
     * 拍卖是否被延长
     */
    private Boolean wasExtended;

    /**
     * 提示消息
     */
    private String message;
}
