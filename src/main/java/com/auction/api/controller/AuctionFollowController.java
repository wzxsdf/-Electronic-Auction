package com.auction.api.controller;

import com.auction.api.dto.response.FollowStatusResponse;
import com.auction.common.Result;
import com.auction.domain.entity.Auction;
import com.auction.infrastructure.security.CurrentUser;
import com.auction.infrastructure.security.UserPrincipal;
import com.auction.repository.AuctionRepository;
import com.auction.service.follow.AuctionFollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 拍卖活动关注控制器
 * <p>
 * 提供活动关注、取消关注、关注状态查询等功能
 */
@Slf4j
@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionFollowController {

    private final AuctionFollowService followService;
    private final AuctionRepository auctionRepository;

    /**
     * 关注活动
     * POST /auctions/{auctionId}/follow
     */
    @PostMapping("/{auctionId}/follow")
    public Result<Void> followAuction(
            @PathVariable Long auctionId,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("关注活动请求: auctionId={}, userId={}", auctionId, currentUser.getUserId());

            followService.followAuction(auctionId, currentUser.getUserId());
            return Result.ok();

        } catch (Exception e) {
            log.error("关注活动失败: auctionId={}, userId={}, error={}",
                    auctionId, currentUser.getUserId(), e.getMessage(), e);
            return Result.fail(500, "关注活动失败: " + e.getMessage());
        }
    }

    /**
     * 取消关注活动
     * DELETE /auctions/{auctionId}/follow
     */
    @DeleteMapping("/{auctionId}/follow")
    public Result<Void> unfollowAuction(
            @PathVariable Long auctionId,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("取消关注活动请求: auctionId={}, userId={}", auctionId, currentUser.getUserId());

            followService.unfollowAuction(auctionId, currentUser.getUserId());
            return Result.ok();

        } catch (Exception e) {
            log.error("取消关注活动失败: auctionId={}, userId={}, error={}",
                    auctionId, currentUser.getUserId(), e.getMessage(), e);
            return Result.fail(500, "取消关注活动失败: " + e.getMessage());
        }
    }

    /**
     * 获取关注状态
     * GET /auctions/{auctionId}/follow/status
     */
    @GetMapping("/{auctionId}/follow/status")
    public Result<FollowStatusResponse> getFollowStatus(
            @PathVariable Long auctionId,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("获取关注状态: auctionId={}, userId={}", auctionId, currentUser.getUserId());

            FollowStatusResponse response = followService.getFollowStatus(auctionId, currentUser.getUserId());
            return Result.ok(response);

        } catch (Exception e) {
            log.error("获取关注状态失败: auctionId={}, userId={}, error={}",
                    auctionId, currentUser.getUserId(), e.getMessage(), e);
            return Result.fail(500, "获取关注状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取我关注的活动列表
     * GET /auctions/my/following
     */
    @GetMapping("/my/following")
    public Result<List<Auction>> getMyFollowedAuctions(@CurrentUser UserPrincipal currentUser) {
        try {
            log.info("获取关注活动列表: userId={}", currentUser.getUserId());

            List<Long> followedIds = followService.getUserFollowedAuctionIds(currentUser.getUserId());

            List<Auction> auctions = followedIds.stream()
                    .map(auctionRepository::findById)
                    .filter(auction -> auction != null)
                    .toList();

            log.info("获取关注活动列表成功: userId={}, count={}", currentUser.getUserId(), auctions.size());
            return Result.ok(auctions);

        } catch (Exception e) {
            log.error("获取关注活动列表失败: userId={}, error={}",
                    currentUser.getUserId(), e.getMessage(), e);
            return Result.fail(500, "获取关注活动列表失败: " + e.getMessage());
        }
    }
}