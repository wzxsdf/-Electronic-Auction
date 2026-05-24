package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("auction_rooms")
public class AuctionRoom {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String description;
    private Long hostId;
    private String status;
    private Integer viewerCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public RoomStatus getStatusEnum() {
        return status != null ? RoomStatus.valueOf(status) : null;
    }

    public void setStatusEnum(RoomStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }

    public enum RoomStatus {
        PENDING,
        LIVE,
        ENDED
    }
}
