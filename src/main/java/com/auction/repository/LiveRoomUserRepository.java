package com.auction.repository;

import com.auction.domain.entity.LiveRoomUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 直播间用户数据访问接口
 */
@Mapper
public interface LiveRoomUserRepository extends BaseMapper<LiveRoomUser> {

    /**
     * 查询指定拍卖活动的所有在线用户
     *
     * @param auctionId 拍卖活动ID
     * @param isOnline  是否在线
     * @return 在线用户列表（按最后活跃时间倒序）
     */
    @Select("SELECT * FROM live_room_users " +
            "WHERE auction_id = #{auctionId} " +
            "AND is_online = #{isOnline} " +
            "ORDER BY last_active_time DESC")
    List<LiveRoomUser> findAllByAuctionIdAndIsOnlineOrderByLastActiveTimeDesc(@Param("auctionId") Long auctionId,
                                                                               @Param("isOnline") Boolean isOnline);

    /**
     * 统计指定拍卖活动的在线人数
     *
     * @param auctionId 拍卖活动ID
     * @param isOnline  是否在线
     * @return 在线人数
     */
    @Select("SELECT COUNT(*) FROM live_room_users " +
            "WHERE auction_id = #{auctionId} " +
            "AND is_online = #{isOnline}")
    long countByAuctionIdAndIsOnline(@Param("auctionId") Long auctionId,
                                     @Param("isOnline") Boolean isOnline);
}