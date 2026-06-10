package com.auction.service.follow;

import com.auction.api.dto.response.FollowStatusResponse;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.AuctionFollow;
import com.auction.domain.enums.AuctionStatus;
import com.auction.repository.AuctionFollowRepository;
import com.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 拍卖活动关注Service
 * <p>
 * 处理用户对拍卖活动的关注、取消关注等业务逻辑
 * 支持关注状态查询和统计功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionFollowService {

    private final AuctionFollowRepository followRepository;
    private final AuctionRepository auctionRepository;

    /**
     * 关注活动
     *
     * @param auctionId 活动ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void followAuction(Long auctionId, Long userId) {
        log.info("用户关注活动: auctionId={}, userId={}", auctionId, userId);

        // 验证活动存在
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 只有待开始的活动可以关注
        if (auction.getStatusEnum() != AuctionStatus.PENDING) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有待开始的活动可以关注");
        }

        // 检查是否已关注（包括检查是否有已取消的记录）
        List<AuctionFollow> existingFollows = followRepository.findByUserIdIncludingAllStatus(userId);
        AuctionFollow existingFollow = existingFollows.stream()
                .filter(f -> f.getAuctionId().equals(auctionId))
                .findFirst()
                .orElse(null);

        if (existingFollow != null) {
            if (existingFollow.getStatusEnum() == AuctionFollow.FollowStatus.ACTIVE) {
                throw new BizException(ErrorCode.BAD_REQUEST, "您已经关注过该活动");
            } else {
                // 如果是已取消的关注，重新激活
                existingFollow.setStatusEnum(AuctionFollow.FollowStatus.ACTIVE);
                followRepository.updateById(existingFollow);
                log.info("用户重新关注活动: auctionId={}, userId={}", auctionId, userId);
                return;
            }
        }

        // 创建关注记录
        AuctionFollow follow = new AuctionFollow();
        follow.setAuctionId(auctionId);
        follow.setUserId(userId);
        follow.setStatusEnum(AuctionFollow.FollowStatus.ACTIVE);

        followRepository.save(follow);

        log.info("用户关注活动成功: auctionId={}, userId={}", auctionId, userId);
    }

    /**
     * 取消关注活动
     *
     * @param auctionId 活动ID
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfollowAuction(Long auctionId, Long userId) {
        log.info("用户取消关注活动: auctionId={}, userId={}", auctionId, userId);

        // 查找关注记录
        List<AuctionFollow> follows = followRepository.findByUserId(userId);
        AuctionFollow follow = follows.stream()
                .filter(f -> f.getAuctionId().equals(auctionId) &&
                        f.getStatusEnum() == AuctionFollow.FollowStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        if (follow == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "您没有关注该活动");
        }

        // 更新状态为已取消
        follow.setStatusEnum(AuctionFollow.FollowStatus.CANCELLED);
        followRepository.updateById(follow);

        log.info("用户取消关注活动成功: auctionId={}, userId={}", auctionId, userId);
    }

    /**
     * 获取关注状态
     *
     * @param auctionId 活动ID
     * @param userId 用户ID
     * @return 关注状态响应
     */
    public FollowStatusResponse getFollowStatus(Long auctionId, Long userId) {
        boolean following = followRepository.isFollowing(auctionId, userId);

        String followedAt = null;
        if (following) {
            List<AuctionFollow> follows = followRepository.findByUserId(userId);
            AuctionFollow targetFollow = follows.stream()
                    .filter(f -> f.getAuctionId().equals(auctionId) &&
                            f.getStatusEnum() == AuctionFollow.FollowStatus.ACTIVE)
                    .findFirst()
                    .orElse(null);

            if (targetFollow != null && targetFollow.getCreatedAt() != null) {
                followedAt = targetFollow.getCreatedAt().toString();
            }
        }

        // 获取关注人数
        List<Long> followerIds = followRepository.findFollowerIdsByAuctionId(auctionId);
        int totalFollowers = followerIds.size();

        return new FollowStatusResponse(following, followedAt, totalFollowers);
    }

    /**
     * 获取用户关注的活动ID列表
     *
     * @param userId 用户ID
     * @return 活动ID列表
     */
    public List<Long> getUserFollowedAuctionIds(Long userId) {
        return followRepository.findFollowedAuctionIdsByUserId(userId);
    }

    /**
     * 获取活动关注者ID列表
     *
     * @param auctionId 活动ID
     * @return 关注者ID列表
     */
    public List<Long> getAuctionFollowerIds(Long auctionId) {
        return followRepository.findFollowerIdsByAuctionId(auctionId);
    }

    /**
     * 获取活动关注数量
     *
     * @param auctionId 活动ID
     * @return 关注者数量
     */
    public Integer getFollowerCount(Long auctionId) {
        List<Long> followerIds = followRepository.findFollowerIdsByAuctionId(auctionId);
        return followerIds.size();
    }
}