package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 * 存储竞拍成交后生成的订单信息
 */
@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;      // 竞拍房间ID
    private Long itemId;      // 竞拍项目ID
    private Long userId;      // 用户ID（获胜者）
    private Long productId;   // 商品ID
    private BigDecimal finalAmount; // 成交金额

    private String status;    // 订单状态

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 以下字段为关联信息，不持久化到数据库
    @TableField(exist = false)
    private String auctionTitle;        // 竞拍标题

    @TableField(exist = false)
    private LocalDateTime auctionStartTime; // 竞拍开始时间

    @TableField(exist = false)
    private LocalDateTime auctionEndTime;   // 竞拍结束时间

    @TableField(exist = false)
    private String productName;       // 商品名称

    @TableField(exist = false)
    private String productImageUrl;   // 商品图片URL

    @TableField(exist = false)
    private String productDescription; // 商品描述

    public OrderStatus getStatusEnum() {
        return status != null ? OrderStatus.valueOf(status) : null;
    }

    public void setStatusEnum(OrderStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
}
