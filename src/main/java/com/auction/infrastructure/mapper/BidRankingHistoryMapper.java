package com.auction.infrastructure.mapper;

import com.auction.domain.entity.BidRankingHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞拍排行榜历史数据映射器
 */
@Mapper
public interface BidRankingHistoryMapper extends BaseMapper<BidRankingHistory> {

    /**
     * 查询指定拍品的历史排行榜记录
     */
    @Select("SELECT * FROM bid_ranking_history " +
            "WHERE auction_item_id = #{auctionItemId} " +
            "ORDER BY snapshot_time DESC " +
            "LIMIT #{limit}")
    List<BidRankingHistory> findByAuctionItemId(@Param("auctionItemId") Long auctionItemId,
                                                 @Param("limit") int limit);

    /**
     * 查询指定时间范围内的排行榜历史
     */
    @Select("SELECT * FROM bid_ranking_history " +
            "WHERE auction_item_id = #{auctionItemId} " +
            "AND snapshot_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY snapshot_time DESC")
    List<BidRankingHistory> findByTimeRange(@Param("auctionItemId") Long auctionItemId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定类型的排行榜历史
     */
    @Select("SELECT * FROM bid_ranking_history " +
            "WHERE auction_item_id = #{auctionItemId} " +
            "AND snapshot_type = #{snapshotType} " +
            "ORDER BY snapshot_time DESC " +
            "LIMIT #{limit}")
    List<BidRankingHistory> findBySnapshotType(@Param("auctionItemId") Long auctionItemId,
                                                 @Param("snapshotType") String snapshotType,
                                                 @Param("limit") int limit);

    /**
     * 统计用户在指定拍品的历史最高排名
     */
    @Select("SELECT MIN(ranking_position) as best_rank FROM bid_ranking_history " +
            "WHERE auction_item_id = #{auctionItemId} " +
            "AND user_id = #{userId}")
    Integer findBestRankByUser(@Param("auctionItemId") Long auctionItemId,
                                @Param("userId") Long userId);

    /**
     * 查询用户的排行榜历史记录
     */
    @Select("SELECT * FROM bid_ranking_history " +
            "WHERE user_id = #{userId} " +
            "ORDER BY snapshot_time DESC " +
            "LIMIT #{limit}")
    List<BidRankingHistory> findByUserId(@Param("userId") Long userId,
                                          @Param("limit") int limit);

    /**
     * 统计拍品的总参与人数历史趋势
     */
    @Select("SELECT snapshot_time, total_participants, current_price " +
            "FROM bid_ranking_history " +
            "WHERE auction_item_id = #{auctionItemId} " +
            "AND ranking_position = 1 " +
            "ORDER BY snapshot_time DESC")
    List<java.util.Map<String, Object>> findTrendsData(@Param("auctionItemId") Long auctionItemId);
}
