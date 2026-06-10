package com.auction.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 拍卖活动ID
     */
    private Long auctionId;

    /**
     * 发送用户ID
     */
    private Long userId;

    /**
     * 用户名（脱敏显示）
     */
    private String username;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：1-用户消息，2-系统消息
     */
    private Integer messageType;

    /**
     * 发送时间
     */
    private LocalDateTime createdAt;
}
