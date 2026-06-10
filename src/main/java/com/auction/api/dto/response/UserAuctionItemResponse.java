package com.auction.api.dto.response;

import com.auction.domain.enums.AuctionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户参与拍品响应DTO
 * <p>
 * 返回用户参与过的拍品商品信息，包括拍品详情、商品信息、所属拍卖活动（直播间）
 * 以及用户参与状态（是否拍中、出价次数、最高出价等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuctionItemResponse {

    // ==================== 拍品基本信息 ====================

    /**
     * 拍品ID
     */
    private Long auctionItemId;

    /**
     * 拍品标题
     */
    private String title;

    /**
     * 当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 起拍价格
     */
    private BigDecimal startPrice;

    /**
     * 竞拍状态
     */
    private AuctionStatus status;

    /**
     * 竞拍开始时间
     */
    private LocalDateTime startTime;

    /**
     * 竞拍结束时间
     */
    private LocalDateTime endTime;

    // ==================== 商品信息 ====================

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片URL
     */
    private String productImageUrl;

    /**
     * 商品描述
     */
    private String productDescription;

    // ==================== 所属拍卖活动（直播间）信息 ====================

    /**
     * 拍卖活动ID（直播间ID）
     */
    private Long auctionId;

    /**
     * 拍卖活动标题（直播间标题）
     */
    private String auctionTitle;

    /**
     * 拍卖活动描述
     */
    private String auctionDescription;

    // ==================== 用户参与状态 ====================

    /**
     * 是否拍中
     */
    private Boolean isWon;

    /**
     * 是否已结束
     */
    private Boolean isFinished;

    /**
     * 用户出价次数
     */
    private Integer yourBidCount;

    /**
     * 用户最高出价金额
     */
    private BigDecimal yourHighestBid;

    /**
     * 最后出价时间
     */
    private LocalDateTime lastBidTime;

    /**
     * 是否当前领先
     */
    private Boolean isLeading;
}
