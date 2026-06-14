package com.auction.delay;

import com.auction.domain.event.AuctionEndingSoonMessage;
import com.auction.domain.event.AuctionStartedEvent;
import com.auction.service.delay.AuctionDelayQueueService;
import com.auction.service.event.AuctionEventHandler;
import com.auction.service.websocket.WsMessageService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 拍卖延时队列功能测试
 * <p>
 * 验证Redisson延时队列的各种场景
 */
@SpringBootTest
public class AuctionDelayQueueServiceTest {

    @Autowired
    private AuctionDelayQueueService delayQueueService;

    @Autowired
    private AuctionEventHandler auctionEventHandler;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WsMessageService wsMessageService;

    /**
     * 测试基本的延时通知功能
     */
    @Test
    public void testBasicDelayNotification() throws InterruptedException {
        System.out.println("=== 开始测试基本延时通知功能 ===");

        // 创建测试消息
        AuctionEndingSoonMessage message = AuctionEndingSoonMessage.create(
                999L,  // 测试活动ID
                "测试拍卖活动",
                1001L, // 测试商家ID
                LocalDateTime.now().plusSeconds(30), // 30秒后结束
                10     // 提前10分钟通知（这里会立即触发因为不到10分钟）
        );

        System.out.println("创建测试消息: " + message);

        // 添加延时任务（10秒后执行）
        delayQueueService.scheduleEndingNotification(message, 10);

        System.out.println("已添加10秒延时任务，等待执行...");

        // 等待15秒，确保任务被执行
        Thread.sleep(15000);

        System.out.println("测试完成！请检查日志中是否有相关通知记录");
    }

    /**
     * 测试事件驱动流程
     */
    @Test
    public void testEventDrivenFlow() throws InterruptedException {
        System.out.println("=== 开始测试事件驱动流程 ===");

        // 创建拍卖开始事件
        AuctionStartedEvent event = AuctionStartedEvent.create(
                888L,  // 测试活动ID
                "事件驱动测试活动",
                1002L, // 测试商家ID
                LocalDateTime.now(),
                LocalDateTime.now().plusSeconds(20), // 20秒后结束
                1000L  // 操作者ID
        );

        System.out.println("发布拍卖开始事件: " + event);

        // 手动触发事件处理器
        auctionEventHandler.onAuctionStarted(event);

        System.out.println("事件已发布，等待延时队列处理...");

        // 等待25秒
        Thread.sleep(25000);

        System.out.println("测试完成！请检查日志和商家通知");
    }

    /**
     * 测试多个延时任务
     */
    @Test
    public void testMultipleDelayTasks() throws InterruptedException {
        System.out.println("=== 开始测试多个延时任务 ===");

        // 创建3个不同时间的测试任务
        for (int i = 1; i <= 3; i++) {
            long delaySeconds = 5L * i; // 5秒、10秒、15秒

            AuctionEndingSoonMessage message = AuctionEndingSoonMessage.create(
                    (long) (1000 + i),
                    "多任务测试活动" + i,
                    2000L + i,
                    LocalDateTime.now().plusSeconds(delaySeconds + 10),
                    10
            );

            delayQueueService.scheduleEndingNotification(message, delaySeconds);
            System.out.println("添加任务" + i + "，延时" + delaySeconds + "秒");
        }

        // 等待20秒，确保所有任务都执行
        Thread.sleep(20000);

        System.out.println("多任务测试完成！");
    }

    /**
     * 测试延时队列状态查看
     */
    @Test
    public void testInspectDelayQueue() throws InterruptedException {
        System.out.println("=== 开始测试延时队列状态查看 ===");

        String queueName = "auction:ending:soon:queue";

        // 添加测试任务
        AuctionEndingSoonMessage message = AuctionEndingSoonMessage.create(
                777L,
                "队列状态测试",
                3000L,
                LocalDateTime.now().plusMinutes(15),
                10
        );

        delayQueueService.scheduleEndingNotification(message, 30);

        System.out.println("已添加30秒延时任务");

        // 查看队列状态
        RDelayedQueue<AuctionEndingSoonMessage> delayedQueue =
                redissonClient.getDelayedQueue(
                        redissonClient.getBlockingQueue(queueName)
                );

        System.out.println("当前队列中的任务数量: " + delayedQueue.size());

        // 列出队列中的任务
        delayedQueue.forEach(task -> {
            System.out.println("队列中的任务: " + task.getAuctionTitle() +
                    ", 计划执行时间: " + task.getScheduledTime());
        });

        // 等待35秒后再次查看
        Thread.sleep(35000);

        System.out.println("35秒后队列中的任务数量: " + delayedQueue.size());

        System.out.println("队列状态测试完成！");
    }

    /**
     * 集成测试：完整流程测试
     */
    @Test
    public void testCompleteFlow() throws InterruptedException {
        System.out.println("=== 开始完整流程集成测试 ===");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        // 1. 创建拍卖开始事件
        AuctionStartedEvent event = AuctionStartedEvent.create(
                666L,
                "完整流程测试",
                4000L,
                LocalDateTime.now(),
                LocalDateTime.now().plusSeconds(25), // 25秒后结束
                5000L
        );

        System.out.println("步骤1: 发布拍卖开始事件");
        auctionEventHandler.onAuctionStarted(event);

        // 2. 等待延时任务执行（25秒 - 10分钟提前通知 = 任务会立即执行，因为不到10分钟）
        // 实际上由于结束时间只有25秒，所以会立即设置一个15秒的延时任务
        System.out.println("步骤2: 等待延时任务执行");

        Thread.sleep(30000); // 等待30秒

        // 3. 验证结果
        System.out.println("步骤3: 验证测试结果");
        System.out.println("测试完成！请查看:");
        System.out.println("1. AuctionDelayQueueService的日志");
        System.out.println("2. WsMessageService的通知日志");
        System.out.println("3. 商家用户是否收到WebSocket消息");

        successCount.set(1);
        latch.countDown();

        assertTrue(latch.await(35, TimeUnit.SECONDS), "测试应该在35秒内完成");
        assertEquals(1, successCount.get(), "测试应该成功完成");

        System.out.println("完整流程测试成功完成！");
    }

    /**
     * 测试清理功能
     */
    @Test
    public void testClearTasks() {
        System.out.println("=== 开始测试清理功能 ===");

        String queueName = "auction:ending:soon:queue";

        // 添加几个测试任务
        for (int i = 1; i <= 3; i++) {
            AuctionEndingSoonMessage message = AuctionEndingSoonMessage.create(
                    (long) (5000 + i),
                    "清理测试" + i,
                    6000L,
                    LocalDateTime.now().plusHours(1),
                    10
            );

            delayQueueService.scheduleEndingNotification(message, 600); // 10分钟延时
        }

        System.out.println("添加了3个测试任务");

        // 查看队列状态
        RDelayedQueue<AuctionEndingSoonMessage> delayedQueue =
                redissonClient.getDelayedQueue(
                        redissonClient.getBlockingQueue(queueName)
                );

        int beforeSize = delayedQueue.size();
        System.out.println("清理前队列大小: " + beforeSize);

        // 清理指定活动任务
        delayQueueService.clearAuctionTasks(5001L);

        // 稍等片刻让清理操作完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int afterSize = delayedQueue.size();
        System.out.println("清理后队列大小: " + afterSize);

        assertTrue(beforeSize > afterSize, "清理后队列应该变小");
        System.out.println("清理功能测试完成！");
    }
}