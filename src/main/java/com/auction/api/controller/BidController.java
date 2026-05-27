package com.auction.api.controller;

import com.auction.annotation.RateLimit;
import com.auction.api.dto.request.PlaceBidRequest;
import com.auction.api.dto.response.BidHistoryResponse;
import com.auction.api.dto.response.BidStatisticsResponse;
import com.auction.api.dto.response.BidResultResponse;
import com.auction.common.Result;
import com.auction.domain.entity.Bid;
import com.auction.domain.entity.User;
import com.auction.repository.BidRepository;
import com.auction.repository.UserRepository;
import com.auction.service.bid.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 出价控制器：处理用户出价请求、查询出价历史记录和统计数据
 */
@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    /**
     * 用户出价：验证出价金额 → 更新竞拍价格 → 广播新价格给所有参与者
     * 限流：每分钟最多 30 次请求
     */
    @PostMapping
    @RateLimit(key = "bid", time = 60, count = 30, message = "出价过于频繁，请稍后再试")
    public Result<BidResultResponse> placeBid(@Valid @RequestBody PlaceBidRequest request) {
        return Result.ok(bidService.placeBid(request));
    }

    /**
     * 查询出价历史：获取指定竞拍的所有出价记录，按时间倒序排列，用户名脱敏处理
     *
     * @param auctionId 竞拍ID
     * @param limit 返回记录数量限制（默认100条，最大1000条）
     * @return 出价历史记录列表
     */
    @GetMapping("/auction/{auctionId}")
    public Result<List<BidHistoryResponse>> getBidHistory(
        @PathVariable Long auctionId,
        @RequestParam(defaultValue = "100") int limit
    ) {
        try {
            // 参数校验
            if (limit <= 0) {
                return Result.fail(400, "limit参数必须大于0");
            }
            int actualLimit = Math.min(limit, 1000); // 最大限制1000条

            // 查询出价记录
            List<Bid> bids = bidRepository.findRecentByItemId(auctionId, actualLimit);

            if (bids == null || bids.isEmpty()) {
                return Result.ok(List.of());
            }

            // 转换为响应DTO，包含用户昵称（脱敏）
            List<BidHistoryResponse> responses = bids.stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());

            return Result.ok(responses);
        } catch (Exception e) {
            return Result.fail(500, "查询出价历史失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户出价记录：获取指定用户在某个竞拍中的所有出价历史
     */
    @GetMapping("/user/{userId}/auction/{auctionId}")
    public Result<List<BidHistoryResponse>> getUserBidsInAuction(
        @PathVariable Long userId,
        @PathVariable Long auctionId
    ) {
        try {
            // 参数校验
            if (userId == null || userId <= 0) {
                return Result.fail(400, "用户ID无效");
            }
            if (auctionId == null || auctionId <= 0) {
                return Result.fail(400, "竞拍ID无效");
            }

            List<Bid> bids = bidRepository.findByItemIdAndUserId(auctionId, userId);

            if (bids == null || bids.isEmpty()) {
                return Result.ok(List.of());
            }

            List<BidHistoryResponse> responses = bids.stream()
                .map(this::convertToHistoryResponse)
                .collect(Collectors.toList());

            return Result.ok(responses);
        } catch (Exception e) {
            return Result.fail(500, "查询用户出价记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取出价统计：计算总出价次数、参与人数、自动出价数量和价格统计信息
     */
    @GetMapping("/auction/{auctionId}/statistics")
    public Result<BidStatisticsResponse> getBidStatistics(@PathVariable Long auctionId) {
        try {
            // 参数校验
            if (auctionId == null || auctionId <= 0) {
                return Result.fail(400, "竞拍ID无效");
            }

            long totalBids = bidRepository.countByItemId(auctionId);
            List<Bid> recentBids = bidRepository.findRecentByItemId(auctionId, 1000);

            if (recentBids == null || recentBids.isEmpty()) {
                // 返回空统计
                BidStatisticsResponse emptyStats = BidStatisticsResponse.builder()
                    .totalBids(0L)
                    .participantCount(0L)
                    .autoBidCount(0L)
                    .currentHighestPrice(BigDecimal.ZERO)
                    .currentLowestPrice(BigDecimal.ZERO)
                    .averagePrice(BigDecimal.ZERO)
                    .build();
                return Result.ok(emptyStats);
            }

            // 统计参与人数（去重用户ID）
            long participantCount = recentBids.stream()
                .map(Bid::getUserId)
                .distinct()
                .count();

            // 统计自动出价数量
            long autoBidCount = recentBids.stream()
                .filter(Bid::getIsAutoBid)
                .count();

            // 计算价格统计
            BigDecimal highestPrice = recentBids.stream()
                .map(Bid::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

            BigDecimal lowestPrice = recentBids.stream()
                .map(Bid::getAmount)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

            BigDecimal averagePrice = recentBids.stream()
                .map(Bid::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(recentBids.size()), 2, java.math.RoundingMode.HALF_UP);

            BidStatisticsResponse stats = BidStatisticsResponse.builder()
                .totalBids(totalBids)
                .participantCount(participantCount)
                .autoBidCount(autoBidCount)
                .currentHighestPrice(highestPrice)
                .currentLowestPrice(lowestPrice)
                .averagePrice(averagePrice)
                .build();

            return Result.ok(stats);
        } catch (Exception e) {
            return Result.fail(500, "获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 转换为出价历史响应DTO
     */
    private BidHistoryResponse convertToHistoryResponse(Bid bid) {
        // 获取用户信息用于脱敏
        User user = userRepository.findById(bid.getUserId());
        String username = user != null ? user.getNickname() : "未知用户";
        String maskedUsername = maskUsername(username);

        return BidHistoryResponse.builder()
            .bidId(bid.getId())
            .auctionId(bid.getAuctionId())
            .userId(bid.getUserId())
            .username(maskedUsername)
            .amount(bid.getAmount())
            .isAutoBid(bid.getIsAutoBid())
            .bidTime(bid.getCreatedAt())
            .build();
    }

    /**
     * 用户名脱敏处理
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return "***";
        }
        return username.substring(0, 2) + "***";
    }
}