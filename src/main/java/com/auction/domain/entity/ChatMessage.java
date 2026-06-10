package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 * <p>
 * 存储直播间的聊天消息记录，支持用户消息和系统消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_messages")
public class ChatMessage {

    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 拍卖活动ID
     */
    private Long auctionId;

    /**
     * 发送用户ID
     */
    private Long userId;

    /**
     * 用户名（显示用，已脱敏）
     */
    private String username;

    /**
     * 聊天内容
     */
    private String content;

    /**
     * 消息类型：1-用户消息，2-系统消息
     */
    private Integer messageType;

    /**
     * 是否已删除
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}