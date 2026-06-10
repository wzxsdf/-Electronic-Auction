package com.auction.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送聊天消息请求
 */
@Data
public class SendMessageRequest {

    /**
     * 拍卖活动ID
     */
    @NotNull(message = "拍卖活动ID不能为空")
    private Long auctionId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Max(value = 500, message = "消息内容不能超过500字")
    private String content;
}
