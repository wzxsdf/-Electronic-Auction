package com.auction.api.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuctionDetailResponse extends AuctionResponse {
    private String description;
    private Integer delaySeconds;
    private LocalDateTime originalEndTime;
    private Long participantCount;
    private Boolean isExtendable;
}
