package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 竞拍排行榜列表响应DTO
 * 用于返回拍品的完整竞拍排行榜
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidRankingListResponse {

    /**
     * 拍品ID
     */
    private Long auctionItemId;

    /**
     * 拍卖活动ID
     */
    private Long auctionId;

    /**
     * 总参与人数
     */
    private Long totalParticipants;

    /**
     * 排行榜列表
     */
    private List<BidRankingResponse> rankings;

    /**
     * 当前价格
     */
    private java.math.BigDecimal currentPrice;

    /**
     * 出价增量
     */
    private java.math.BigDecimal bidIncrement;
}
