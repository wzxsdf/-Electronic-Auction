package com.auction.api.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖详情响应DTO
 * 继承自AuctionResponse，用于返回拍卖完整详细信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuctionDetailResponse extends AuctionResponse {

    /**
     * 拍卖描述
     */
    private String description;

    /**
     * 延时拍卖时长（秒）
     */
    private Integer delaySeconds;

    /**
     * 原始结束时间
     */
    private LocalDateTime originalEndTime;

    /**
     * 参与人数
     */
    private Long participantCount;

    /**
     * 是否可延长（是否处于延时阶段）
     */
    private Boolean isExtendable;
}
