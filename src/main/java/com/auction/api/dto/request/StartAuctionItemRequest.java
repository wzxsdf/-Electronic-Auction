package com.auction.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 开始拍品请求DTO
 * <p>
 * 用于在开始拍品时传递持续时间和可选的延时秒数
 * 持续时间决定拍品的结束时间：endTime = 开始时间 + durationMinutes
 */
@Data
public class StartAuctionItemRequest {

    /**
     * 拍卖持续时间（分钟）
     * <p>
     * 决定拍品的结束时间：endTime = now + durationMinutes
     * <p>
     * 业务规则：
     * - 最短1分钟：适用于测试或快速拍卖
     * - 最长1440分钟（24小时）：防止超长拍卖影响系统性能
     * - 推荐值：5-60分钟
     */
    @NotNull(message = "持续时间不能为空")
    @Min(value = 1, message = "持续时间至少1分钟")
    @Max(value = 1440, message = "持续时间最多1440分钟（24小时）")
    private Integer durationMinutes;

    /**
     * 自定义延时秒数（可选）
     * <p>
     * 如果不为空，则覆盖拍品默认的延时设置
     * 延时机制：在结束前delaySeconds内出价时，自动延长结束时间
     * <p>
     * 业务规则：
     * - 默认值：如果不提供，使用拍品的delaySeconds字段
     * - 范围：5-300秒（与现有系统一致）
     */
    @Min(value = 5, message = "延时秒数至少5秒")
    @Max(value = 300, message = "延时秒数最多300秒")
    private Integer overrideDelaySeconds;
}
