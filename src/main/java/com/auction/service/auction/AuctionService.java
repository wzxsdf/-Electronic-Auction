package com.auction.service.auction;

import com.auction.api.dto.request.CreateAuctionRequest;
import com.auction.api.dto.response.AuctionDetailResponse;
import com.auction.api.dto.response.AuctionStatisticsResponse;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.AuctionItem;
import com.auction.domain.entity.Product;
import com.auction.domain.enums.AuctionStatus;
import com.auction.domain.enums.ProductStatus;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.AuctionItemRepository;
import com.auction.repository.AuctionRepository;
import com.auction.repository.ProductRepository;
import com.auction.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 拍卖活动Service（重构后）
 * <p>
 * 实现活动级别的业务逻辑，支持多拍品管理
 * 提供完整的活动生命周期管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final ProductRepository productRepository;
    private final RedisService redisService;
    private final NotificationService notificationService;

    /**
     * 创建拍卖活动
     */
    @Transactional(rollbackFor = Exception.class)
    public AuctionDetailResponse createAuction(CreateAuctionRequest request, Long hostId) {
        log.info("创建拍卖活动: hostId={}, title={}, itemCount={}",
                hostId, request.getTitle(), request.getItems().size());

        // 1. 验证活动创建权限
        validateAuctionCreation(request, hostId);

        // 2. 验证商品并创建活动
        List<Long> productIds = request.getItems().stream()
                .map(CreateAuctionRequest.AuctionItemRequest::getProductId)
                .collect(Collectors.toList());

        List<Product> products = validateAndGetProducts(productIds);

        // 3. 创建活动主体
        Auction auction = new Auction();
        auction.setTitle(request.getTitle());
        auction.setDescription(request.getDescription());
        auction.setHostId(hostId);
        auction.setStartTime(request.getStartTime());
        auction.setEndTime(request.getEndTime());
        auction.setMinDeposit(request.getMinDeposit() != null ? request.getMinDeposit() : BigDecimal.ZERO);
        auction.setMaxItems(request.getMaxItems() != null ? request.getMaxItems() : 50);
        auction.setStatusEnum(AuctionStatus.PENDING);
        auction.setViewerCount(0);

        auction = auctionRepository.save(auction);
        log.info("活动创建成功: auctionId={}", auction.getId());

        // 4. 批量创建拍品
        List<AuctionItem> items = createAuctionItems(auction.getId(), request, products);
        log.info("批量创建拍品成功: auctionId={}, itemCount={}", auction.getId(), items.size());

        // 5. 缓存活动信息
        cacheAuctionInfo(auction, items);

        // 6. 返回详情
        return buildAuctionDetailResponse(auction, items);
    }

    /**
     * 查询活动详情
     */
    public AuctionDetailResponse getAuctionDetail(Long auctionId) {
        log.info("查询活动详情: auctionId={}", auctionId);

        // 尝试从缓存获取（使用泛型方法避免类型转换异常）
        String cacheKey = "auction:detail:" + auctionId;
        AuctionDetailResponse cached = redisService.get(cacheKey, AuctionDetailResponse.class);
        if (cached != null) {
            log.debug("从缓存获取活动详情: auctionId={}", auctionId);
            return cached;
        }

        // 从数据库查询
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);
        loadProductInfo(items);

        AuctionDetailResponse response = buildAuctionDetailResponse(auction, items);

        // 缓存结果（5分钟）
        redisService.set(cacheKey, response, 300, java.util.concurrent.TimeUnit.SECONDS);

        return response;
    }

    /**
     * 开始活动
     */
    @Transactional(rollbackFor = Exception.class)
    public void startAuction(Long auctionId, Long operatorId) {
        log.info("开始活动: auctionId={}, operatorId={}", auctionId, operatorId);

        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 验证权限
        if (!auction.getHostId().equals(operatorId)) {
            throw new BizException(ErrorCode.NO_PERMISSION, "只有创建者可以开始活动");
        }

        // 验证状态
        if (auction.getStatusEnum() != AuctionStatus.PENDING) {
            throw new BizException(ErrorCode.AUCTION_STATUS_INVALID, "活动状态不允许开始");
        }

        // 注释掉开始时间检查 - 允许管理员在任意时间手动启动拍卖活动
        // // 验证时间
        // if (LocalDateTime.now().isBefore(auction.getStartTime())) {
        //     throw new BizException(ErrorCode.AUCTION_NOT_STARTED, "活动尚未到开始时间");
        // }

        // 验证拍品
        List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);
        if (items.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "活动没有拍品，无法开始");
        }

        // 更新活动状态
        auction.setStatusEnum(AuctionStatus.ACTIVE);
        auctionRepository.updateById(auction);

        // 批量更新拍品状态（有独立开始时间的拍品除外）
        int updatedCount = 0;
        for (AuctionItem item : items) {
            if (item.getStartTime() == null || item.getStartTime().isBefore(LocalDateTime.now())) {
                item.setStatusEnum(AuctionStatus.ACTIVE);
                auctionItemRepository.updateById(item);
                updatedCount++;
            }
        }

        // 清除缓存
        clearAuctionCache(auctionId);

        // 通知关注者（异步执行，不影响主流程）
        try {
            notificationService.notifyAuctionStartedToFollower(auctionId);
        } catch (Exception e) {
            log.error("通知关注者失败: auctionId={}, error={}", auctionId, e.getMessage(), e);
            // 不影响主业务流程，仅记录日志
        }

        log.info("活动开始成功: auctionId={}, updatedItems={}", auctionId, updatedCount);
    }

    /**
     * 结束活动
     */
    @Transactional(rollbackFor = Exception.class)
    public void endAuction(Long auctionId, Long operatorId) {
        log.info("结束活动: auctionId={}, operatorId={}", auctionId, operatorId);

        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 验证权限
        if (!auction.getHostId().equals(operatorId)) {
            throw new BizException(ErrorCode.NO_PERMISSION, "只有创建者可以结束活动");
        }

        // 验证状态
        if (auction.getStatusEnum() != AuctionStatus.ACTIVE) {
            throw new BizException(ErrorCode.AUCTION_STATUS_INVALID, "活动状态不允许结束");
        }

        // 检查所有拍品是否已结束
        List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);
        boolean allEnded = items.stream()
                .allMatch(item -> item.getStatusEnum() == AuctionStatus.COMPLETED ||
                                item.getStatusEnum() == AuctionStatus.CANCELLED ||
                                item.getStatusEnum() == AuctionStatus.PAUSED); // 临时使用PAUSED表示流拍

        if (!allEnded) {
            throw new BizException(ErrorCode.BAD_REQUEST, "还有拍品进行中，无法结束活动");
        }

        // 更新活动状态
        auction.setStatusEnum(AuctionStatus.COMPLETED);
        auctionRepository.updateById(auction);

        // 清除缓存
        clearAuctionCache(auctionId);

        log.info("活动结束成功: auctionId={}", auctionId);
    }

    /**
     * 取消活动
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelAuction(Long auctionId, String reason, Long operatorId) {
        log.info("取消活动: auctionId={}, reason={}, operatorId={}", auctionId, reason, operatorId);

        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 验证权限
        if (!auction.getHostId().equals(operatorId)) {
            throw new BizException(ErrorCode.NO_PERMISSION, "只有创建者可以取消活动");
        }

        // 验证状态
        AuctionStatus currentStatus = auction.getStatusEnum();
        if (currentStatus == AuctionStatus.COMPLETED) {
            throw new BizException(ErrorCode.AUCTION_STATUS_INVALID, "已结束的活动无法取消");
        }

        // 更新活动状态
        auction.setStatusEnum(AuctionStatus.CANCELLED);
        auctionRepository.updateById(auction);

        // 取消所有进行中的拍品
        List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);
        for (AuctionItem item : items) {
            if (item.getStatusEnum() == AuctionStatus.ACTIVE ||
                item.getStatusEnum() == AuctionStatus.PENDING) {
                item.setStatusEnum(AuctionStatus.CANCELLED);
                auctionItemRepository.updateById(item);
            }
        }

        // 清除缓存
        clearAuctionCache(auctionId);

        log.info("活动取消成功: auctionId={}, reason={}", auctionId, reason);
    }

    /**
     * 更新活动信息
     */
    @Transactional(rollbackFor = Exception.class)
    public AuctionDetailResponse updateAuction(Long auctionId, CreateAuctionRequest request, Long operatorId) {
        log.info("更新活动信息: auctionId={}, operatorId={}", auctionId, operatorId);

        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 验证权限
        if (!auction.getHostId().equals(operatorId)) {
            throw new BizException(ErrorCode.NO_PERMISSION, "只有创建者可以更新活动");
        }

        // 验证状态
        if (auction.getStatusEnum() != AuctionStatus.PENDING) {
            throw new BizException(ErrorCode.AUCTION_STATUS_INVALID, "只有待开始状态的活动可以更新");
        }

        // 验证时间
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "结束时间不能早于开始时间");
        }

        // 更新基本信息
        auction.setTitle(request.getTitle());
        auction.setDescription(request.getDescription());
        auction.setStartTime(request.getStartTime());
        auction.setEndTime(request.getEndTime());
        auction.setMinDeposit(request.getMinDeposit());
        auction.setMaxItems(request.getMaxItems());
        auctionRepository.updateById(auction);

        // 清除缓存
        clearAuctionCache(auctionId);

        log.info("活动信息更新成功: auctionId={}", auctionId);

        return getAuctionDetail(auctionId);
    }

    /**
     * 删除活动
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAuction(Long auctionId, Long operatorId) {
        log.info("删除活动: auctionId={}, operatorId={}", auctionId, operatorId);

        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 验证权限
        if (!auction.getHostId().equals(operatorId)) {
            throw new BizException(ErrorCode.NO_PERMISSION, "只有创建者可以删除活动");
        }

        // 验证状态
        if (auction.getStatusEnum() != AuctionStatus.PENDING) {
            throw new BizException(ErrorCode.AUCTION_STATUS_INVALID, "只有待开始状态的活动可以删除");
        }

        // 级联删除拍品
        List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);
        for (AuctionItem item : items) {
            auctionItemRepository.deleteById(item.getId());
        }

        // 删除活动
        auctionRepository.deleteById(auctionId);

        // 清除缓存
        clearAuctionCache(auctionId);

        log.info("活动删除成功: auctionId={}, deletedItems={}", auctionId, items.size());
    }

    /**
     * 延长活动时间
     */
    @Transactional(rollbackFor = Exception.class)
    public void extendAuction(Long auctionId, Integer extendMinutes, Long operatorId) {
        log.info("延长活动时间: auctionId={}, extendMinutes={}, operatorId={}",
                auctionId, extendMinutes, operatorId);

        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 验证状态
        if (auction.getStatusEnum() != AuctionStatus.ACTIVE) {
            throw new BizException(ErrorCode.AUCTION_STATUS_INVALID, "只有进行中的活动可以延长");
        }

        // 验证延长时长
        if (extendMinutes <= 0 || extendMinutes > 120) {
            throw new BizException(ErrorCode.BAD_REQUEST, "延长时长必须在1-120分钟之间");
        }

        // 延长活动结束时间
        LocalDateTime newEndTime = auction.getEndTime().plusMinutes(extendMinutes);
        auction.setEndTime(newEndTime);
        auctionRepository.updateById(auction);

        // 延长所有未结束拍品的时间
        List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);
        int extendedCount = 0;
        for (AuctionItem item : items) {
            if (item.getStatusEnum() == AuctionStatus.ACTIVE && item.getEndTime() != null) {
                item.setEndTime(item.getEndTime().plusMinutes(extendMinutes));
                auctionItemRepository.updateById(item);
                extendedCount++;
            }
        }

        // 清除缓存
        clearAuctionCache(auctionId);

        log.info("活动时间延长成功: auctionId={}, newEndTime={}, extendedItems={}",
                auctionId, newEndTime, extendedCount);
    }

    /**
     * 获取活动统计
     */
    public AuctionStatisticsResponse getStatistics(Long auctionId) {
        log.info("获取活动统计: auctionId={}", auctionId);

        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        List<AuctionItem> items = auctionItemRepository.findByAuctionId(auctionId);

        // 计算统计数据
        AuctionStatisticsResponse.ItemStatistics itemStats = calculateItemStatistics(items);
        AuctionStatisticsResponse.BidStatistics bidStats = calculateBidStatistics(items);
        AuctionStatisticsResponse.ParticipantStatistics participantStats = calculateParticipantStatistics(items);
        AuctionStatisticsResponse.TimeStatistics timeStats = calculateTimeStatistics(auction);

        return AuctionStatisticsResponse.builder()
                .auctionId(auction.getId())
                .title(auction.getTitle())
                .statisticsTime(LocalDateTime.now())
                .itemStats(itemStats)
                .bidStats(bidStats)
                .participantStats(participantStats)
                .timeStats(timeStats)
                .build();
    }

    // ==================== 私有方法 ====================

    /**
     * 验证活动创建请求
     */
    private void validateAuctionCreation(CreateAuctionRequest request, Long hostId) {
        // 验证时间
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "结束时间不能早于开始时间");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.getStartTime().isBefore(now.plusMinutes(10))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "开始时间至少在10分钟后");
        }

        long durationMinutes = ChronoUnit.MINUTES.between(request.getStartTime(), request.getEndTime());
        if (durationMinutes < 30) {
            throw new BizException(ErrorCode.BAD_REQUEST, "活动时长至少30分钟");
        }
        if (durationMinutes > 10080) { // 7天
            throw new BizException(ErrorCode.BAD_REQUEST, "活动时长不能超过7天");
        }

        // 验证拍品数量
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "至少添加1个拍品");
        }
        if (request.getItems().size() > 50) {
            throw new BizException(ErrorCode.BAD_REQUEST, "最多添加50个拍品");
        }
    }

    /**
     * 验证并获取商品列表
     */
    private List<Product> validateAndGetProducts(List<Long> productIds) {
        List<Product> products = new ArrayList<>();

        for (Long productId : productIds) {
            Product product = productRepository.findById(productId);
            if (product == null) {
                throw new BizException(ErrorCode.PRODUCT_NOT_FOUND, "商品不存在: " + productId);
            }

            if (product.getStatus() != ProductStatus.LISTED) {
                throw new BizException(ErrorCode.BAD_REQUEST, "商品未上架: " + productId);
            }

            products.add(product);
        }

        return products;
    }

    /**
     * 批量创建拍品
     */
    private List<AuctionItem> createAuctionItems(Long auctionId,
                                                  CreateAuctionRequest request,
                                                  List<Product> products) {
        List<AuctionItem> items = new ArrayList<>();

        for (int i = 0; i < request.getItems().size(); i++) {
            CreateAuctionRequest.AuctionItemRequest itemRequest = request.getItems().get(i);
            Product product = products.get(i);

            AuctionItem item = new AuctionItem();
            item.setAuctionId(auctionId);
            item.setProductId(product.getId());
            item.setTitle(itemRequest.getTitle() != null ?
                    itemRequest.getTitle() : product.getName());
            // 起拍价优先使用request中的值，否则使用product的initialPrice
            item.setStartPrice(itemRequest.getStartPrice() != null ?
                    itemRequest.getStartPrice() : product.getInitialPrice());
            // 加价幅度优先使用product中的值，否则使用request中的值
            item.setBidIncrement(product.getBidIncrement() != null ?
                    product.getBidIncrement() : itemRequest.getBidIncrement());
            // 封顶价格优先使用product中的值，否则使用request中的值
            item.setMaxPrice(product.getMaxPrice() != null ?
                    product.getMaxPrice() : itemRequest.getMaxPrice());
            item.setDelaySeconds(itemRequest.getDelaySeconds() != null ?
                    itemRequest.getDelaySeconds() : request.getDefaultDelaySeconds());
            item.setStartTime(itemRequest.getStartTime() != null ?
                    itemRequest.getStartTime() : request.getStartTime());
            item.setEndTime(itemRequest.getEndTime() != null ?
                    itemRequest.getEndTime() : request.getEndTime());
            item.setOriginalEndTime(itemRequest.getEndTime() != null ?
                    itemRequest.getEndTime() : request.getEndTime());
            item.setCurrentPrice(itemRequest.getStartPrice());
            item.setStatusEnum(AuctionStatus.PENDING);
            item.setBidCount(0);
            item.setDisplayOrder(itemRequest.getDisplayOrder() != null ?
                    itemRequest.getDisplayOrder() : (i + 1));

            auctionItemRepository.save(item);
            items.add(item);

            log.debug("创建拍品: itemId={}, productId={}, title={}",
                    item.getId(), product.getId(), item.getTitle());
        }

        return items;
    }

    /**
     * 加载商品信息到拍品
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
     * 缓存活动信息
     */
    private void cacheAuctionInfo(Auction auction, List<AuctionItem> items) {
        String cacheKey = "auction:detail:" + auction.getId();
        AuctionDetailResponse response = buildAuctionDetailResponse(auction, items);
        redisService.set(cacheKey, response, 300, java.util.concurrent.TimeUnit.SECONDS); // 5分钟
    }

    /**
     * 清除活动缓存
     */
    private void clearAuctionCache(Long auctionId) {
        String cacheKey = "auction:detail:" + auctionId;
        redisService.delete(cacheKey);
    }

    /**
     * 构建活动详情响应
     */
    private AuctionDetailResponse buildAuctionDetailResponse(Auction auction, List<AuctionItem> items) {
        AuctionDetailResponse response = new AuctionDetailResponse();

        // 基本信息
        response.setId(auction.getId());
        response.setTitle(auction.getTitle());
        response.setDescription(auction.getDescription());
        response.setHostId(auction.getHostId());
        response.setStatus(auction.getStatus());
        response.setStartTime(auction.getStartTime());
        response.setEndTime(auction.getEndTime());
        response.setMinDeposit(auction.getMinDeposit());
        response.setMaxItems(auction.getMaxItems());
        response.setViewerCount(auction.getViewerCount());
        response.setCreatedAt(auction.getCreatedAt());
        response.setUpdatedAt(auction.getUpdatedAt());

        // 拍品列表
        loadProductInfo(items);
        response.setItems(items.stream()
                .map(this::buildItemResponse)
                .collect(Collectors.toList()));

        // 统计信息
        response.setStatistics(calculateSimpleStatistics(items));

        return response;
    }

    /**
     * 构建拍品响应
     */
    private AuctionDetailResponse.AuctionItemResponse buildItemResponse(AuctionItem item) {
        AuctionDetailResponse.AuctionItemResponse response = new AuctionDetailResponse.AuctionItemResponse();

        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setTitle(item.getTitle());
        response.setProductName(item.getProductName());
        response.setProductImageUrl(item.getProductImageUrl());
        response.setProductDescription(item.getDescription());
        response.setStartPrice(item.getStartPrice());
        response.setBidIncrement(item.getBidIncrement());
        response.setMaxPrice(item.getMaxPrice());
        response.setDelaySeconds(item.getDelaySeconds());
        response.setStartTime(item.getStartTime());
        response.setEndTime(item.getEndTime());
        response.setOriginalEndTime(item.getOriginalEndTime());
        response.setCurrentPrice(item.getCurrentPrice());
        response.setHighestBidder(item.getHighestBidder());
        response.setStatus(item.getStatus());
        response.setBidCount(item.getBidCount());
        response.setDisplayOrder(item.getDisplayOrder());
        response.setDelayCount(item.getDelayCount());

        // 计算可出价状态
        boolean isBiddable = item.getStatusEnum() == AuctionStatus.ACTIVE &&
                (item.getEndTime() == null || item.getEndTime().isAfter(LocalDateTime.now()));
        response.setIsBiddable(isBiddable);

        // 计算剩余时间
        if (item.getEndTime() != null) {
            long remaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), item.getEndTime());
            response.setRemainingSeconds(Math.max(0, remaining));
        }

        return response;
    }

    /**
     * 计算简单统计信息
     */
    private AuctionDetailResponse.AuctionStatistics calculateSimpleStatistics(List<AuctionItem> items) {
        AuctionDetailResponse.AuctionStatistics stats = new AuctionDetailResponse.AuctionStatistics();

        stats.setTotalItems(items.size());
        stats.setActiveItems((int) items.stream()
                .filter(item -> item.getStatusEnum() == AuctionStatus.ACTIVE)
                .count());
        stats.setSoldItems((int) items.stream()
                .filter(item -> item.getStatusEnum() == AuctionStatus.COMPLETED)
                .count());
        stats.setUnsoldItems((int) items.stream()
                .filter(item -> item.getStatusEnum() == AuctionStatus.PAUSED) // 临时使用PAUSED表示流拍
                .count());
        stats.setTotalBids((long) items.stream()
                .mapToInt(item -> item.getBidCount() != null ? item.getBidCount() : 0)
                .sum());
        stats.setParticipantCount(0); // TODO: 从出价记录计算

        return stats;
    }

    /**
     * 计算拍品统计
     */
    private AuctionStatisticsResponse.ItemStatistics calculateItemStatistics(List<AuctionItem> items) {
        int totalItems = items.size();
        int soldItems = (int) items.stream()
                .filter(item -> item.getStatusEnum() == AuctionStatus.COMPLETED)
                .count();
        int unsoldItems = (int) items.stream()
                .filter(item -> item.getStatusEnum() == AuctionStatus.PAUSED) // 临时使用PAUSED表示流拍
                .count();
        int activeItems = (int) items.stream()
                .filter(item -> item.getStatusEnum() == AuctionStatus.ACTIVE)
                .count();

        BigDecimal successRate = totalItems > 0 ?
                BigDecimal.valueOf(soldItems * 100.0 / totalItems) : BigDecimal.ZERO;

        return AuctionStatisticsResponse.ItemStatistics.builder()
                .totalItems(totalItems)
                .activeItems(activeItems)
                .soldItems(soldItems)
                .unsoldItems(unsoldItems)
                .successRate(successRate)
                .build();
    }

    /**
     * 计算出价统计
     */
    private AuctionStatisticsResponse.BidStatistics calculateBidStatistics(List<AuctionItem> items) {
        long totalBids = items.stream()
                .mapToInt(item -> item.getBidCount() != null ? item.getBidCount() : 0)
                .sum();

        BigDecimal averageBidAmount = items.stream()
                .map(AuctionItem::getCurrentPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(totalBids, 1)), 2, BigDecimal.ROUND_HALF_UP);

        return AuctionStatisticsResponse.BidStatistics.builder()
                .totalBids(totalBids)
                .averageBidAmount(averageBidAmount)
                .build();
    }

    /**
     * 计算参与者统计
     */
    private AuctionStatisticsResponse.ParticipantStatistics calculateParticipantStatistics(List<AuctionItem> items) {
        // TODO: 从出价记录表计算实际参与人数
        return AuctionStatisticsResponse.ParticipantStatistics.builder()
                .totalParticipants(0)
                .onlineParticipants(0)
                .biddingUsers(0)
                .watchingUsers(0)
                .build();
    }

    /**
     * 计算时间统计
     */
    private AuctionStatisticsResponse.TimeStatistics calculateTimeStatistics(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = 0;
        long remainingSeconds = 0;
        boolean isEnded = false;

        if (auction.getStatusEnum() == AuctionStatus.COMPLETED ||
            auction.getStatusEnum() == AuctionStatus.CANCELLED) {
            isEnded = true;
            elapsedSeconds = ChronoUnit.SECONDS.between(auction.getStartTime(), auction.getEndTime());
        } else if (auction.getStatusEnum() == AuctionStatus.ACTIVE) {
            elapsedSeconds = ChronoUnit.SECONDS.between(auction.getStartTime(), now);
            remainingSeconds = ChronoUnit.SECONDS.between(now, auction.getEndTime());
        }

        return AuctionStatisticsResponse.TimeStatistics.builder()
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .elapsedSeconds(elapsedSeconds)
                .remainingSeconds(Math.max(0, remainingSeconds))
                .isEnded(isEnded)
                .delayCount(auction.getDelayCount())
                .build();
    }
}
