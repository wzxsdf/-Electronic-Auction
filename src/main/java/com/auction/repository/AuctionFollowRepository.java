package com.auction.repository;

import com.auction.domain.entity.AuctionFollow;
import com.auction.infrastructure.mapper.AuctionFollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 拍卖活动关注Repository
 * <p>
 * 提供关注关系的数据访问操作
 */
@Repository
@RequiredArgsConstructor
public class AuctionFollowRepository {

    private final AuctionFollowMapper auctionFollowMapper;

    /**
     * 保存关注记录
     *
     * @param follow 关注记录
     * @return 保存后的关注记录
     */
    public AuctionFollow save(AuctionFollow follow) {
        if (follow.getId() == null) {
            auctionFollowMapper.insert(follow);
        } else {
            auctionFollowMapper.updateById(follow);
        }
        return follow;
    }

    /**
     * 更新关注记录
     *
     * @param follow 关注记录
     * @return 影响的行数
     */
    public int updateById(AuctionFollow follow) {
        return auctionFollowMapper.updateById(follow);
    }

    /**
     * 根据ID查询关注记录
     *
     * @param id 关注记录ID
     * @return 关注记录
     */
    public AuctionFollow findById(Long id) {
        return auctionFollowMapper.selectById(id);
    }

    /**
     * 查询用户关注的活动ID列表
     *
     * @param userId 用户ID
     * @return 活动ID列表
     */
    public List<Long> findFollowedAuctionIdsByUserId(Long userId) {
        return auctionFollowMapper.findFollowedAuctionIdsByUserId(userId);
    }

    /**
     * 查询活动的关注者ID列表
     *
     * @param auctionId 活动ID
     * @return 关注者ID列表
     */
    public List<Long> findFollowerIdsByAuctionId(Long auctionId) {
        return auctionFollowMapper.findFollowerIdsByAuctionId(auctionId);
    }

    /**
     * 查询用户是否关注了指定活动
     *
     * @param auctionId 活动ID
     * @param userId 用户ID
     * @return 是否关注
     */
    public boolean isFollowing(Long auctionId, Long userId) {
        return auctionFollowMapper.isFollowing(auctionId, userId);
    }

    /**
     * 查询用户的关注记录列表
     *
     * @param userId 用户ID
     * @return 关注记录列表
     */
    public List<AuctionFollow> findByUserId(Long userId) {
        return auctionFollowMapper.findByUserId(userId);
    }

    /**
     * 查询用户的关注记录列表（包括所有状态，用于检查重复关注）
     *
     * @param userId 用户ID
     * @return 所有关注记录列表
     */
    public List<AuctionFollow> findByUserIdIncludingAllStatus(Long userId) {
        return auctionFollowMapper.findByUserIdIncludingAllStatus(userId);
    }
}