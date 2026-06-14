package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 竞拍排行榜历史记录实体
 * <p>
 * 定期保存拍品的排行榜快照，用于历史数据分析和趋势预测
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bid_ranking_history")
public class BidRankingHistory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 拍品ID
     */
    private Long auctionItemId;

    /**
     * 拍卖活动ID
     */
    private Long auctionId;

    /**
     * 拍品标题（冗余字段，便于查询）
     */
    private String auctionItemTitle;

    /**
     * 排名
     */
    private Integer rankingPosition;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名（脱敏）
     */
    private String maskedUsername;

    /**
     * 最高出价金额
     */
    private BigDecimal highestBidAmount;

    /**
     * 出价次数
     */
    private Integer bidCount;

    /**
     * 最后出价时间
     */
    private LocalDateTime lastBidTime;

    /**
     * 是否为当前最高出价者
     */
    private Boolean isHighestBidder;

    /**
     * 当前拍品价格
     */
    private BigDecimal currentPrice;

    /**
     * 总参与人数
     */
    private Integer totalParticipants;

    /**
     * 拍品状态
     */
    private String itemStatus;

    /**
     * 记录时间戳
     */
    private LocalDateTime snapshotTime;

    /**
     * 记录类型（HOURLY=每小时快照，DAILY=每天快照，EVENT=关键事件快照，FINAL=最终结果）
     */
    private String snapshotType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 快照类型枚举
     */
    public enum SnapshotType {
        HOURLY,  // 每小时快照
        DAILY,   // 每天快照
        EVENT,   // 关键事件快照（如价格突破、领先者变更）
        FINAL    // 最终结果
    }
}
