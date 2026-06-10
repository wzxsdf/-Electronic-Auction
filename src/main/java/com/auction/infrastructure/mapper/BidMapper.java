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
            "WHERE b.auction_item_id = #{auctionItemId} " +
            "ORDER BY b.amount DESC, b.created_at ASC")
    List<Bid> getBidsByItemId(@Param("auctionItemId") Long auctionItemId);

    @Select("SELECT b.*, u.username FROM bids b " +
            "LEFT JOIN users u ON b.user_id = u.id " +
            "WHERE b.auction_id = #{auctionId} " +
            "ORDER BY b.created_at DESC")
    List<Bid> getBidsByRoomId(@Param("auctionId") Long auctionId);

    @Select("SELECT COUNT(*) FROM bids WHERE auction_item_id = #{auctionItemId}")
    int countByItemId(@Param("auctionItemId") Long auctionItemId);

    /**
     * 统计出价高于指定金额的不同用户数量
     *
     * @param auctionItemId 拍品ID
     * @param amount 比较金额
     * @return 用户数量
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM bids " +
            "WHERE auction_item_id = #{auctionItemId} " +
            "AND status = 'ACTIVE' " +
            "AND amount > #{amount}")
    long countUsersWithHigherBid(@Param("auctionItemId") Long auctionItemId,
                                   @Param("amount") java.math.BigDecimal amount);
}
