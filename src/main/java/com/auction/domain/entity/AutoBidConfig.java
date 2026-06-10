package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自动出价配置实体
 * <p>
 * 存储用户的自动出价策略和配置信息
 * 支持用户设置最高出价限制和自动出价行为
 */
@Data
@TableName("auto_bid_configs")
public class AutoBidConfig {
    /**
     * 配置ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 竞拍项目ID（拍品ID，重构后）
     */
    private Long auctionItemId;

    /**
     * 最高出价限制（用户愿意支付的最高价格）
     */
    private BigDecimal maxPrice;

    /**
     * 出价策略类型（如AGGRESSIVE激进型、CONSERVATIVE保守型等）
     */
    private String strategy;

    /**
     * 配置状态（ACTIVE=激活，PAUSED=暂停，CANCELLED=取消）
     */
    private String status;

    /**
     * 已出价次数统计
     */
    private Integer bidCount;

    /**
     * 当前出价金额
     */
    private BigDecimal currentBid;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
