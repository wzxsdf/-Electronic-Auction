package com.auction.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateAuctionRequest {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotBlank(message = "竞拍标题不能为空")
    private String title;

    @NotNull(message = "起拍价不能为空")
    @Positive(message = "起拍价必须大于0")
    private BigDecimal startPrice;

    @NotNull(message = "加价幅度不能为空")
    @Positive(message = "加价幅度必须大于0")
    private BigDecimal bidIncrement;

    private BigDecimal maxPrice;
    private Integer delaySeconds = 15;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
