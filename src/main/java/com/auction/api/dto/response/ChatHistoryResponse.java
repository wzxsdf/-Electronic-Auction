package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天历史响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {

    /**
     * 拍卖活动ID
     */
    private Long auctionId;

    /**
     * 消息总数
     */
    private Integer total;

    /**
     * 聊天消息列表
     */
    private List<ChatMessageResponse> messages;
}
