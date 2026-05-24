package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bids")
public class Bid {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;
    private Long auctionId;
    private Long userId;
    private BigDecimal amount;
    private Integer rankWhenBid;
    private String status;
    private Boolean isAutoBid;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String username;
}
