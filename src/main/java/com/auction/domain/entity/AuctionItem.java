package com.auction.domain.entity;

import com.auction.domain.enums.AuctionStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("auction_items")
public class AuctionItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 乐观锁版本号 - 防止并发修改冲突
     */
    @Version
    private Integer version;

    private Long roomId;
    private Long productId;
    private String title;
    private BigDecimal startPrice;
    private BigDecimal bidIncrement;
    private BigDecimal maxPrice;
    private Integer delaySeconds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime originalEndTime;
    private BigDecimal currentPrice;
    private Long highestBidder;
    private String status;
    private Integer bidCount;
    private Integer displayOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productImageUrl;

    @TableField(exist = false)
    private String description;

    @TableField(exist = false)
    private String roomTitle;

    public AuctionStatus getStatusEnum() {
        return status != null ? AuctionStatus.valueOf(status) : null;
    }

    public void setStatusEnum(AuctionStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
}
