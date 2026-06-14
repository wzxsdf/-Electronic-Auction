package com.auction.controller;

import com.auction.domain.event.AuctionEndingSoonMessage;
import com.auction.service.delay.AuctionDelayQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 延时队列调试控制器
 * <p>
 * 仅在开发环境启用，用于验证和调试延时队列功能
 */
@Slf4j
@RestController
@RequestMapping("/debug/delay-queue")
@ConditionalOnProperty(
    name = "app.debug.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@RequiredArgsConstructor
public class DelayQueueDebugController {

    private final RedissonClient redissonClient;
    private final AuctionDelayQueueService delayQueueService;

    private static final String AUCTION_ENDING_SOON_QUEUE = "auction:ending:soon:queue";

    /**
     * 查看队列状态
     * GET /debug/delay-queue/status
     */
    @GetMapping("/status")
    public Map<String, Object> getQueueStatus() {
        try {
            RDelayedQueue<AuctionEndingSoonMessage> delayedQueue =
                    redissonClient.getDelayedQueue(
                            redissonClient.getBlockingQueue(AUCTION_ENDING_SOON_QUEUE)
                    );

            Map<String, Object> status = new HashMap<>();
            status.put("queueSize", delayedQueue.size());
            status.put("queueName", AUCTION_ENDING_SOON_QUEUE);
            status.put("timestamp", System.currentTimeMillis());

            // 获取队列中的任务列表
            List<Map<String, Object>> tasks = new ArrayList<>();
            delayedQueue.forEach(message -> {
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("auctionId", message.getAuctionId());
                taskInfo.put("auctionTitle", message.getAuctionTitle());
                taskInfo.put("merchantId", message.getMerchantId());
                taskInfo.put("endTime", message.getEndTime().toString());
                taskInfo.put("remainingMinutes", message.getRemainingMinutes());
                taskInfo.put("createdTime", message.getCreatedTime().toString());
                taskInfo.put("scheduledTime", message.getScheduledTime().toString());
                tasks.add(taskInfo);
            });

            status.put("tasks", tasks);
            status.put("status", "success");

            return status;

        } catch (Exception e) {
            log.error("获取队列状态失败", e);
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * 添加测试任务
     * POST /debug/delay-queue/test-task
     */
    @PostMapping("/test-task")
    public Map<String, Object> addTestTask(
            @RequestParam(defaultValue = "999") Long auctionId,
            @RequestParam(defaultValue = "10") int delaySeconds,
            @RequestParam(defaultValue = "调试测试活动") String title
    ) {
        try {
            AuctionEndingSoonMessage message = AuctionEndingSoonMessage.create(
                    auctionId,
                    title,
                    8888L,
                    LocalDateTime.now().plusSeconds(delaySeconds + 600),
                    10
            );

            delayQueueService.scheduleEndingNotification(message, delaySeconds);

            return Map.of(
                    "status", "success",
                    "auctionId", auctionId,
                    "delaySeconds", delaySeconds,
                    "scheduledTime", message.getScheduledTime().toString(),
                    "message", "测试任务已添加，将在" + delaySeconds + "秒后执行"
            );

        } catch (Exception e) {
            log.error("添加测试任务失败", e);
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * 批量添加测试任务
     * POST /debug/delay-queue/batch-tasks
     */
    @PostMapping("/batch-tasks")
    public Map<String, Object> addBatchTasks(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "10") int delaySeconds
    ) {
        try {
            List<Map<String, Object>> results = new ArrayList<>();

            for (int i = 1; i <= count; i++) {
                Long auctionId = (long) (10000 + i);
                String title = "批量测试" + i;

                try {
                    AuctionEndingSoonMessage message = AuctionEndingSoonMessage.create(
                            auctionId,
                            title,
                            8888L,
                            LocalDateTime.now().plusSeconds(delaySeconds + 600),
                            10
                    );

                    delayQueueService.scheduleEndingNotification(message, delaySeconds);

                    results.add(Map.of(
                            "auctionId", auctionId,
                            "title", title,
                            "status", "success"
                    ));

                } catch (Exception e) {
                    results.add(Map.of(
                            "auctionId", auctionId,
                            "title", title,
                            "status", "error",
                            "message", e.getMessage()
                            )
                    );
                }
            }

            return Map.of(
                    "status", "success",
                    "totalTasks", count,
                    "delaySeconds", delaySeconds,
                    "results", results
            );

        } catch (Exception e) {
            log.error("批量添加测试任务失败", e);
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * 清空队列
     * DELETE /debug/delay-queue/clear
     */
    @DeleteMapping("/clear")
    public Map<String, Object> clearQueue() {
        try {
            RDelayedQueue<AuctionEndingSoonMessage> delayedQueue =
                    redissonClient.getDelayedQueue(
                            redissonClient.getBlockingQueue(AUCTION_ENDING_SOON_QUEUE)
                    );

            int sizeBefore = delayedQueue.size();
            delayedQueue.clear();

            log.info("已清空延时队列: 清理任务数={}", sizeBefore);

            return Map.of(
                    "status", "success",
                    "clearedCount", sizeBefore,
                    "message", "队列已清空"
            );

        } catch (Exception e) {
            log.error("清空队列失败", e);
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * 模拟完整的拍卖流程
     * POST /debug/delay-queue/simulate-auction
     */
    @PostMapping("/simulate-auction")
    public Map<String, Object> simulateAuction(
            @RequestParam(defaultValue = "777") Long auctionId,
            @RequestParam(defaultValue = "模拟拍卖测试") String title,
            @RequestParam(defaultValue = "30") int durationSeconds
    ) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = now.plusSeconds(durationSeconds);

            // 计算提前10分钟通知（如果活动时长超过10分钟）
            int notificationMinutes = Math.max(1, durationSeconds / 60 - 10);
            if (durationSeconds < 60) {
                notificationMinutes = durationSeconds / 3; // 如果活动少于1分钟，提前1/3时间通知
            }

            LocalDateTime notifyTime = endTime.minusMinutes(notificationMinutes);
            long delaySeconds = java.time.Duration.between(now, notifyTime).getSeconds();

            if (delaySeconds <= 0) {
                return Map.of(
                        "status", "error",
                        "message", "活动时间过短，无法设置提前通知"
                );
            }

            // 创建测试消息
            AuctionEndingSoonMessage message = AuctionEndingSoonMessage.create(
                    auctionId,
                    title,
                    6666L,
                    endTime,
                    notificationMinutes
            );

            // 添加到延时队列
            delayQueueService.scheduleEndingNotification(message, delaySeconds);

            return Map.of(
                    "status", "success",
                    "auctionId", auctionId,
                    "title", title,
                    "startTime", now.toString(),
                    "endTime", endTime.toString(),
                    "notifyTime", notifyTime.toString(),
                    "delaySeconds", delaySeconds,
                    "notificationMinutes", notificationMinutes,
                    "message", "模拟拍卖已创建，将在" + delaySeconds + "秒后发送即将结束通知"
            );

        } catch (Exception e) {
            log.error("模拟拍卖失败", e);
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * 获取队列统计信息
     * GET /debug/delay-queue/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getQueueStats() {
        try {
            RDelayedQueue<AuctionEndingSoonMessage> delayedQueue =
                    redissonClient.getDelayedQueue(
                            redissonClient.getBlockingQueue(AUCTION_ENDING_SOON_QUEUE)
                    );

            Map<String, Object> stats = new HashMap<>();
            stats.put("queueSize", delayedQueue.size());
            stats.put("timestamp", System.currentTimeMillis());

            // 统计任务分布
            Map<String, Integer> distribution = new HashMap<>();
            delayedQueue.forEach(message -> {
                String key = message.getAuctionTitle().substring(0, Math.min(10, message.getAuctionTitle().length()));
                distribution.put(key, distribution.getOrDefault(key, 0) + 1);
            });

            stats.put("distribution", distribution);
            stats.put("status", "success");

            return stats;

        } catch (Exception e) {
            log.error("获取队列统计失败", e);
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * 健康检查
     * GET /debug/delay-queue/health
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        try {
            // 检查Redisson连接
            redissonClient.getKeys().count();

            // 检查队列访问
            RDelayedQueue<AuctionEndingSoonMessage> delayedQueue =
                    redissonClient.getDelayedQueue(
                            redissonClient.getBlockingQueue(AUCTION_ENDING_SOON_QUEUE)
                    );

            return Map.of(
                    "status", "healthy",
                    "timestamp", System.currentTimeMillis(),
                    "queueAccessible", true,
                    "currentQueueSize", delayedQueue.size()
            );

        } catch (Exception e) {
            return Map.of(
                    "status", "unhealthy",
                    "timestamp", System.currentTimeMillis(),
                    "error", e.getMessage()
            );
        }
    }
}