package com.auction.service.event;

import com.auction.domain.event.AuctionStartedEvent;
import com.auction.service.delay.AuctionDelayQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 拍卖活动事件监听器
 * <p>
 * 监听拍卖活动相关的领域事件，异步处理后续业务逻辑
 * 包括：设置延时通知任务、统计记录、日志记录等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEventHandler {

    private final AuctionDelayQueueService delayQueueService;

    /**
     * 监听拍卖活动开始事件
     * <p>
     * 当活动开始时，设置"即将结束"的延时通知任务
     * 使用@TransactionalEventListener确保事务提交后再处理
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("taskExecutor")
    public void onAuctionStarted(AuctionStartedEvent event) {
        try {
            log.info("收到拍卖活动开始事件: auctionId={}, merchantId={}, endTime={}",
                    event.getAuctionId(), event.getMerchantId(), event.getEndTime());

            // 计算提前10分钟通知的时间点
            LocalDateTime notifyTime = event.getEndTime().minusMinutes(10);
            LocalDateTime now = LocalDateTime.now();

            // 只有当通知时间在未来时才设置延时任务
            if (notifyTime.isAfter(now)) {
                // 计算延时秒数
                long delaySeconds = ChronoUnit.SECONDS.between(now, notifyTime);

                // 创建即将结束的延时消息
                com.auction.domain.event.AuctionEndingSoonMessage message =
                        com.auction.domain.event.AuctionEndingSoonMessage.create(
                                event.getAuctionId(),
                                event.getAuctionTitle(),
                                event.getMerchantId(),
                                event.getEndTime(),
                                10  // 固定提前10分钟
                        );

                // 添加到延时队列
                delayQueueService.scheduleEndingNotification(message, delaySeconds);

                log.info("已设置拍卖活动即将结束延时通知: auctionId={}, 延时={}秒, 预计通知时间={}",
                        event.getAuctionId(), delaySeconds, notifyTime);
            } else {
                log.info("活动结束时间过近，无需设置即将结束通知: auctionId={}, endTime={}",
                        event.getAuctionId(), event.getEndTime());
            }

        } catch (Exception e) {
            log.error("处理拍卖活动开始事件失败: auctionId={}", event.getAuctionId(), e);
        }
    }

    /**
     * 监听其他活动事件的扩展方法
     * 可用于处理活动结束、取消等其他事件
     */
    @EventListener
    @Async("taskExecutor")
    public void onAuctionEvent(Object event) {
        // 这里可以扩展处理其他活动相关事件
        log.debug("收到拍卖活动事件: {}", event.getClass().getSimpleName());
    }
}