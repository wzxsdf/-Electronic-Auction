package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 拍卖活动关注实体
 * <p>
 * 记录用户对拍卖活动的关注关系
 * 支持活动状态变更时向关注者推送通知
 */
@Data
@TableName("auction_follows")
public class AuctionFollow {
    /**
     * 关注记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 拍卖活动ID
     */
    private Long auctionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 关注状态：ACTIVE-有效, CANCELLED-已取消
     */
    private String status;

    /**
     * 关注时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 获取关注状态枚举
     */
    public FollowStatus getStatusEnum() {
        return status != null ? FollowStatus.valueOf(status) : null;
    }

    /**
     * 设置关注状态枚举
     */
    public void setStatusEnum(FollowStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.name() : null;
    }

    /**
     * 关注状态枚举
     */
    public enum FollowStatus {
        /**
         * 有效关注
         */
        ACTIVE,

        /**
         * 已取消
         */
        CANCELLED
    }
}