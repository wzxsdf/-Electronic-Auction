package com.auction.api.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuctionResponse {
    private Long id;
    private String title;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal bidIncrement;
    private BigDecimal maxPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String statusDesc;
    private Long highestBidder;
    private Integer bidCount;
    private LocalDateTime createdAt;
}
