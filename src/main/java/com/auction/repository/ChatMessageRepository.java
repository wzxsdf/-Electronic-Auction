package com.auction.repository;

import com.auction.domain.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息数据访问接口
 */
@Mapper
public interface ChatMessageRepository extends BaseMapper<ChatMessage> {

    /**
     * 查询指定拍卖活动最近N条用户消息
     *
     * @param auctionId   拍卖活动ID
     * @param messageType 消息类型（1-用户消息）
     * @param limit       限制数量
     * @return 聊天消息列表（按创建时间倒序）
     */
    @Select("SELECT * FROM chat_messages " +
            "WHERE auction_id = #{auctionId} " +
            "AND message_type = #{messageType} " +
            "AND is_deleted = false " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<ChatMessage> findRecentMessages(@Param("auctionId") Long auctionId,
                                         @Param("messageType") Integer messageType,
                                         @Param("limit") int limit);
}