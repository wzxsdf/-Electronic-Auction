package com.auction.domain.entity;

import com.auction.domain.enums.AuctionStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 竞拍项目实体（拍品）
 * <p>
 * 存储竞拍活动的具体拍品信息，属于某个拍卖活动（Auction）
 * 每个拍品关联一个商品（Product），拥有独立的价格和时间设置
 *
 * 重构说明：
 * - roomId改为auctionId，关联到拍卖活动
 * - 添加start_time和end_time的可选设置
 * - 支持拍品级别的时间覆盖
 */
@Data
@TableName("auction_items")
public class AuctionItem {
    /**
     * 竞拍项目ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 乐观锁版本号 - 防止并发修改冲突
     */
    @Version
    private Integer version;

    /**
     * 所属拍卖活动ID（重构：替代roomId）
     */
    private Long auctionId;

    /**
     * 关联商品ID
     */
    private Long productId;

    /**
     * 竞拍项目标题
     */
    private String title;

    /**
     * 起拍价格
     */
    private BigDecimal startPrice;

    /**
     * 出价递增幅度
     */
    private BigDecimal bidIncrement;

    /**
     * 封顶价格（可选）
     */
    private BigDecimal maxPrice;

    /**
     * 延时秒数
     */
    private Integer delaySeconds;

    /**
     * 竞拍开始时间（可选，覆盖活动时间）
     */
    private LocalDateTime startTime;

    /**
     * 竞拍结束时间（可选，可延长）
     */
    private LocalDateTime endTime;

    /**
     * 原始结束时间（不因延时而变化）
     */
    private LocalDateTime originalEndTime;

    /**
     * 当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 当前最高出价者ID
     */
    private Long highestBidder;

    /**
     * 竞拍状态
     */
    private String status;

    /**
     * 出价次数统计
     */
    private Integer bidCount;

    /**
     * 显示顺序（在活动内的竞拍顺序）
     */
    private Integer displayOrder;

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
     * 关联商品名称（不持久化）
     */
    @TableField(exist = false)
    private String productName;

    /**
     * 关联商品图片URL（不持久化）
     */
    @TableField(exist = false)
    private String productImageUrl;

    /**
     * 商品描述（不持久化）
     */
    @TableField(exist = false)
    private String description;

    /**
     * 所属活动标题（不持久化）
     */
    @TableField(exist = false)
    private String auctionTitle;

    /**
     * 延时次数计数（持久化）
     */
    private Integer delayCount = 0;

    // ==================== 枚举转换方法 ====================

    /**
     * 获取竞拍状态枚举
     */
    public AuctionStatus getStatusEnum() {
        return status != null ? AuctionStatus.valueOf(status) : null;
    }

    /**
     * 设置竞拍状态枚举
     */
    public void setStatusEnum(AuctionStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
}
