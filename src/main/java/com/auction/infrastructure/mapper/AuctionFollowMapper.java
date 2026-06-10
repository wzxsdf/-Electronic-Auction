package com.auction.infrastructure.mapper;

import com.auction.domain.entity.AuctionFollow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 拍卖活动关注Mapper
 */
@Mapper
public interface AuctionFollowMapper extends BaseMapper<AuctionFollow> {

    /**
     * 查询用户关注的活动ID列表
     */
    @Select("SELECT auction_id FROM auction_follows WHERE user_id = #{userId} AND status = 'ACTIVE'")
    List<Long> findFollowedAuctionIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询活动的关注者ID列表
     */
    @Select("SELECT user_id FROM auction_follows WHERE auction_id = #{auctionId} AND status = 'ACTIVE'")
    List<Long> findFollowerIdsByAuctionId(@Param("auctionId") Long auctionId);

    /**
     * 查询用户是否关注了指定活动
     */
    @Select("SELECT COUNT(*) > 0 FROM auction_follows WHERE auction_id = #{auctionId} AND user_id = #{userId} AND status = 'ACTIVE'")
    boolean isFollowing(@Param("auctionId") Long auctionId, @Param("userId") Long userId);

    /**
     * 查询用户的关注记录列表
     */
    @Select("SELECT * FROM auction_follows WHERE user_id = #{userId} AND status = 'ACTIVE' ORDER BY created_at DESC")
    List<AuctionFollow> findByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的关注记录列表（包括所有状态，用于检查重复关注）
     */
    @Select("SELECT * FROM auction_follows WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<AuctionFollow> findByUserIdIncludingAllStatus(@Param("userId") Long userId);
}