package com.auction.service.delay;

import com.auction.domain.event.AuctionEndingSoonMessage;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.AuctionRepository;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 拍卖活动延时队列服务
 * <p>
 * 基于Redisson实现延时队列，用于处理拍卖活动即将结束等延时任务
 * 支持高并发、分布式部署
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionDelayQueueService {

    private final RedissonClient redissonClient;
    private final WsMessageService wsMessageService;
    private final AuctionRepository auctionRepository;
    private final RedisService redisService;

    private static final String AUCTION_ENDING_SOON_QUEUE = "auction:ending:soon:queue";

    private final AtomicBoolean running = new AtomicBoolean(true);
    private CompletableFuture<?> consumerFuture;

    /**
     * 启动延时队列消费者
     */
    @PostConstruct
    public void startConsumer() {
        consumerFuture = CompletableFuture.runAsync(() -> {
            // 获取阻塞队列
            RBlockingQueue<AuctionEndingSoonMessage> blockingQueue =
                    redissonClient.getBlockingQueue(AUCTION_ENDING_SOON_QUEUE);

            // 获取延时队列
            RDelayedQueue<AuctionEndingSoonMessage> delayedQueue =
                    redissonClient.getDelayedQueue(blockingQueue);

            log.info("拍卖活动延时队列消费者已启动");

            // 持续监听队列
            while (running.get()) {
                try {
                    // 阻塞获取到期的消息（最多等待1秒）
                    AuctionEndingSoonMessage message = blockingQueue.poll(1, TimeUnit.SECONDS);

                    if (message != null) {
                        handleMessage(message);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("延时队列消费者被中断");
                    break;
                } catch (Exception e) {
                    log.error("处理延时消息异常", e);
                    // 避免异常导致循环退出
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            log.info("拍卖活动延时队列消费者已停止");
        });

        log.info("拍卖活动延时队列服务启动完成");
    }

    /**
     * 添加拍卖活动即将结束的延时任务
     *
     * @param message        延时消息
     * @param delaySeconds   延时秒数
     */
    public void scheduleEndingNotification(AuctionEndingSoonMessage message, long delaySeconds) {
        try {
            // 获取延时队列
            RBlockingQueue<AuctionEndingSoonMessage> blockingQueue =
                    redissonClient.getBlockingQueue(AUCTION_ENDING_SOON_QUEUE);

            RDelayedQueue<AuctionEndingSoonMessage> delayedQueue =
                    redissonClient.getDelayedQueue(blockingQueue);

            // 添加到延时队列
            delayedQueue.offer(message, delaySeconds, TimeUnit.SECONDS);

            log.info("已添加拍卖活动即将结束延时任务: auctionId={}, merchantId={}, 延时={}秒, 预计执行时间={}",
                    message.getAuctionId(), message.getMerchantId(), delaySeconds, message.getScheduledTime());

        } catch (Exception e) {
            log.error("添加延时任务失败: auctionId={}", message.getAuctionId(), e);
        }
    }

    /**
     * 处理到期的延时消息
     *
     * @param message 延时消息
     */
    private void handleMessage(AuctionEndingSoonMessage message) {
        try {
            log.info("开始处理拍卖活动即将结束通知: auctionId={}, merchantId={}",
                    message.getAuctionId(), message.getMerchantId());

            // 1. 检查活动是否仍然有效（防止活动被取消后仍发送通知）
            String statusKey = "auction:status:" + message.getAuctionId();
            Object cachedStatusObj = redisService.get(statusKey);
            String cachedStatus = cachedStatusObj != null ? String.valueOf(cachedStatusObj) : null;

            if ("CANCELLED".equals(cachedStatus)) {
                log.info("活动已取消，跳过通知: auctionId={}", message.getAuctionId());
                return;
            }

            // 2. 双重检查：从数据库确认活动状态
            com.auction.domain.entity.Auction auction = auctionRepository.findById(message.getAuctionId());
            if (auction == null) {
                log.warn("活动不存在，跳过通知: auctionId={}", message.getAuctionId());
                return;
            }

            // 3. 检查活动是否已结束或取消
            if (!"ACTIVE".equals(auction.getStatus())) {
                log.info("活动状态已变更，跳过通知: auctionId={}, status={}",
                        message.getAuctionId(), auction.getStatus());
                return;
            }

            // 4. 检查是否已发送过类似通知（防止重复）
            String notificationKey = "auction:ending:soon:notification:" + message.getAuctionId();
            boolean alreadyNotified = redisService.hasKey(notificationKey);

            if (alreadyNotified) {
                log.debug("拍卖活动已发送过即将结束通知，跳过: auctionId={}", message.getAuctionId());
                return;
            }

            // 5. 重新计算剩余时间（确保通知的准确性）
            LocalDateTime now = LocalDateTime.now();
            int remainingMinutes = (int) java.time.Duration.between(now, message.getEndTime()).toMinutes();

            if (remainingMinutes <= 0) {
                log.info("活动已到期，跳过通知: auctionId={}", message.getAuctionId());
                return;
            }

            // 6. 发送WebSocket通知给商家
            wsMessageService.sendAuctionEndingSoonToMerchant(
                    message.getMerchantId(),
                    message.getAuctionId(),
                    message.getAuctionTitle(),
                    message.getEndTime(),
                    remainingMinutes
            );

            // 7. 标记已发送通知，防止重复通知（24小时过期）
            redisService.set(notificationKey, "1", 24 * 60 * 60L, TimeUnit.SECONDS);

            log.info("拍卖活动即将结束通知已发送: auctionId={}, merchantId={}, 剩余时间={}分钟",
                    message.getAuctionId(), message.getMerchantId(), remainingMinutes);

        } catch (Exception e) {
            log.error("处理拍卖活动即将结束通知失败: auctionId={}", message.getAuctionId(), e);
        }
    }

    /**
     * 停止延时队列消费者
     */
    @PreDestroy
    public void stopConsumer() {
        running.set(false);

        if (consumerFuture != null) {
            consumerFuture.cancel(true);
        }

        log.info("拍卖活动延时队列消费者已停止");
    }

    /**
     * 清理指定活动的所有延时任务
     * 用于活动取消或结束时清理未执行的通知
     *
     * @param auctionId 活动ID
     */
    public void clearAuctionTasks(Long auctionId) {
        try {
            RBlockingQueue<AuctionEndingSoonMessage> blockingQueue =
                    redissonClient.getBlockingQueue(AUCTION_ENDING_SOON_QUEUE);

            RDelayedQueue<AuctionEndingSoonMessage> delayedQueue =
                    redissonClient.getDelayedQueue(blockingQueue);

            // 移除队列中该活动的所有消息
            delayedQueue.forEach(message -> {
                if (message.getAuctionId().equals(auctionId)) {
                    delayedQueue.remove(message);
                    log.info("已清理活动的延时任务: auctionId={}", auctionId);
                }
            });

        } catch (Exception e) {
            log.error("清理活动延时任务失败: auctionId={}", auctionId, e);
        }
    }
}