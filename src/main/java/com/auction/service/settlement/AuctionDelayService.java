package com.auction.service.settlement;

import com.auction.domain.entity.Auction;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.AuctionRepository;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞拍自动延时服务
 * 负责处理竞拍临近结束时的自动延时逻辑
 * 当竞拍即将结束（最后delaySeconds秒）且有出价时，自动延长竞拍时间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionDelayService {

    private final AuctionRepository auctionRepository;
    private final RedisService redisService;
    private final WsMessageService wsMessageService;

    /**
     * 最大延时次数限制
     * 防止竞拍无限延长
     */
    private static final int MAX_DELAY_COUNT = 10;

    /**
     * 检查并自动延展竞拍时间
     * 定时任务调用此方法，检查是否有需要延长的竞拍
     *
     * @return 延长的竞拍数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int checkAndExtendAuctions() {
        log.debug("开始检查竞拍自动延期");

        LocalDateTime now = LocalDateTime.now();
        List<Auction> auctionsNeedDelay = auctionRepository.findAuctionsNeedDelay(now, 15);

        if (auctionsNeedDelay == null || auctionsNeedDelay.isEmpty()) {
            log.debug("无需要延期的竞拍");
            return 0;
        }

        int extendedCount = 0;
        for (Auction auction : auctionsNeedDelay) {
            try {
                if (shouldExtendAuction(auction)) {
                    extendAuctionTime(auction);
                    extendedCount++;
                }
            } catch (Exception e) {
                log.error("延期竞拍失败: auctionId={}", auction.getId(), e);
            }
        }

        if (extendedCount > 0) {
            log.info("竞拍延期完成: 检查数量={}, 延长数量={}", auctionsNeedDelay.size(), extendedCount);
        }

        return extendedCount;
    }

    /**
     * 判断竞拍是否需要延期
     * 条件：竞拍即将结束（结束时间在未来15秒内）且在最近5秒内有出价
     */
    private boolean shouldExtendAuction(Auction auction) {
        // 检查延时次数限制
        if (auction.getDelayCount() != null && auction.getDelayCount() >= MAX_DELAY_COUNT) {
            log.info("竞拍已达最大延时次数，不再延期: auctionId={}, delayCount={}",
                auction.getId(), auction.getDelayCount());
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();

        // 检查竞拍是否即将结束（剩余时间<=delaySeconds）
        if (endTime.isBefore(now) || endTime.isAfter(now.plusSeconds(auction.getDelaySeconds()))) {
            return false;
        }

        // 检查最近是否有出价（使用Redis记录最后出价时间）
        String lastBidTimeKey = "auction:" + auction.getId() + ":last_bid_time";
        Object lastBidTimeObj = redisService.get(lastBidTimeKey);

        if (lastBidTimeObj == null) {
            return false;
        }

        try {
            LocalDateTime lastBidTime = LocalDateTime.parse(lastBidTimeObj.toString());
            // 如果最后出价时间在最近5秒内，则需要延期
            return lastBidTime.isAfter(now.minusSeconds(5));
        } catch (Exception e) {
            log.warn("解析最后出价时间失败: auctionId={}, value={}", auction.getId(), lastBidTimeObj);
            return false;
        }
    }

    /**
     * 延长竞拍时间
     */
    private void extendAuctionTime(Auction auction) {
        LocalDateTime oldEndTime = auction.getEndTime();
        LocalDateTime newEndTime = oldEndTime.plusSeconds(auction.getDelaySeconds());

        // 更新延时次数计数
        if (auction.getDelayCount() == null) {
            auction.setDelayCount(1);
        } else {
            auction.setDelayCount(auction.getDelayCount() + 1);
        }

        // 更新数据库
        auction.setEndTime(newEndTime);
        auctionRepository.updateById(auction);

        // 更新Redis缓存
        String endTimeKey = "auction:" + auction.getId() + ":end_time";
        redisService.set(endTimeKey, newEndTime.toString());

        // 发送WebSocket通知
        wsMessageService.sendAuctionDelayed(auction.getId(), newEndTime);

        log.info("竞拍时间延长: auctionId={}, oldEndTime={}, newEndTime={}, delayCount={}",
            auction.getId(), oldEndTime, newEndTime, auction.getDelayCount());
    }

    /**
     * 记录出价时间
     * 在出价时调用此方法，记录最后出价时间用于延时判断
     */
    public void recordBidTime(Long auctionId) {
        String lastBidTimeKey = "auction:" + auctionId + ":last_bid_time";
        redisService.set(lastBidTimeKey, LocalDateTime.now().toString());

        // 设置过期时间（30分钟）
        redisService.expire(lastBidTimeKey, 1800, java.util.concurrent.TimeUnit.SECONDS);

        log.debug("记录出价时间: auctionId={}", auctionId);
    }

    /**
     * 检查指定竞拍是否需要延期
     * 供出价服务调用，在出价后立即检查
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkDelayForAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null || auction.getStatusEnum() != AuctionStatus.ACTIVE) {
            return;
        }

        if (shouldExtendAuction(auction)) {
            extendAuctionTime(auction);
        }
    }
}