package com.auction.service.bid;

import com.auction.annotation.RateLimit;
import com.auction.api.dto.request.PlaceBidRequest;
import com.auction.api.dto.response.BidResultResponse;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.Bid;
import com.auction.domain.entity.User;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.lock.DistributedLockService;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.AuctionRepository;
import com.auction.repository.BidRepository;
import com.auction.repository.UserRepository;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final WsMessageService wsMessageService;
    private final RedisService redisService;
    private final DistributedLockService distributedLockService;

    /**
     * 出价（使用分布式锁保证并发安全）
     *
     * @param request 出价请求
     * @return 出价结果
     */
    public BidResultResponse placeBid(PlaceBidRequest request) {
        // 使用分布式锁保护整个出价过程，防止并发问题
        String lockKey = DistributedLockService.bidLockKey(request.getAuctionId());

        return distributedLockService.executeWithLock(lockKey, () -> {
            // 在事务内执行出价逻辑
            return placeBidInternal(request);
        });
    }

    /**
     * 内部出价逻辑（在分布式锁保护下执行）
     */
    @Transactional(rollbackFor = Exception.class)
    public BidResultResponse placeBidInternal(PlaceBidRequest request) {
        // 1. 获取竞拍信息
        Auction auction = auctionRepository.findById(request.getAuctionId());
        if (auction == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        // 2. 获取用户信息
        User user = userRepository.findById(request.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. 校验出价
        validateBid(auction, request.getUserId(), request.getAmount());

        // 4. 获取之前的价格
        BigDecimal previousPrice = auction.getCurrentPrice();
        Long previousHighestBidder = auction.getHighestBidder();

        // 5. 更新 Redis
        updateAuctionState(auction, request.getAmount(), request.getUserId());

        // 6. 更新排行榜
        updateLeaderboard(auction.getId(), request.getUserId(), request.getAmount());

        // 7. 保存出价记录
        Bid bid = new Bid();
        bid.setAuctionId(request.getAuctionId());
        bid.setUserId(request.getUserId());
        bid.setAmount(request.getAmount());
        bid.setStatus("ACTIVE");
        bid.setIsAutoBid(request.getIsAutoBid() != null && request.getIsAutoBid());
        bid = bidRepository.save(bid);

        // 8. 更新竞拍当前价格
        auction.setCurrentPrice(request.getAmount());
        auction.setHighestBidder(request.getUserId());
        auctionRepository.updateById(auction);

        // 9. 计算排名
        Integer rank = calculateRank(auction.getId(), request.getUserId());

        // 10. WebSocket 推送
        wsMessageService.broadcastNewBid(auction, bid, user.getNickname() != null ? user.getNickname() : "用户***", rank);
        wsMessageService.broadcastPriceUpdate(request.getAuctionId());

        // 11. 通知被超越者
        if (previousHighestBidder != null && !previousHighestBidder.equals(request.getUserId())) {
            wsMessageService.sendYouWereOvertaken(previousHighestBidder, auction.getId(), request.getAmount());
        }

        // 12. 通知当前领先者
        wsMessageService.sendYouAreLeading(request.getUserId(), auction.getId(), request.getAmount());

        // 13. 更新用户统计
        user.setTotalBids(user.getTotalBids() + 1);
        userRepository.updateById(user);

        // 14. 计算剩余时间
        Long remainingMs = calculateRemainingMs(auction);

        log.info("出价成功: auctionId={}, userId={}, amount={}, rank={}",
            auction.getId(), request.getUserId(), request.getAmount(), rank);

        return BidResultResponse.builder()
            .bidId(bid.getId())
            .currentPrice(request.getAmount())
            .yourRank(rank)
            .isLeading(true)
            .remainingMs(remainingMs)
            .message("出价成功")
            .build();
    }

    private void validateBid(Auction auction, Long userId, BigDecimal amount) {
        AuctionStatus status = auction.getStatusEnum();
        if (status != AuctionStatus.ACTIVE) {
            if (status == AuctionStatus.PENDING) {
                throw new BizException(ErrorCode.AUCTION_NOT_STARTED);
            } else if (status == AuctionStatus.COMPLETED) {
                throw new BizException(ErrorCode.AUCTION_ALREADY_ENDED);
            } else if (status == AuctionStatus.CANCELLED) {
                throw new BizException(ErrorCode.AUCTION_CANCELLED);
            }
        }

        BigDecimal currentPrice = getCurrentPrice(auction);
        if (amount.compareTo(currentPrice) <= 0) {
            throw new BizException(ErrorCode.BID_AMOUNT_TOO_LOW,
                String.format("出价必须高于当前价格 %s", currentPrice));
        }

        BigDecimal minIncrement = auction.getBidIncrement();
        BigDecimal minValidPrice = currentPrice.add(minIncrement);
        if (amount.compareTo(minValidPrice) < 0) {
            throw new BizException(ErrorCode.BID_AMOUNT_INVALID,
                String.format("出价必须按 %s 的幅度递增，最低有效出价为 %s", minIncrement, minValidPrice));
        }

        if (auction.getMaxPrice() != null && amount.compareTo(auction.getMaxPrice()) > 0) {
            throw new BizException(ErrorCode.BID_EXCEED_MAX_PRICE,
                String.format("出价不能超过封顶价 %s", auction.getMaxPrice()));
        }

        Long highestBidder = getHighestBidder(auction);
        if (userId.equals(highestBidder)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "您当前是最高出价者，无需再次出价");
        }

        checkBidFrequency(userId);
    }

    private BigDecimal getCurrentPrice(Auction auction) {
        String key = "auction:" + auction.getId() + ":current_price";
        Object priceObj = redisService.get(key);
        return priceObj != null ? new BigDecimal(priceObj.toString()) : auction.getCurrentPrice();
    }

    private Long getHighestBidder(Auction auction) {
        String key = "auction:" + auction.getId() + ":highest_bidder";
        Object bidderObj = redisService.get(key);
        return bidderObj != null ? Long.parseLong(bidderObj.toString()) : auction.getHighestBidder();
    }

    private void checkBidFrequency(Long userId) {
        String key = "bid:count:" + userId + ":1min";
        Object countObj = redisService.get(key);
        int count = countObj != null ? Integer.parseInt(countObj.toString()) : 0;

        if (count >= 30) {
            throw new BizException(ErrorCode.BID_FREQUENCY_HIGH);
        }

        redisService.increment(key);
        redisService.expire(key, 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void updateAuctionState(Auction auction, BigDecimal newPrice, Long userId) {
        String auctionKey = "auction:" + auction.getId();
        redisService.set(auctionKey + ":current_price", newPrice.toString());
        redisService.set(auctionKey + ":highest_bidder", userId.toString());
        redisService.increment(auctionKey + ":bid_count");
    }

    private void updateLeaderboard(Long auctionId, Long userId, BigDecimal amount) {
        String leaderboardKey = "auction:" + auctionId + ":leaderboard";
        redisService.zAdd(leaderboardKey, userId.toString(), amount.doubleValue());
    }

    private Integer calculateRank(Long auctionId, Long userId) {
        String leaderboardKey = "auction:" + auctionId + ":leaderboard";
        Long rank = redisService.zReverseRank(leaderboardKey, userId.toString());
        return rank != null ? rank.intValue() + 1 : null;
    }

    private Long calculateRemainingMs(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return 0L;
        }
        return Duration.between(now, auction.getEndTime()).toMillis();
    }
}
