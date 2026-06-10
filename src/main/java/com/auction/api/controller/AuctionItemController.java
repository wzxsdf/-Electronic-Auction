package com.auction.api.controller;

import com.auction.api.dto.request.PlaceBidRequest;
import com.auction.api.dto.response.ActiveAuctionsResponse;
import com.auction.api.dto.response.AuctionItemPriceResponse;
import com.auction.api.dto.response.AuctionItemStatisticsResponse;
import com.auction.api.dto.response.BidHistoryResponse;
import com.auction.api.dto.response.BidResultResponse;
import com.auction.common.Result;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.AuctionItem;
import com.auction.domain.entity.Bid;
import com.auction.domain.entity.Product;
import com.auction.domain.entity.User;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.security.CurrentUser;
import com.auction.infrastructure.security.UserPrincipal;
import com.auction.repository.AuctionItemRepository;
import com.auction.repository.AuctionRepository;
import com.auction.repository.BidRepository;
import com.auction.repository.ProductRepository;
import com.auction.repository.UserRepository;
import com.auction.service.auction.AuctionItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拍品控制器（重构后）
 * <p>
 * 完整实现拍品级别的API接口
 * 集成新的AuctionItemService
 * 支持出价、自动出价、状态管理等功能
 */
@Slf4j
@RestController
@RequestMapping("/auction-items")
@RequiredArgsConstructor
public class AuctionItemController {

