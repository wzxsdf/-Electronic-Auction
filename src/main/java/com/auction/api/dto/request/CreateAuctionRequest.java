package com.auction.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建拍卖活动请求DTO（重构后）
 * <p>
 * 支持创建包含多个拍品的拍卖活动
 * 每个拍品可以独立设置价格参数
 */
@Data
public class CreateAuctionRequest {

    /**
     * 活动标题
     */
    @NotBlank(message = "活动标题不能为空")
    private String title;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 活动开始时间
     */
    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是未来时间")
    private LocalDateTime startTime;

    /**
     * 活动结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 最低保证金要求
     */
    @DecimalMin(value = "0.00", message = "保证金不能为负数")
    private BigDecimal minDeposit = BigDecimal.ZERO;

    /**
     * 最大拍品数量限制
     */
    @Min(value = 1, message = "至少添加1个拍品")
    @Max(value = 50, message = "最多添加50个拍品")
    private Integer maxItems = 50;

    /**
     * 活动级别的延时秒数（可被拍品级别覆盖）
     */
    @Min(value = 5, message = "延时秒数至少5秒")
    @Max(value = 300, message = "延时秒数最多300秒")
    private Integer defaultDelaySeconds = 15;

    /**
     * 拍品列表
     */
    @NotNull(message = "拍品列表不能为空")
    @Size(min = 1, max = 50, message = "拍品数量必须在1-50之间")
    @Valid
    private List<AuctionItemRequest> items;

    /**
     * 拍品参数请求DTO
     */
    @Data
    public static class AuctionItemRequest {

        /**
         * 商品ID
         */
        @NotNull(message = "商品ID不能为空")
        private Long productId;

        /**
         * 拍品标题（可选，不填则使用商品名称）
         */
        private String title;

        /**
         * 起拍价格
         */
        @NotNull(message = "起拍价不能为空")
        @DecimalMin(value = "0.01", message = "起拍价必须大于0")
        private BigDecimal startPrice;

        /**
         * 加价幅度
         */
        @NotNull(message = "加价幅度不能为空")
        @DecimalMin(value = "0.01", message = "加价幅度必须大于0")
        private BigDecimal bidIncrement;

        /**
         * 封顶价格（可选）
         */
        @DecimalMin(value = "0.01", message = "封顶价必须大于0")
        private BigDecimal maxPrice;

        /**
         * 延时秒数（可选，覆盖活动级别设置）
         */
        @Min(value = 5, message = "延时秒数至少5秒")
        @Max(value = 300, message = "延时秒数最多300秒")
        private Integer delaySeconds;

        /**
         * 拍品开始时间（可选，覆盖活动时间）
         */
        private LocalDateTime startTime;

        /**
         * 拍品结束时间（可选，覆盖活动时间）
         */
        private LocalDateTime endTime;

        /**
         * 显示顺序（可选，不填则自动分配）
         */
        @Min(value = 1, message = "显示顺序必须大于0")
        private Integer displayOrder;
    }
}
