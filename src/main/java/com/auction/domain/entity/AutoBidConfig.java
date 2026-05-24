package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("auto_bid_configs")
public class AutoBidConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long itemId;
    private Long auctionId;
    private BigDecimal maxPrice;
    private String strategy;
    private String status;
    private Integer bidCount;
    private BigDecimal currentBid;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
