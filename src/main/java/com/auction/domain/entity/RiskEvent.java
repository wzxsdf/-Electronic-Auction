package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("risk_events")
public class RiskEvent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long roomId;
    private Long itemId;
    private String eventType;
    private String severity;
    private String description;
    private String metadata;
    private String actionTaken;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
