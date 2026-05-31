package com.auction.api.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖信息响应DTO
 * 用于返回拍卖基本信息，适用于列表展示场景
 */
@Data
public class AuctionResponse {

    /**
     * 拍卖ID
     */
    private Long id;

    /**
     * 拍卖标题
     */
    private String title;

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
     * 起拍价
     */
    private BigDecimal startPrice;

    /**
     * 当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 加价幅度
     */
    private BigDecimal bidIncrement;

    /**
     * 封顶价
     */
    private BigDecimal maxPrice;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态码
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 最高出价者用户ID
     */
    private Long highestBidder;

    /**
     * 出价次数
     */
    private Integer bidCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
