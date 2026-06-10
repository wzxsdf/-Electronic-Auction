package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 * <p>
 * 存储竞拍成交后生成的订单信息，用于后续的支付和履约流程
 * 记录成交价格、买方信息以及关联的活动、拍品和商品信息
 *
 * 重构说明：
 * - 删除roomId字段
 * - 添加auctionId字段，关联拍卖活动
 * - itemId改为auctionItemId，明确关联到拍品
 * - 添加depositAmount字段，保证金抵扣金额
 * - 添加payableAmount字段，应付金额
 */
@Data
@TableName("orders")
public class Order {
    /**
     * 订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 拍卖活动ID
     */
    private Long auctionId;

    /**
     * 竞拍项目ID（拍品）
     */
    private Long auctionItemId;

    /**
     * 用户ID（竞拍获胜者）
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 最终成交金额
     */
    private BigDecimal finalAmount;

    /**
     * 保证金抵扣金额
     */
    private BigDecimal depositAmount;

    /**
     * 应付金额（成交价 - 保证金）
     */
    private BigDecimal payableAmount;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 创建时间（订单生成时间）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 关联信息字段（不持久化） ====================

    /**
     * 活动标题（关联查询）
     */
    @TableField(exist = false)
    private String auctionTitle;

    /**
     * 拍品标题（关联查询）
     */
    @TableField(exist = false)
    private String itemTitle;

    /**
     * 商品名称（关联查询）
     */
    @TableField(exist = false)
    private String productName;

    /**
     * 商品图片URL（关联查询）
     */
    @TableField(exist = false)
    private String productImageUrl;

    /**
     * 商品描述（关联查询）
     */
    @TableField(exist = false)
    private String productDescription;

    // ==================== 枚举转换方法 ====================

    /**
     * 获取订单状态枚举
     */
    public OrderStatus getStatusEnum() {
        return status != null ? OrderStatus.valueOf(status) : null;
    }

    /**
     * 设置订单状态枚举
     */
    public void setStatusEnum(OrderStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
}
