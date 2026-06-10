package com.auction.api.controller;

import com.auction.annotation.RateLimit;
import com.auction.api.dto.request.PlaceBidRequest;
import com.auction.api.dto.response.BidHistoryResponse;
import com.auction.api.dto.response.BidStatisticsResponse;
import com.auction.api.dto.response.BidResultResponse;
import com.auction.common.Result;
import com.auction.domain.entity.AuctionItem;
import com.auction.domain.entity.Bid;
import com.auction.domain.entity.User;
import com.auction.infrastructure.security.CurrentUser;
import com.auction.infrastructure.security.UserPrincipal;
import com.auction.repository.AuctionItemRepository;
import com.auction.repository.BidRepository;
import com.auction.repository.UserRepository;
import com.auction.service.auction.AuctionItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 出价控制器（重构后）
 * <p>
 * 处理用户出价请求、查询出价历史记录和统计数据
 * 集成AuctionItemService，支持拍品级别出价
 */
@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
public class BidController {

    private final AuctionItemService auctionItemService;
    private final AuctionItemRepository auctionItemRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    /**
     * 用户出价：验证出价金额 → 更新拍品价格 → 广播新价格给所有参与者
     * 限流：每分钟最多 30 次请求
     * POST /bids
     */
    @PostMapping
    @RateLimit(key = "bid", time = 60, count = 30, message = "出价过于频繁，请稍后再试")
    public Result<BidResultResponse> placeBid(
            @Valid @RequestBody PlaceBidRequest request,
            @CurrentUser UserPrincipal currentUser) {
        try {
            // 设置当前用户
            request.setUserId(currentUser.getUserId());

            // 如果指定了拍品ID，使用拍品出价
            if (request.getAuctionItemId() != null) {
                return Result.ok(auctionItemService.placeBid(request));
            }

            // 兼容旧API：如果只有auctionId，查找第一个活跃拍品
            if (request.getAuctionId() != null) {
                return handleLegacyBid(request);
            }

            return Result.fail(400, "请指定auctionItemId或auctionId参数");

        } catch (Exception e) {
            return Result.fail(500, "出价失败: " + e.getMessage());
        }
    }

    /**
     * 查询出价历史：获取指定拍品的所有出价记录，按时间倒序排列，用户名脱敏处理
     * GET /bids/auction-item/{auctionItemId}
     *
     * @param auctionItemId 拍品ID
     * @param limit 返回记录数量限制（默认100条，最大1000条）
     * @return 出价历史记录列表
     */
    @GetMapping("/auction-item/{auctionItemId}")
    public Result<List<BidHistoryResponse>> getBidHistoryByItemId(
            @PathVariable Long auctionItemId,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            // 参数校验
            if (limit <= 0) {
                return Result.fail(400, "limit参数必须大于0");
            }
            int actualLimit = Math.min(limit, 1000); // 最大限制1000条

            // 查询出价记录
            List<Bid> bids = bidRepository.findRecentByItemId(auctionItemId, actualLimit);

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
     * 查询用户出价记录：获取指定用户在某个拍品中的所有出价历史
     * GET /bids/user/{userId}/auction-item/{auctionItemId}
     */
    @GetMapping("/user/{userId}/auction-item/{auctionItemId}")
    public Result<List<BidHistoryResponse>> getUserBidsInItem(
            @PathVariable Long userId,
            @PathVariable Long auctionItemId
    ) {
        try {
            // 参数校验
            if (userId == null || userId <= 0) {
                return Result.fail(400, "用户ID无效");
            }
            if (auctionItemId == null || auctionItemId <= 0) {
                return Result.fail(400, "拍品ID无效");
            }

            List<Bid> bids = bidRepository.findByItemIdAndUserId(auctionItemId, userId);

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
     * GET /bids/auction-item/{auctionItemId}/statistics
     */
    @GetMapping("/auction-item/{auctionItemId}/statistics")
    public Result<BidStatisticsResponse> getBidStatisticsByItemId(@PathVariable Long auctionItemId) {
        try {
            // 参数校验
            if (auctionItemId == null || auctionItemId <= 0) {
                return Result.fail(400, "拍品ID无效");
            }

            long totalBids = bidRepository.countByItemId(auctionItemId);
            List<Bid> recentBids = bidRepository.findRecentByItemId(auctionItemId, 1000);

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
                .auctionItemId(bid.getAuctionItemId())
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

    /**
     * 处理旧版API出价请求（根据auctionId查找活跃拍品）
     */
    private Result<BidResultResponse> handleLegacyBid(PlaceBidRequest request) {
        // 查找该拍卖活动下的活跃拍品
        List<AuctionItem> activeItems = auctionItemRepository.findActiveItemsByAuctionId(request.getAuctionId());

        if (activeItems == null || activeItems.isEmpty()) {
            return Result.fail(400, "该拍卖活动下没有活跃的拍品");
        }

        // 如果有多个活跃拍品，选择displayOrder最小的第一个
        AuctionItem targetItem = activeItems.stream()
                .min(java.util.Comparator.comparing(item ->
                    item.getDisplayOrder() != null ? item.getDisplayOrder() : Integer.MAX_VALUE))
                .orElse(activeItems.get(0));

        // 设置auctionItemId并执行出价
        request.setAuctionItemId(targetItem.getId());
        return Result.ok(auctionItemService.placeBid(request));
    }
}
