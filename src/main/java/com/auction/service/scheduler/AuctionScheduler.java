package com.auction.service.scheduler;

import com.auction.service.consistency.DataConsistencyService;
import com.auction.service.settlement.AuctionDelayService;
import com.auction.service.settlement.AuctionSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 竞拍定时任务调度器
 * 负责处理竞拍相关的定时任务，包括：
 * 1. 竞拍结束结算
 * 2. 竞拍自动延期检查
 * 3. 竞拍自动开始
 * 4. 数据一致性检查
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionSettlementService settlementService;
    private final AuctionDelayService delayService;
    private final DataConsistencyService dataConsistencyService;

    /**
     * 竞拍结算定时任务
     * 每10秒执行一次，检查并结算已到期的竞拍
     *
     * 使用固定延迟执行，上次执行完成后等待10秒再执行下次
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void settleExpiredAuctions() {
        try {
            log.debug("开始执行竞拍结算定时任务");
            int count = settlementService.settleExpiredAuctions();
            if (count > 0) {
                log.info("竞拍结算定时任务执行完成，结算数量: {}", count);
            }
        } catch (Exception e) {
            log.error("竞拍结算定时任务执行失败", e);
        }
    }

    /**
     * 竞拍延期检查定时任务
     * 每5秒执行一次，检查是否需要自动延期
     *
     * 临近结束（最后delaySeconds秒）有出价时自动延期
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 3000)
    public void checkAuctionDelay() {
        try {
            log.debug("开始执行竞拍延期检查定时任务");
            int count = delayService.checkAndExtendAuctions();
            if (count > 0) {
                log.info("竞拍延期检查定时任务执行完成，延长数量: {}", count);
            }
        } catch (Exception e) {
            log.error("竞拍延期检查定时任务执行失败", e);
        }
    }

    /**
     * 竞拍自动开始定时任务
     * 每30秒执行一次，检查并启动到期的待开始竞拍
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void startPendingAuctions() {
        try {
            log.debug("开始执行竞拍自动开始定时任务");
            // TODO: 实现自动开始逻辑
            // settlementService.startPendingAuctions();
        } catch (Exception e) {
            log.error("竞拍自动开始定时任务执行失败", e);
        }
    }

    /**
     * 数据一致性检查定时任务
     * 每5分钟执行一次，检查并修复Redis-数据库数据不一致问题
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void checkDataConsistency() {
        try {
            log.debug("开始执行数据一致性检查定时任务");
            int fixedCount = dataConsistencyService.checkActiveAuctionsConsistency();
            if (fixedCount > 0) {
                log.info("数据一致性检查完成，修复数量: {}", fixedCount);
            }
        } catch (Exception e) {
            log.error("数据一致性检查定时任务执行失败", e);
        }
    }
}