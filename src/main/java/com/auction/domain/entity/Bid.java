package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出价记录实体
 * <p>
 * 记录用户在竞拍中的每一次出价信息，包括出价金额、时间、排名等关键数据
 * 用于竞拍结算、历史记录查询和用户行为分析
 */
@Data
@TableName("bids")
public class Bid {
    /**
     * 出价记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 拍品ID（重构后：auction_item_id）
     */
    private Long auctionItemId;

    /**
     * 竞拍ID
     */
    private Long auctionId;

    /**
     * 出价用户ID
     */
    private Long userId;

    /**
     * 出价金额
     */
    private BigDecimal amount;

    /**
     * 出价时的排名
     */
    private Integer rankWhenBid;

    /**
     * 出价状态（ACTIVE=有效，CANCELLED=已取消）
     */
    private String status;

    /**
     * 是否为自动出价
     */
    private Boolean isAutoBid;

    /**
     * 出价时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 出价用户名（不持久化，用于显示）
     */
    @TableField(exist = false)
    private String username;
}
