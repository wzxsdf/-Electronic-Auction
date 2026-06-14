package com.auction.service.event;

import com.auction.domain.event.BidEvent;
import com.auction.service.ranking.BidRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

/**
 * 出价事件监听器
 * <p>
 * 监听出价事件并触发相关功能：
 * 1. 更新排行榜（Redis缓存 + WebSocket推送）
 * 2. 记录排行榜历史快照
 * 3. 发送实时通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidEventListener {

    private final BidRankingService bidRankingService;

    /**
     * 处理出价事件（事务提交后执行）
     * <p>
     * 当出价成功后，自动更新排行榜并推送变化
     */
    @Async("websocketTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBidEvent(BidEvent event) {
        try {
            log.info("处理出价事件: auctionItemId={}, userId={}, amount={}",
                event.getAuctionItemId(), event.getUserId(), event.getAmount());

            // 1. 更新排行榜（Redis缓存 + WebSocket推送）
            bidRankingService.updateRanking(
                event.getAuctionItemId(),
                event.getUserId(),
                event.getAmount()
            );

            // 2. 如果是关键事件，保存排行榜快照
            if (isKeyRankingEvent(event)) {
                bidRankingService.saveRankingSnapshot(
                    event.getAuctionItemId(),
                    "EVENT"
                );
                log.info("关键事件快照已保存: auctionItemId={}", event.getAuctionItemId());
            }

        } catch (Exception e) {
            log.error("处理出价事件失败: auctionItemId={}, userId={}, error={}",
                event.getAuctionItemId(), event.getUserId(), e.getMessage(), e);
        }
    }

    /**
     * 判断是否为关键排行榜事件
     * <p>
     * 关键事件包括：
     * 1. 价格突破整数关口
     * 2. 领先者变更
     * 3. 参与人数显著增加
     */
    private boolean isKeyRankingEvent(BidEvent event) {
        // 1. 价格突破整数关口
        BigDecimal amount = event.getAmount();
        if (amount != null && amount.longValue() % 1000 == 0) {
            return true; // 价格每达到1000的倍数
        }

        // 2. 参与人数达到里程碑
        Integer bidCount = event.getBidCount();
        if (bidCount != null && bidCount % 10 == 0) {
            return true; // 每10次出价
        }

        // 3. 判断是否为新领先者（需要比较历史最高出价者）
        // 这里简化处理，实际应该查询历史数据比较

        return false;
    }

    /**
     * 拍品结束时保存最终排行榜结果
     * <p>
     * 这个方法可以由AuctionScheduler或其他调度器调用
     */
    @Async("websocketTaskExecutor")
    public void handleAuctionItemEnd(Long auctionItemId) {
        try {
            log.info("拍品结束，保存最终排行榜: auctionItemId={}", auctionItemId);

            // 保存最终排行榜结果
            bidRankingService.saveFinalRanking(auctionItemId);

        } catch (Exception e) {
            log.error("保存最终排行榜失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
        }
    }
}
