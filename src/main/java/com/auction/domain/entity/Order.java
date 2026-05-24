package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;
    private Long itemId;
    private Long userId;
    private Long productId;
    private BigDecimal finalAmount;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public OrderStatus getStatusEnum() {
        return status != null ? OrderStatus.valueOf(status) : null;
    }

    public void setStatusEnum(OrderStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
}
