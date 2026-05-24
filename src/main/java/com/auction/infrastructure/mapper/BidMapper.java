package com.auction.infrastructure.mapper;

import com.auction.domain.entity.Bid;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BidMapper extends BaseMapper<Bid> {

    @Select("SELECT b.*, u.username FROM bids b " +
            "LEFT JOIN users u ON b.user_id = u.id " +
            "WHERE b.item_id = #{itemId} " +
            "ORDER BY b.amount DESC, b.created_at ASC")
    List<Bid> getBidsByItemId(@Param("itemId") Long itemId);

    @Select("SELECT b.*, u.username FROM bids b " +
            "LEFT JOIN users u ON b.user_id = u.id " +
            "WHERE b.auction_id = #{roomId} " +
            "ORDER BY b.created_at DESC")
    List<Bid> getBidsByRoomId(@Param("roomId") Long roomId);

    @Select("SELECT COUNT(*) FROM bids WHERE item_id = #{itemId}")
    int countByItemId(@Param("itemId") Long itemId);
}
