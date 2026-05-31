package com.auction.service.consistency;

import com.auction.domain.entity.Auction;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 数据一致性服务
 * 负责Redis缓存和数据库之间的数据一致性检查和修复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataConsistencyService {

    private final RedisService redisService;
    private final AuctionRepository auctionRepository;

    /**
     * 检查并修复指定竞拍的数据一致性
     *
     * @param auctionId 竞拍ID
     * @return 是否进行了修复
     */
    public boolean checkAndFixAuctionConsistency(Long auctionId) {
        try {
            // 获取数据库中的数据
            Auction dbAuction = auctionRepository.findById(auctionId);
            if (dbAuction == null) {
                log.warn("竞拍不存在，无法检查一致性: auctionId={}", auctionId);
                return false;
            }

            // 获取Redis中的数据
            String auctionKey = "auction:" + auctionId;
            Object redisPriceObj = redisService.get(auctionKey + ":current_price");
            Object redisBidderObj = redisService.get(auctionKey + ":highest_bidder");

            // 检查价格一致性
            boolean needFix = false;
            if (redisPriceObj != null) {
                BigDecimal redisPrice = new BigDecimal(redisPriceObj.toString());
                BigDecimal dbPrice = dbAuction.getCurrentPrice();

                if (redisPrice.compareTo(dbPrice) != 0) {
                    log.warn("发现价格不一致: auctionId={}, redisPrice={}, dbPrice={}",
                        auctionId, redisPrice, dbPrice);
                    // 以数据库为准，修复Redis
                    redisService.set(auctionKey + ":current_price", dbPrice.toString());
                    needFix = true;
                }
            } else {
                // Redis中没有数据，从数据库恢复
                redisService.set(auctionKey + ":current_price", dbAuction.getCurrentPrice().toString());
                needFix = true;
            }

            // 检查出价者一致性
            if (redisBidderObj != null && dbAuction.getHighestBidder() != null) {
                Long redisBidder = Long.parseLong(redisBidderObj.toString());
                Long dbBidder = dbAuction.getHighestBidder();

                if (!redisBidder.equals(dbBidder)) {
                    log.warn("发现出价者不一致: auctionId={}, redisBidder={}, dbBidder={}",
                        auctionId, redisBidder, dbBidder);
                    // 以数据库为准，修复Redis
                    redisService.set(auctionKey + ":highest_bidder", dbBidder.toString());
                    needFix = true;
                }
            } else if (dbAuction.getHighestBidder() != null) {
                // Redis中没有数据，从数据库恢复
                redisService.set(auctionKey + ":highest_bidder", dbAuction.getHighestBidder().toString());
                needFix = true;
            }

            if (needFix) {
                log.info("数据一致性修复完成: auctionId={}", auctionId);
            }

            return needFix;

        } catch (Exception e) {
            log.error("检查数据一致性失败: auctionId={}", auctionId, e);
            return false;
        }
    }

    /**
     * 批量检查活跃竞拍的数据一致性
     * 定时任务调用此方法
     *
     * @return 修复的数据数量
     */
    public int checkActiveAuctionsConsistency() {
        log.debug("开始批量检查活跃竞拍数据一致性");

        try {
            List<Auction> activeAuctions = auctionRepository.findActiveAuctions();
            if (activeAuctions == null || activeAuctions.isEmpty()) {
                log.debug("无活跃竞拍需要检查");
                return 0;
            }

            int fixedCount = 0;
            for (Auction auction : activeAuctions) {
                if (checkAndFixAuctionConsistency(auction.getId())) {
                    fixedCount++;
                }
            }

            if (fixedCount > 0) {
                log.info("活跃竞拍数据一致性检查完成: 总数={}, 修复={}", activeAuctions.size(), fixedCount);
            }

            return fixedCount;

        } catch (Exception e) {
            log.error("批量检查活跃竞拍数据一致性失败", e);
            return 0;
        }
    }

    /**
     * 强制刷新竞拍数据到Redis
     *
     * @param auctionId 竞拍ID
     */
    public void refreshAuctionDataToRedis(Long auctionId) {
        try {
            Auction auction = auctionRepository.findById(auctionId);
            if (auction == null) {
                log.warn("竞拍不存在，无法刷新数据: auctionId={}", auctionId);
                return;
            }

            String auctionKey = "auction:" + auctionId;

            // 刷新价格
            redisService.set(auctionKey + ":current_price", auction.getCurrentPrice().toString());

            // 刷新出价者
            if (auction.getHighestBidder() != null) {
                redisService.set(auctionKey + ":highest_bidder", auction.getHighestBidder().toString());
            }

            // 刷新结束时间
            if (auction.getEndTime() != null) {
                redisService.set(auctionKey + ":end_time", auction.getEndTime().toString());
            }

            log.info("竞拍数据刷新到Redis成功: auctionId={}", auctionId);

        } catch (Exception e) {
            log.error("刷新竞拍数据到Redis失败: auctionId={}", auctionId, e);
        }
    }

    /**
     * 清理过期的Redis数据
     *
     * @param auctionId 竞拍ID
     */
    public void clearExpiredAuctionData(Long auctionId) {
        try {
            String auctionKey = "auction:" + auctionId;
            String[] keysToDelete = {
                auctionKey + ":current_price",
                auctionKey + ":highest_bidder",
                auctionKey + ":end_time",
                auctionKey + ":last_bid_time",
                auctionKey + ":bid_count",
                auctionKey + ":leaderboard"
            };

            for (String key : keysToDelete) {
                redisService.delete(key);
            }

            log.info("清理过期竞拍数据成功: auctionId={}", auctionId);

        } catch (Exception e) {
            log.error("清理过期竞拍数据失败: auctionId={}", auctionId, e);
        }
    }
}