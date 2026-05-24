package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class BidResultResponse {
    private Long bidId;
    private BigDecimal currentPrice;
    private Integer yourRank;
    private Boolean isLeading;
    private Long remainingMs;
    private Boolean wasExtended;
    private Integer newEndTime;
    private String message;
}
