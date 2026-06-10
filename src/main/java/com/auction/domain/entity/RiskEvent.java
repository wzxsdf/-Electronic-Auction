package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 风险事件实体
 * <p>
 * 记录系统中检测到的风险事件，用于安全监控和异常行为分析
 * 支持自动风险检测和人工审核处理
 *
 * 重构说明：
 * - roomId改为auctionId
 * - itemId改为auctionItemId
 */
@Data
@TableName("risk_events")
public class RiskEvent {
    /**
     * 风险事件ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 涉事用户ID
     */
    private Long userId;

    /**
     * 涉事拍卖活动ID（重构：替代roomId）
     */
    private Long auctionId;

    /**
     * 涉事竞拍项目ID（重构：替代itemId）
     */
    private Long auctionItemId;

    /**
     * 事件类型（如FREQUENT_BID频繁出价、ABNORMAL_BEHAVIOR异常行为等）
     */
    private String eventType;

    /**
     * 严重程度（LOW低、MEDIUM中、HIGH高、CRITICAL严重）
     */
    private String severity;

    /**
     * 事件描述
     */
    private String description;

    /**
     * 元数据（JSON格式，存储事件的详细信息）
     */
    private String metadata;

    /**
     * 采取的处理措施（如WARNING警告、BLOCKED封禁等）
     */
    private String actionTaken;

    /**
     * 创建时间（事件检测时间）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