    private final AuctionItemService auctionItemService;
    private final AuctionItemRepository auctionItemRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * 查询拍品详情
     * GET /auction-items/{id}
     */
    @GetMapping("/{id}")
    public Result<AuctionItem> getById(@PathVariable Long id) {
        try {
            log.info("查询拍品详情: auctionItemId={}", id);

            AuctionItem item = auctionItemRepository.findById(id);
            if (item == null) {
                return Result.fail(404, "拍品不存在");
            }

            return Result.ok(item);

        } catch (Exception e) {
            log.error("查询拍品详情失败: auctionItemId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "查询拍品详情失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有拍品
     * GET /auction-items
     */
    @GetMapping
    public Result<List<AuctionItem>> listAll() {
        try {
            log.info("查询所有拍品");
            List<AuctionItem> items = auctionItemRepository.findAll();
            return Result.ok(items);
        } catch (Exception e) {
            log.error("查询所有拍品失败: error={}", e.getMessage(), e);
            return Result.fail(500, "查询所有拍品失败: " + e.getMessage());
        }
    }

    /**
     * 查询活跃拍品
     * GET /auction-items/active
     */
    @GetMapping("/active")
    public Result<List<AuctionItem>> listActive() {
        try {
            log.info("查询活跃拍品");
            List<AuctionItem> items = auctionItemRepository.findByStatus(AuctionStatus.ACTIVE);
            return Result.ok(items);
        } catch (Exception e) {
            log.error("查询活跃拍品失败: error={}", e.getMessage(), e);
            return Result.fail(500, "查询活跃拍品失败: " + e.getMessage());
        }
    }

    /**
     * 查询指定拍卖活动的活跃拍品
     * GET /auction-items/auction/{auctionId}/active-items
     * <p>
     * 返回指定拍卖活动下正在进行中的拍品列表
     * 包含完整信息：价格、倒计时、最高出价者、商品图片等
     *
     * @param auctionId 拍卖活动ID
     * @return 活跃拍品列表（包含商品信息和时间信息）
     */
    @GetMapping("/auction/{auctionId}/active-items")
    public Result<List<ActiveAuctionsResponse.ActiveItemInfo>> getActiveItemsByAuction(
            @PathVariable Long auctionId) {
        try {
            log.info("查询活动活跃拍品: auctionId={}", auctionId);

            // 验证活动是否存在
            Auction auction = auctionRepository.findById(auctionId);
            if (auction == null) {
                return Result.fail(404, "拍卖活动不存在");
            }

            // 查询该活动下正在进行中的拍品
            List<AuctionItem> activeItems = auctionItemRepository.findByAuctionIdAndStatus(
                    auctionId, AuctionStatus.ACTIVE);

            if (activeItems == null || activeItems.isEmpty()) {
                log.info("该活动下没有正在进行的拍品: auctionId={}", auctionId);
                return Result.ok(List.of());
            }

            // 加载商品信息
            loadProductInfo(activeItems);

            // 构建响应列表
            List<ActiveAuctionsResponse.ActiveItemInfo> itemInfos = activeItems.stream()
                    .map(this::buildActiveItemInfo)
                    .collect(java.util.stream.Collectors.toList());

            log.info("查询活动活跃拍品成功: auctionId={}, 拍品数={}", auctionId, itemInfos.size());
            return Result.ok(itemInfos);

        } catch (Exception e) {
            log.error("查询活动活跃拍品失败: auctionId={}, error={}", auctionId, e.getMessage(), e);
            return Result.fail(500, "查询活动活跃拍品失败: " + e.getMessage());
        }
    }

    /**
     * 查询活动内的拍品列表
     * GET /auctions/{auctionId}/items
     */
    @GetMapping("/auction/{auctionId}")
    public Result<List<AuctionItem>> getByAuction(@PathVariable Long auctionId) {
        try {
            log.info("查询活动拍品: auctionId={}", auctionId);
            List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);

            // 加载商品信息（包含图片URL）
            loadProductInfo(items);

            return Result.ok(items);
        } catch (Exception e) {
            log.error("查询活动拍品失败: auctionId={}, error={}", auctionId, e.getMessage(), e);
            return Result.fail(500, "查询活动拍品失败: " + e.getMessage());
        }
    }

    /**
     * 开始拍品
     * POST /auction-items/{id}/start
     */
    @PostMapping("/{id}/start")
    public Result<Void> startItem(
            @PathVariable Long id,
            @RequestBody @Valid com.auction.api.dto.request.StartAuctionItemRequest request,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("开始拍品: auctionItemId={}, userId={}, durationMinutes={}",
                    id, currentUser.getUserId(), request.getDurationMinutes());
            auctionItemService.startItem(id, currentUser.getUserId(), request);
            return Result.ok();
        } catch (Exception e) {
            log.error("开始拍品失败: auctionItemId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "开始拍品失败: " + e.getMessage());
        }
    }

    /**
     * 结束拍品
     * POST /auction-items/{id}/end
     */
    @PostMapping("/{id}/end")
    public Result<Void> endItem(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("结束拍品: auctionItemId={}, userId={}", id, currentUser.getUserId());
            auctionItemService.endItem(id, currentUser.getUserId());
            return Result.ok();
        } catch (Exception e) {
            log.error("结束拍品失败: auctionItemId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "结束拍品失败: " + e.getMessage());
        }
    }

    /**
     * 对拍品出价
     * POST /auction-items/{id}/bid
     */
    @PostMapping("/{id}/bid")
    public Result<BidResultResponse> placeBid(
            @PathVariable Long id,
            @RequestBody PlaceBidRequest request,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("拍品出价: auctionItemId={}, userId={}, amount={}",
                    id, currentUser.getUserId(), request.getAmount());

            request.setUserId(currentUser.getUserId());
            request.setAuctionItemId(id);

            BidResultResponse response = auctionItemService.placeBid(request);
            return Result.ok(response);

        } catch (Exception e) {
            log.error("拍品出价失败: auctionItemId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "拍品出价失败: " + e.getMessage());
        }
    }

    /**
     * 设置自动出价
     * POST /auction-items/{id}/auto-bid
     */
    @PostMapping("/{id}/auto-bid")
    public Result<Void> setAutoBid(
            @PathVariable Long id,
            @RequestParam @NotNull Long maxPrice,
            @RequestParam(defaultValue = "SMART") String strategy,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("设置自动出价: auctionItemId={}, userId={}, maxPrice={}, strategy={}",
                    id, currentUser.getUserId(), maxPrice, strategy);

            auctionItemService.setAutoBid(id, currentUser.getUserId(),
                    new BigDecimal(maxPrice),
                    com.auction.domain.enums.AutoBidStrategy.valueOf(strategy));
            return Result.ok();
        } catch (Exception e) {
            log.error("设置自动出价失败: auctionItemId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "设置自动出价失败: " + e.getMessage());
        }
    }

    /**
     * 取消自动出价
     * DELETE /auction-items/{id}/auto-bid
     */
    @DeleteMapping("/{id}/auto-bid")
    public Result<Void> cancelAutoBid(
            @PathVariable Long id,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("取消自动出价: auctionItemId={}, userId={}", id, currentUser.getUserId());
            auctionItemService.cancelAutoBid(id, currentUser.getUserId());
            return Result.ok();
        } catch (Exception e) {
            log.error("取消自动出价失败: auctionItemId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "取消自动出价失败: " + e.getMessage());
        }
    }

    /**
     * 查询拍品出价历史
     * GET /auction-items/{id}/bids
     */
    @GetMapping("/{id}/bids")
    public Result<List<BidHistoryResponse>> getBidHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            log.info("查询出价历史: auctionItemId={}, limit={}", id, limit);

            if (limit <= 0) {
                return Result.fail(400, "limit参数必须大于0");
            }
            int actualLimit = Math.min(limit, 1000);

            List<Bid> bids = bidRepository.findRecentByItemId(id, actualLimit);

            if (bids == null || bids.isEmpty()) {
                return Result.ok(List.of());
            }

            List<BidHistoryResponse> responses = bids.stream()
                    .map(this::convertBidToResponse)
                    .collect(java.util.stream.Collectors.toList());

            return Result.ok(responses);

        } catch (Exception e) {
            log.error("查询出价历史失败: auctionItemId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "查询出价历史失败: " + e.getMessage());
        }
    }

    /**
     * 转换出价记录为响应DTO
     */
    private BidHistoryResponse convertBidToResponse(Bid bid) {
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
     * 查询拍品统计
     * GET /auction-items/{id}/statistics
     */
    @GetMapping("/{id}/statistics")
    public Result<AuctionItemStatisticsResponse> getStatistics(@PathVariable Long id) {
        try {
            log.info("查询拍品统计: auctionItemId={}", id);

            AuctionItem item = auctionItemRepository.findById(id);
            if (item == null) {
                return Result.fail(404, "拍品不存在");
            }

            List<Bid> bids = bidRepository.findRecentByItemId(id, 1000);

            long participantCount = bids.stream()
                    .map(Bid::getUserId)
                    .distinct()
                    .count();

            long autoBidCount = bids.stream()
                    .filter(Bid::getIsAutoBid)
                    .count();

            BigDecimal highestPrice = item.getCurrentPrice();
            BigDecimal lowestPrice = item.getStartPrice();

            BigDecimal averagePrice = bids.isEmpty() ? BigDecimal.ZERO :
                    bids.stream()
                            .map(Bid::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(bids.size()), 2, java.math.RoundingMode.HALF_UP);

            List<BigDecimal> priceTrend = bids.stream()
                    .limit(10)
                    .map(Bid::getAmount)
                    .collect(java.util.stream.Collectors.toList());

            AuctionItemStatisticsResponse response = AuctionItemStatisticsResponse.builder()
                    .auctionItemId(id)
                    .auctionId(item.getAuctionId())
                    .totalBids((long) bids.size())
                    .participantCount(participantCount)
                    .autoBidCount(autoBidCount)
                    .startPrice(item.getStartPrice())
                    .currentPrice(item.getCurrentPrice())
                    .highestPrice(highestPrice)
                    .lowestPrice(lowestPrice)
                    .averagePrice(averagePrice)
                    .priceTrend(priceTrend)
                    .bidCount(item.getBidCount())
                    .status(item.getStatus())
                    .build();

            return Result.ok(response);

        } catch (Exception e) {
            log.error("查询拍品统计失败: auctionItemId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "查询拍品统计失败: " + e.getMessage());
        }
    }

    /**
     * 查询拍品实时价格
     * GET /auction-items/{id}/price
     */
    @GetMapping("/{id}/price")
    public Result<AuctionItemPriceResponse> getCurrentPrice(@PathVariable Long id) {
        try {
            log.info("查询拍品价格: auctionItemId={}", id);

            AuctionItem item = auctionItemRepository.findById(id);
            if (item == null) {
                return Result.fail(404, "拍品不存在");
            }

            // 计算剩余秒数（已废弃，保留兼容性）
            Long remainingSeconds = null;
            String endTime = null;
            Long endTimeTimestamp = null;

            if (item.getEndTime() != null) {
                long secs = java.time.temporal.ChronoUnit.SECONDS.between(
                        LocalDateTime.now(), item.getEndTime());
                remainingSeconds = Math.max(0, secs);
                endTime = item.getEndTime().toString(); // ISO 8601格式
                endTimeTimestamp = item.getEndTime().atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli(); // 转换为时间戳
            }

            boolean isBiddable = item.getStatusEnum() == AuctionStatus.ACTIVE &&
                    (remainingSeconds == null || remainingSeconds > 0);

            AuctionItemPriceResponse response = AuctionItemPriceResponse.builder()
                    .auctionItemId(id)
                    .auctionId(item.getAuctionId())
                    .currentPrice(item.getCurrentPrice())
                    .highestBidder(item.getHighestBidder())
                    .bidCount(item.getBidCount())
                    .status(item.getStatus())
                    .endTime(endTime) // 绝对时间（ISO格式）
                    .endTimeTimestamp(endTimeTimestamp) // 绝对时间戳
                    .remainingSeconds(remainingSeconds) // 兼容性字段
                    .startPrice(item.getStartPrice())
                    .bidIncrement(item.getBidIncrement())
                    .maxPrice(item.getMaxPrice())
                    .isBiddable(isBiddable)
                    .build();

            return Result.ok(response);

        } catch (Exception e) {
            log.error("查询拍品价格失败: auctionItemId={}, error={}", id, e.getMessage(), e);
            return Result.fail(500, "查询拍品价格失败: " + e.getMessage());
        }
    }

    /**
     * 添加拍品到活动
     * POST /auctions/{auctionId}/items
     */
    @PostMapping("/auction/{auctionId}/items")
    public Result<AuctionItem> addItemToAuction(
            @PathVariable Long auctionId,
            @RequestBody com.auction.api.dto.request.CreateAuctionRequest.AuctionItemRequest itemRequest,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("添加拍品到活动: auctionId={}, productId={}, userId={}",
                    auctionId, itemRequest.getProductId(), currentUser.getUserId());

            Auction auction = auctionRepository.findById(auctionId);
            if (auction == null) {
                return Result.fail(404, "活动不存在");
            }

            if (auction.getStatusEnum() != AuctionStatus.PENDING) {
                return Result.fail(400, "只有待开始状态的活动可以添加拍品");
            }

            // TODO: 实现添加拍品逻辑
            return Result.ok(new AuctionItem());

        } catch (Exception e) {
            log.error("添加拍品失败: auctionId={}, error={}", auctionId, e.getMessage(), e);
            return Result.fail(500, "添加拍品失败: " + e.getMessage());
        }
    }

    /**
     * 加载拍品的商品信息
     * 填充商品名称、图片URL和描述信息
     */
    private void loadProductInfo(List<AuctionItem> items) {
        for (AuctionItem item : items) {
            Product product = productRepository.findById(item.getProductId());
            if (product != null) {
                item.setProductName(product.getName());
                item.setProductImageUrl(product.getImageUrl());
                item.setDescription(product.getDescription());
            }
        }
    }

    /**
     * 构建活动信息（包含拍品列表）
     */
    private ActiveAuctionsResponse.AuctionInfo buildAuctionInfo(Auction auction, List<AuctionItem> items) {
        List<ActiveAuctionsResponse.ActiveItemInfo> itemInfos = items.stream()
                .map(this::buildActiveItemInfo)
                .collect(java.util.stream.Collectors.toList());

        return ActiveAuctionsResponse.AuctionInfo.builder()
                .auctionId(auction.getId())
                .auctionTitle(auction.getTitle())
                .auctionDescription(auction.getDescription())
                .items(itemInfos)
                .build();
    }

    /**
     * 构建活跃拍品信息（包含时间计算）
     */
    private ActiveAuctionsResponse.ActiveItemInfo buildActiveItemInfo(AuctionItem item) {
        // 计算时间信息
        Long remainingSeconds = null;
        String endTime = null;
        Long endTimeTimestamp = null;

        if (item.getEndTime() != null) {
            long secs = java.time.temporal.ChronoUnit.SECONDS.between(
                    LocalDateTime.now(), item.getEndTime());
            remainingSeconds = Math.max(0, secs);
            endTime = item.getEndTime().toString();
            endTimeTimestamp = item.getEndTime().atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
        }

        // 判断是否可出价
        boolean isBiddable = item.getStatusEnum() == AuctionStatus.ACTIVE &&
                (remainingSeconds == null || remainingSeconds > 0);

        return ActiveAuctionsResponse.ActiveItemInfo.builder()
                .itemId(item.getId())
                .title(item.getTitle())
                .productName(item.getProductName())
                .productImageUrl(item.getProductImageUrl())
                .description(item.getDescription())
                .currentPrice(item.getCurrentPrice())
                .startPrice(item.getStartPrice())
                .bidIncrement(item.getBidIncrement())
                .maxPrice(item.getMaxPrice())
                .highestBidder(item.getHighestBidder())
                .bidCount(item.getBidCount())
                .endTime(endTime)
                .endTimeTimestamp(endTimeTimestamp)
                .remainingSeconds(remainingSeconds)
                .isBiddable(isBiddable)
                .delayCount(item.getDelayCount())
                .build();
    }
}
