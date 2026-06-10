package com.auction.service.auction;

import com.auction.api.dto.request.PlaceBidRequest;
import com.auction.api.dto.response.BidResultResponse;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.AuctionItem;
import com.auction.domain.entity.AutoBidConfig;
import com.auction.domain.entity.Bid;
import com.auction.domain.entity.User;
import com.auction.domain.enums.AuctionStatus;
import com.auction.domain.enums.AutoBidStrategy;
import com.auction.domain.enums.AutoBidConfigStatus;
import com.auction.infrastructure.lock.DistributedLockService;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.*;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 拍品Service（重构后）
 * <p>
 * 实现拍品级别的业务逻辑
 * 支持出价、自动出价、延时机制等核心功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionItemService {

    private final AuctionItemRepository auctionItemRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AutoBidConfigRepository autoBidConfigRepository;
    private final WsMessageService wsMessageService;
    private final RedisService redisService;
    private final DistributedLockService distributedLockService;
    private final com.auction.service.order.OrderService orderService;

    /**
     * 对拍品出价
     */
    public BidResultResponse placeBid(PlaceBidRequest request) {
        // 确定要出价的拍品ID
        Long targetItemId = request.getAuctionItemId() != null ? request.getAuctionItemId() : request.getAuctionId();

        log.info("拍品出价: auctionItemId={}, userId={}, amount={}",
                targetItemId, request.getUserId(), request.getAmount());

        // 使用分布式锁保护出价过程
        String lockKey = DistributedLockService.auctionItemLockKey(targetItemId);

        return distributedLockService.executeWithLock(lockKey, () -> {
            return doPlaceBid(request, targetItemId);
        });
    }

    /**
     * 设置自动出价
     */
    @Transactional(rollbackFor = Exception.class)
    public void setAutoBid(Long auctionItemId, Long userId, BigDecimal maxPrice, AutoBidStrategy strategy) {
        log.info("设置自动出价: auctionItemId={}, userId={}, maxPrice={}, strategy={}",
                auctionItemId, userId, maxPrice, strategy);

        // 验证拍品
        AuctionItem item = auctionItemRepository.findById(auctionItemId);
        if (item == null) {
            throw new BizException(ErrorCode.AUCTION_ITEM_NOT_FOUND);
        }

        if (item.getStatusEnum() != AuctionStatus.ACTIVE && item.getStatusEnum() != AuctionStatus.PENDING) {
            throw new BizException(ErrorCode.BAD_REQUEST, "拍品状态不允许设置自动出价");
        }

        // 验证价格
        if (maxPrice.compareTo(item.getStartPrice()) < 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "最高价不能低于起拍价");
        }

        if (item.getMaxPrice() != null && maxPrice.compareTo(item.getMaxPrice()) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "最高价不能超过封顶价");
        }

        // 检查是否已存在自动出价配置
        AutoBidConfig existingConfig = autoBidConfigRepository.findByUserAndItem(userId, auctionItemId);
        if (existingConfig != null) {
            // 更新现有配置
            existingConfig.setMaxPrice(maxPrice);
            existingConfig.setStrategy(strategy.name());
            existingConfig.setStatus(AutoBidConfigStatus.ACTIVE.name());
            autoBidConfigRepository.save(existingConfig);
            log.info("更新自动出价配置: configId={}", existingConfig.getId());
        } else {
            // 创建新配置
            AutoBidConfig config = new AutoBidConfig();
            config.setUserId(userId);
            config.setAuctionItemId(auctionItemId);
            config.setMaxPrice(maxPrice);
            config.setStrategy(strategy.name());
            config.setStatus(AutoBidConfigStatus.ACTIVE.name());
            config.setBidCount(0);
            autoBidConfigRepository.save(config);
            log.info("创建自动出价配置: configId={}", config.getId());
        }

        // 如果拍品正在进行中，立即执行一次自动出价
        if (item.getStatusEnum() == AuctionStatus.ACTIVE) {
            executeAutoBid(item, userId, maxPrice, strategy);
        }
    }

    /**
     * 批量结算到期拍品
     * 定时任务调用此方法
     */
    @Transactional(rollbackFor = Exception.class)
    public int settleExpiredItems() {
        log.info("开始批量结算到期拍品");

        LocalDateTime now = LocalDateTime.now();
        List<AuctionItem> expiredItems = auctionItemRepository.findExpiredActiveItems(now);

        if (expiredItems.isEmpty()) {
            log.debug("无到期拍品需要结算");
            return 0;
        }

        int settledCount = 0;
        for (AuctionItem item : expiredItems) {
            try {
                endItem(item.getId(), 0L); // 系统自动结算，operatorId设为0
                settledCount++;
                log.info("拍品自动结算成功: auctionItemId={}", item.getId());
            } catch (Exception e) {
                log.error("拍品自动结算失败: auctionItemId={}", item.getId(), e);
            }
        }

        log.info("批量结算完成: 总数={}, 成功={}", expiredItems.size(), settledCount);
        return settledCount;
    }

    /**
     * 批量自动开始待开始拍品
     * 定时任务调用此方法
     */
    @Transactional(rollbackFor = Exception.class)
    public int startPendingItems() {
        log.info("开始批量自动开始拍品");

        LocalDateTime now = LocalDateTime.now();
        List<AuctionItem> pendingItems = auctionItemRepository.findPendingItemsToStart(now);

        if (pendingItems.isEmpty()) {
            log.debug("无待开始拍品");
            return 0;
        }

        int startedCount = 0;
        for (AuctionItem item : pendingItems) {
            try {
                // 验证所属活动状态
                Auction auction = auctionRepository.findById(item.getAuctionId());
                if (auction != null && auction.getStatusEnum() == AuctionStatus.ACTIVE) {
                    startItem(item.getId(), 0L); // 系统自动开始，operatorId设为0
                    startedCount++;
                    log.info("拍品自动开始成功: auctionItemId={}", item.getId());
                }
            } catch (Exception e) {
                log.error("拍品自动开始失败: auctionItemId={}", item.getId(), e);
            }
        }

        log.info("批量自动开始完成: 总数={}, 成功={}", pendingItems.size(), startedCount);
        return startedCount;
    }

    /**
     * 取消自动出价
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelAutoBid(Long auctionItemId, Long userId) {
        log.info("取消自动出价: auctionItemId={}, userId={}", auctionItemId, userId);

        AutoBidConfig config = autoBidConfigRepository.findByUserAndItem(userId, auctionItemId);
        if (config == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "未找到自动出价配置");
        }

        config.setStatus(AutoBidConfigStatus.CANCELLED.name());
        autoBidConfigRepository.save(config);

        log.info("自动出价已取消: configId={}", config.getId());
    }

    /**
     * 开始单个拍品（内部方法，使用默认持续时间）
     * <p>
     * 用于定时任务等内部调用，使用默认持续时间60分钟
     */
    @Transactional(rollbackFor = Exception.class)
    public void startItem(Long auctionItemId, Long operatorId) {
        com.auction.api.dto.request.StartAuctionItemRequest request = new com.auction.api.dto.request.StartAuctionItemRequest();
        request.setDurationMinutes(60);  // 默认1小时
        startItem(auctionItemId, operatorId, request);
    }

    /**
     * 开始单个拍品（完整方法）
     */
    @Transactional(rollbackFor = Exception.class)
    public void startItem(Long auctionItemId, Long operatorId, com.auction.api.dto.request.StartAuctionItemRequest request) {
        log.info("开始拍品: auctionItemId={}, operatorId={}, durationMinutes={}",
                auctionItemId, operatorId, request.getDurationMinutes());

        AuctionItem item = auctionItemRepository.findById(auctionItemId);
        if (item == null) {
            throw new BizException(ErrorCode.AUCTION_ITEM_NOT_FOUND);
        }

        // 验证所属活动状态
        Auction auction = auctionRepository.findById(item.getAuctionId());
        if (auction == null || auction.getStatusEnum() != AuctionStatus.ACTIVE) {
            throw new BizException(ErrorCode.BAD_REQUEST, "所属活动未开始");
        }

        // 验证拍品状态
        if (item.getStatusEnum() != AuctionStatus.PENDING) {
            throw new BizException(ErrorCode.BAD_REQUEST, "拍品状态不允许开始");
        }

        // 注释掉开始时间检查 - 允许管理员在任意时间手动启动拍品
        // // 验证时间 - 如果拍品设置了startTime，检查是否已到开始时间
        // if (item.getStartTime() != null && item.getStartTime().isAfter(LocalDateTime.now())) {
        //     throw new BizException(ErrorCode.BAD_REQUEST, "拍品尚未到开始时间");
        // }

        // 计算并设置结束时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusMinutes(request.getDurationMinutes());

        // 设置拍品时间字段
        item.setStartTime(now);  // 记录实际开始时间
        item.setEndTime(endTime);
        item.setOriginalEndTime(endTime);  // 保存原始结束时间（不因延时而变化）
        item.setDelayCount(0);  // 重置延时计数

        // 处理延时秒数覆盖
        if (request.getOverrideDelaySeconds() != null) {
            item.setDelaySeconds(request.getOverrideDelaySeconds());
            log.info("覆盖延时秒数: auctionItemId={}, delaySeconds={}",
                    auctionItemId, request.getOverrideDelaySeconds());
        }

        // 验证延时秒数已设置
        if (item.getDelaySeconds() == null) {
            item.setDelaySeconds(15);  // 设置默认值15秒
            log.warn("拍品未设置延时秒数，使用默认值15秒: auctionItemId={}", auctionItemId);
        }

        // 更新拍品状态
        item.setStatusEnum(AuctionStatus.ACTIVE);
        item.setCurrentPrice(item.getStartPrice());
        auctionItemRepository.updateById(item);

        // 触发自动出价
        triggerAutoBids(item);

        // WebSocket通知
        wsMessageService.sendAuctionItemStarted(auctionItemId, endTime);

        log.info("拍品开始成功: auctionItemId={}, startTime={}, endTime={}, durationMinutes={}",
                auctionItemId, now, endTime, request.getDurationMinutes());
    }

    /**
     * 结束拍品
     */
    @Transactional(rollbackFor = Exception.class)
    public void endItem(Long auctionItemId, Long operatorId) {
        log.info("结束拍品: auctionItemId={}, operatorId={}", auctionItemId, operatorId);

        AuctionItem item = auctionItemRepository.findById(auctionItemId);
        if (item == null) {
            throw new BizException(ErrorCode.AUCTION_ITEM_NOT_FOUND);
        }

        // 验证状态
        if (item.getStatusEnum() != AuctionStatus.ACTIVE) {
            throw new BizException(ErrorCode.BAD_REQUEST, "拍品状态不允许结束");
        }

        // 确定获胜者
        if (item.getHighestBidder() != null && item.getCurrentPrice() != null &&
            item.getCurrentPrice().compareTo(item.getStartPrice()) > 0) {
            // 有成交
            item.setStatusEnum(AuctionStatus.COMPLETED);

            // 生成订单
            createOrderForAuctionItem(item);

            // 获取中标者信息用于通知
            User winner = userRepository.findById(item.getHighestBidder());
            String winnerUsername = winner != null ? winner.getNickname() : "用户***";

            // WebSocket通知 - 包含中标者信息
            wsMessageService.sendAuctionItemEnded(auctionItemId, item.getHighestBidder(),
                    item.getCurrentPrice(), winnerUsername, true);

            log.info("拍品已成交: auctionItemId={}, winnerId={}, winnerUsername={}, finalPrice={}",
                    auctionItemId, item.getHighestBidder(), winnerUsername, item.getCurrentPrice());
        } else {
            // 流拍
            item.setStatusEnum(AuctionStatus.PAUSED); // 临时使用PAUSED表示流拍

            // WebSocket通知
            wsMessageService.sendAuctionItemEnded(auctionItemId, null,
                    item.getStartPrice(), null, false);

            log.info("拍品流拍: auctionItemId={}", auctionItemId);
        }

        auctionItemRepository.updateById(item);

        // 取消该拍品的自动出价配置
        cancelAutoBidConfigs(auctionItemId);
    }

    // ==================== 私有方法 ====================

    /**
     * 执行出价逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    protected BidResultResponse doPlaceBid(PlaceBidRequest request, Long targetItemId) {
        // 1. 获取拍品信息
        AuctionItem item = auctionItemRepository.findById(targetItemId);
        if (item == null) {
            throw new BizException(ErrorCode.AUCTION_ITEM_NOT_FOUND);
        }

        // 2. 获取用户信息
        User user = userRepository.findById(request.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. 验证出价
        validateBid(item, user, request.getAmount());

        // 4. 记录之前的状态
        BigDecimal previousPrice = item.getCurrentPrice();
        Long previousBidder = item.getHighestBidder();

        // 5. 更新拍品状态
        item.setCurrentPrice(request.getAmount());
        item.setHighestBidder(request.getUserId());
        item.setBidCount((item.getBidCount() != null ? item.getBidCount() : 0) + 1);
        auctionItemRepository.updateById(item);

        // 6. 保存出价记录
        Bid bid = new Bid();
        bid.setAuctionItemId(targetItemId);
        bid.setAuctionId(item.getAuctionId());
        bid.setUserId(request.getUserId());
        bid.setAmount(request.getAmount());
        bid.setStatus("ACTIVE");
        bid.setIsAutoBid(request.getIsAutoBid() != null && request.getIsAutoBid());
        bidRepository.save(bid);

        // 7. 更新Redis缓存
        updateItemPriceCache(item);

        // 8. 触发延时机制
        checkAndTriggerDelay(item);

        // 9. WebSocket通知
        notifyBidSuccess(item, bid, user, previousBidder);

        // 10. 触发其他用户的自动出价
        triggerAutoBids(item);

        log.info("出价成功: auctionItemId={}, userId={}, amount={}, bidId={}",
                item.getId(), request.getUserId(), request.getAmount(), bid.getId());

        // 计算用户排名和是否领先
        int userRank = calculateUserRank(item.getId(), request.getUserId());
        boolean isLeading = userRank == 1;

        // 准备时间信息
        String endTime = null;
        Long endTimeTimestamp = null;
        Long remainingMs = calculateRemainingMs(item);

        if (item.getEndTime() != null) {
            endTime = item.getEndTime().toString(); // ISO 8601格式
            endTimeTimestamp = item.getEndTime().atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli(); // 转换为时间戳
        }

        return BidResultResponse.builder()
                .bidId(bid.getId())
                .currentPrice(item.getCurrentPrice())
                .yourRank(userRank)
                .isLeading(isLeading)
                .endTime(endTime) // 绝对时间（ISO格式）
                .endTimeTimestamp(endTimeTimestamp) // 绝对时间戳
                .remainingMs(remainingMs) // 兼容性字段
                .message("出价成功")
                .build();
    }

    /**
     * 验证出价
     */
    private void validateBid(AuctionItem item, User user, BigDecimal amount) {
        // 验证拍品状态
        if (item.getStatusEnum() != AuctionStatus.ACTIVE) {
            throw new BizException(ErrorCode.AUCTION_STATUS_INVALID, "拍品未在进行中");
        }

        // 验证用户状态
        if (user.getStatus() == null || !user.getStatus().equals("ACTIVE")) {
            throw new BizException(ErrorCode.USER_DISABLED, "用户状态异常");
        }

        // 验证是否是当前最高出价者
        if (item.getHighestBidder() != null && item.getHighestBidder().equals(user.getId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "您当前是最高出价者，无需再次出价");
        }

        // 验证出价金额
        BigDecimal minPrice = item.getCurrentPrice().add(item.getBidIncrement());
        if (amount.compareTo(minPrice) < 0) {
            throw new BizException(ErrorCode.BID_AMOUNT_TOO_LOW,
                    String.format("出价必须 >= 当前价 + 加价幅度 = %s", minPrice));
        }

        // 验证封顶价
        if (item.getMaxPrice() != null && amount.compareTo(item.getMaxPrice()) > 0) {
            throw new BizException(ErrorCode.BID_EXCEED_MAX_PRICE,
                    String.format("出价不能超过封顶价 %s", item.getMaxPrice()));
        }

        // 验证时间
        if (item.getEndTime() != null && item.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.AUCTION_ALREADY_ENDED, "拍品已结束");
        }
    }

    /**
     * 检查并触发延时机制
     */
    private void checkAndTriggerDelay(AuctionItem item) {
        if (item.getEndTime() == null || item.getDelaySeconds() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime delayThreshold = item.getEndTime().minusSeconds(item.getDelaySeconds());

        if (now.isAfter(delayThreshold)) {
            // 触发延时
            int maxDelayCount = 3;
            if (item.getDelayCount() < maxDelayCount) {
                triggerDelay(item);
            }
        }
    }

    /**
     * 触发延时
     */
    private void triggerDelay(AuctionItem item) {
        log.info("触发延时: auctionItemId={}, delayCount={}", item.getId(), item.getDelayCount());

        // 延长结束时间
        LocalDateTime newEndTime = item.getEndTime().plusSeconds(item.getDelaySeconds());
        item.setEndTime(newEndTime);
        item.setDelayCount((item.getDelayCount() != null ? item.getDelayCount() : 0) + 1);
        auctionItemRepository.updateById(item);

        // 更新缓存
        String cacheKey = "auction:item:delay:" + item.getId();
        redisService.set(cacheKey, item.getDelayCount(), 3600, TimeUnit.SECONDS);

        // WebSocket通知
        wsMessageService.sendAuctionItemDelay(item.getId(), newEndTime, item.getDelayCount());

        log.info("延时成功: auctionItemId={}, newEndTime={}, delayCount={}",
                item.getId(), newEndTime, item.getDelayCount());
    }

    /**
     * 更新拍品价格缓存
     */
    private void updateItemPriceCache(AuctionItem item) {
        String priceKey = "auction:item:price:" + item.getId();
        String bidderKey = "auction:item:bidder:" + item.getId();

        redisService.set(priceKey, item.getCurrentPrice(), 3600, TimeUnit.SECONDS);
        redisService.set(bidderKey, item.getHighestBidder(), 3600, TimeUnit.SECONDS);
    }

    /**
     * 通知出价成功
     */
    private void notifyBidSuccess(AuctionItem item, Bid bid, User user, Long previousBidder) {
        // 通知当前出价者
        String username = user.getNickname() != null ? user.getNickname() : "用户***";
        wsMessageService.broadcastNewBid(item, bid, username, 1);
        wsMessageService.broadcastItemPriceUpdate(item.getId());

        // 通知被超越者
        if (previousBidder != null && !previousBidder.equals(bid.getUserId())) {
            wsMessageService.sendYouWereOvertaken(previousBidder, item.getId(), bid.getAmount());
        }

        // 通知当前领先者
        wsMessageService.sendYouAreLeading(bid.getUserId(), item.getId(), bid.getAmount());
    }

    /**
     * 触发自动出价
     */
    private void triggerAutoBids(AuctionItem item) {
        // 获取该拍品的所有活跃自动出价配置
        List<AutoBidConfig> configs = autoBidConfigRepository.findActiveByItemId(item.getId());

        for (AutoBidConfig config : configs) {
            executeAutoBid(item, config.getUserId(), config.getMaxPrice(),
                    AutoBidStrategy.valueOf(config.getStrategy()));
        }
    }

    /**
     * 执行自动出价
     */
    private void executeAutoBid(AuctionItem item, Long userId, BigDecimal maxPrice, AutoBidStrategy strategy) {
        // 检查用户是否已经是最高出价者
        if (item.getHighestBidder() != null && item.getHighestBidder().equals(userId)) {
            return; // 已经是最高价，无需自动出价
        }

        // 计算自动出价金额
        BigDecimal autoBidAmount = calculateAutoBidAmount(item, maxPrice, strategy);

        if (autoBidAmount == null) {
            log.info("自动出价取消: 出价已超过最高价，userId={}, maxPrice={}, currentPrice={}",
                    userId, maxPrice, item.getCurrentPrice());
            return;
        }

        // 执行自动出价
        PlaceBidRequest request = new PlaceBidRequest();
        request.setAuctionItemId(item.getId());
        request.setUserId(userId);
        request.setAmount(autoBidAmount);
        request.setIsAutoBid(true);

        try {
            doPlaceBid(request, item.getId());
            log.info("自动出价成功: userId={}, amount={}, strategy={}",
                    userId, autoBidAmount, strategy);
        } catch (Exception e) {
            log.warn("自动出价失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 计算自动出价金额
     */
    private BigDecimal calculateAutoBidAmount(AuctionItem item, BigDecimal maxPrice, AutoBidStrategy strategy) {
        BigDecimal currentPrice = item.getCurrentPrice();
        BigDecimal minBidAmount = currentPrice.add(item.getBidIncrement());

        // 检查最低出价是否超过最高价
        if (minBidAmount.compareTo(maxPrice) > 0) {
            return null; // 超过最高价，不出价
        }

        return switch (strategy) {
            case AGGRESSIVE -> minBidAmount; // 激进型：立即出最低价
            case SMART -> minBidAmount.add(item.getBidIncrement()); // 智能型：加两档
            case LAST_SEC -> {
                // 保守型：仅在最后阶段出价
                if (item.getEndTime() != null) {
                    long remainingSeconds = java.time.Duration.between(
                            LocalDateTime.now(), item.getEndTime()).getSeconds();
                    if (remainingSeconds <= 60) { // 最后1分钟
                        yield minBidAmount;
                    }
                }
                yield null;
            }
        };
    }

    /**
     * 取消自动出价配置
     */
    private void cancelAutoBidConfigs(Long auctionItemId) {
        List<AutoBidConfig> configs = autoBidConfigRepository.findActiveByItemId(auctionItemId);
        for (AutoBidConfig config : configs) {
            config.setStatus(AutoBidConfigStatus.CANCELLED.name());
            autoBidConfigRepository.save(config);
        }
        log.info("取消自动出价配置: auctionItemId={}, count={}", auctionItemId, configs.size());
    }

    /**
     * 为成交的拍品创建订单
     */
    private void createOrderForAuctionItem(AuctionItem item) {
        try {
            // 创建订单对象
            com.auction.domain.entity.Order order = new com.auction.domain.entity.Order();
            order.setAuctionId(item.getAuctionId());
            order.setAuctionItemId(item.getId());
            order.setUserId(item.getHighestBidder());
            order.setProductId(item.getProductId());
            order.setFinalAmount(item.getCurrentPrice());

            // 计算保证金和应付金额（暂时设为0，后续可根据业务规则调整）
            order.setDepositAmount(BigDecimal.ZERO);
            order.setPayableAmount(item.getCurrentPrice());

            // 生成订单
            com.auction.domain.entity.Order createdOrder = orderService.createOrder(order);

            log.info("订单生成成功: orderId={}, auctionItemId={}, userId={}, amount={}",
                    createdOrder.getId(), item.getId(), item.getHighestBidder(), item.getCurrentPrice());

        } catch (Exception e) {
            log.error("订单生成失败: auctionItemId={}, error={}", item.getId(), e.getMessage(), e);
            // 订单生成失败不应影响拍品状态更新，记录日志即可
        }
    }

    /**
     * 计算剩余时间（毫秒）
     */
    private Long calculateRemainingMs(AuctionItem item) {
        if (item.getEndTime() == null) {
            return null;
        }
        return java.time.Duration.between(LocalDateTime.now(), item.getEndTime()).toMillis();
    }

    /**
     * 计算用户在拍品中的排名
     * 排名规则：按最高出价金额排序，金额越高排名越靠前
     */
    private int calculateUserRank(Long auctionItemId, Long userId) {
        // 获取该用户在当前拍品的最高出价
        BigDecimal userMaxBid = bidRepository.getUserMaxBidAmount(auctionItemId, userId);
        if (userMaxBid == null) {
            return 9999; // 用户没有出价记录，返回一个很大的排名
        }

        // 获取所有比当前用户出价高的不同用户数量
        long higherBidCount = bidRepository.countUsersWithHigherBid(auctionItemId, userMaxBid);

        // 排名 = 高出价用户数量 + 1
        return (int) higherBidCount + 1;
    }
}
