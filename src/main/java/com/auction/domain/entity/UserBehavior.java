package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户行为分析实体
 * <p>
 * 记录和分析用户在竞拍中的行为模式，用于风险评估和用户体验优化
 * 统计用户的出价频率、间隔时间等行为特征
 *
 * 重构说明：
 * - roomId改为auctionId
 * - itemId改为auctionItemId
 */
@Data
@TableName("user_behaviors")
public class UserBehavior {
    /**
     * 行为记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 拍卖活动ID（重构：替代roomId）
     */
    private Long auctionId;

    /**
     * 竞拍项目ID（重构：替代itemId）
     */
    private Long auctionItemId;

    /**
     * 出价次数统计
     */
    private Integer bidCount;

    /**
     * 平均出价间隔时间（秒）
     */
    private Integer avgBidInterval;

    /**
     * 最后出价时间
     */
    private LocalDateTime lastBidTime;

    /**
     * 风险评分（0-100，分数越高风险越大）
     */
    private BigDecimal riskScore;

    /**
     * 风险等级（SAFE安全、LOW_RISK低风险、MEDIUM_RISK中风险、HIGH_RISK高风险）
     */
    private String riskLevel;

    /**
     * 是否被封禁（true=已封禁，false=正常）
     */
    private Boolean isBlocked;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
