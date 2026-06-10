package com.auction.service.scheduler;

import com.auction.service.auction.AuctionItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 拍卖定时任务调度器（重构后）
 * <p>
 * 负责处理拍品相关的定时任务，包括：
 * 1. 拍品自动结算 - 结束到期的活跃拍品
 * 2. 拍品自动开始 - 启动待开始的拍品
 * <p>
 * 重构说明：
 * - 删除了Auction级别的延时检查（延时机制已在出价时触发）
 * - 删除了Auction级别的数据一致性检查（新架构无Auction级缓存）
 * - 所有操作改为AuctionItem级别
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionItemService auctionItemService;

    /**
     * 拍品自动结算定时任务
     * 每10秒执行一次，检查并结算已到期的拍品
     * <p>
     * 查询结束时间已过但状态仍为ACTIVE的拍品，自动结束并生成订单
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void settleExpiredItems() {
        try {
            int count = auctionItemService.settleExpiredItems();
            if (count > 0) {
                log.info("拍品自动结算定时任务执行完成，结算数量: {}", count);
            }
        } catch (Exception e) {
            log.error("拍品自动结算定时任务执行失败", e);
        }
    }

//    /**
//     * 拍品自动开始定时任务
//     * 每30秒执行一次，检查并启动到期的待开始拍品
//     * <p>
//     * 查询开始时间已到但状态仍为PENDING的拍品，自动启动
//     * 注意：只有所属活动状态为ACTIVE时，拍品才能自动开始
//     */
//    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
//    public void startPendingItems() {
//        try {
//            int count = auctionItemService.startPendingItems();
//            if (count > 0) {
//                log.info("拍品自动开始定时任务执行完成，启动数量: {}", count);
//            }
//        } catch (Exception e) {
//            log.error("拍品自动开始定时任务执行失败", e);
//        }
//    }

    /**
     * ==================== 已废弃的定时任务 ====================
     * 以下定时任务在重构后已不再需要，已删除：
     * - checkAuctionDelay(): 延时机制已在AuctionItemService出价时触发
     * - checkDataConsistency(): 新架构无Auction级别缓存，无需检查
     */
}
