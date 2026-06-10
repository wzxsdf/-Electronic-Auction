package com.auction.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlaceBidRequest {
    @NotNull(message = "竞拍ID不能为空")
    private Long auctionId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "出价金额不能为空")
    @Positive(message = "出价金额必须大于0")
    private BigDecimal amount;

    private Boolean isAutoBid = false;

    /**
     * 拍品ID（可选，用于多拍品拍卖）
     * 如果不为空，则对指定拍品出价；否则对auctionId表示的拍品出价
     */
    private Long auctionItemId;
}
