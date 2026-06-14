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

    /**
     * 获取拍品的竞拍排行榜
     * 按用户分组，返回每个用户的最高出价、出价次数、最后出价时间等信息
     * 按最高出价金额降序排列
     *
     * @param auctionItemId 拍品ID
     * @param limit 返回数量限制
     * @return 排行榜数据
     */
    @Select("SELECT " +
            "  user_id, " +
            "  MAX(amount) as highest_bid_amount, " +
            "  COUNT(*) as bid_count, " +
            "  MAX(created_at) as last_bid_time " +
            "FROM bids " +
            "WHERE auction_item_id = #{auctionItemId} " +
            "  AND status = 'ACTIVE' " +
            "GROUP BY user_id " +
            "ORDER BY highest_bid_amount DESC, last_bid_time ASC " +
            "LIMIT #{limit}")
    List<java.util.Map<String, Object>> getBidRanking(@Param("auctionItemId") Long auctionItemId,
                                                       @Param("limit") int limit);
}
