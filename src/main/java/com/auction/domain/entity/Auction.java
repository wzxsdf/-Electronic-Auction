package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("auctions")
public class Auction {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 乐观锁版本号 - 防止并发修改冲突
     */
    @Version
    private Integer version;

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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 结算相关字段
    private Long winnerId;           // 获胜者ID
    private BigDecimal finalPrice;   // 最终成交价格
    private LocalDateTime settledAt;  // 结算时间
    private Long roomId;             // 竞拍房间ID

    // 延时相关字段
    @TableField(exist = false)
    private Integer delayCount = 0;  // 延时次数计数

    // 关联商品信息（不持久化）
    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productImageUrl;

    @TableField(exist = false)
    private String description;

    public com.auction.domain.enums.AuctionStatus getStatusEnum() {
        return status != null ? com.auction.domain.enums.AuctionStatus.valueOf(status) : null;
    }

    public void setStatusEnum(com.auction.domain.enums.AuctionStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
}
