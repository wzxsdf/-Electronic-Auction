package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞拍活动实体（拍卖会）
 * <p>
 * 存储拍卖活动的核心信息，一个活动可包含多个拍品（AuctionItem）
 * 支持活动级别的状态管理、时间控制和保证金设置
 *
 * 重构说明：
 * - 删除productId字段，不再直接关联商品
 * - 添加hostId字段，记录活动创建者
 * - 添加description字段，活动描述信息
 * - 添加minDeposit字段，最低保证金要求
 * - 添加maxItems字段，最大拍品数量限制
 * - 添加viewerCount字段，在线观看人数
 */
@Data
@TableName("auctions")
public class Auction {
    /**
     * 竞拍活动ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 乐观锁版本号 - 防止并发修改冲突
     */
    @Version
    private Integer version;

    /**
     * 活动标题
     */
    private String title;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 创建者ID（商家或管理员）
     */
    private Long hostId;

    /**
     * 活动状态
     */
    private String status;

    /**
     * 活动开始时间
     */
    private LocalDateTime startTime;

    /**
     * 活动结束时间（可延长）
     */
    private LocalDateTime endTime;

    /**
     * 最低保证金要求
     */
    private BigDecimal minDeposit;

    /**
     * 最大拍品数量限制
     */
    private Integer maxItems;

    /**
     * 当前在线观看人数
     */
    private Integer viewerCount;

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

    // ==================== 非持久化字段 ====================

    /**
     * 活动包含的拍品列表（不持久化）
     */
    @TableField(exist = false)
    private List<AuctionItem> items;

    /**
     * 拍品总数（不持久化）
     */
    @TableField(exist = false)
    private Integer totalItems;

    /**
     * 成交拍品数（不持久化）
     */
    @TableField(exist = false)
    private Integer soldItems;

    /**
     * 总成交金额（不持久化）
     */
    @TableField(exist = false)
    private BigDecimal totalAmount;

    /**
     * 总参与人数（不持久化）
     */
    @TableField(exist = false)
    private Integer totalParticipants;

    /**
     * 延时次数计数（不持久化）
     */
    @TableField(exist = false)
    private Integer delayCount = 0;

    /**
     * 延时秒数（不持久化，从拍品中获取）
     */
    @TableField(exist = false)
    private Integer delaySeconds = 15;

    // ==================== 枚举转换方法 ====================

    /**
     * 获取活动状态枚举
     */
    public com.auction.domain.enums.AuctionStatus getStatusEnum() {
        return status != null ? com.auction.domain.enums.AuctionStatus.valueOf(status) : null;
    }

    /**
     * 设置活动状态枚举
     */
    public void setStatusEnum(com.auction.domain.enums.AuctionStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
}
