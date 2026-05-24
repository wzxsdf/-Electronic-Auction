package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_behaviors")
public class UserBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long roomId;
    private Long itemId;
    private Integer bidCount;
    private Integer avgBidInterval;
    private LocalDateTime lastBidTime;
    private BigDecimal riskScore;
    private String riskLevel;
    private Boolean isBlocked;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
