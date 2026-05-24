package com.auction.concurrent;

import com.auction.infrastructure.lock.DistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发出价测试
 * 验证分布式锁和乐观锁的并发安全性
 */
@Slf4j
@SpringBootTest
public class ConcurrentBidTest {

    @Autowired(required = false)
    private DistributedLockService distributedLockService;

    /**
     * 测试分布式锁的基本功能
     */
    @Test
    public void testDistributedLock() {
        if (distributedLockService == null) {
            log.warn("DistributedLockService 未注入，跳过测试");
            return;
        }

        String lockKey = "test:lock:1";
        AtomicInteger counter = new AtomicInteger(0);

        // 创建 10 个线程并发执行
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    distributedLockService.executeWithLock(lockKey, () -> {
                        // 模拟业务操作
                        int value = counter.incrementAndGet();
                        log.info("当前值: {}, 线程: {}", value, Thread.currentThread().getName());
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        log.info("最终值: {}, 预期值: {}", counter.get(), threadCount);

        // 验证：最终值应该等于线程数（无并发问题）
        assert counter.get() == threadCount : "并发安全验证失败！";
    }

    /**
     * 测试模拟并发出价场景
     */
    @Test
    public void testConcurrentBidding() throws InterruptedException {
        if (distributedLockService == null) {
            log.warn("DistributedLockService 未注入，跳过测试");
            return;
        }

        Long auctionId = 1L;
        String lockKey = DistributedLockService.auctionLockKey(auctionId);

        // 模拟当前价格
        ConcurrentHashMap<String, BigDecimal> priceStorage = new ConcurrentHashMap<>();
        priceStorage.put("auction:" + auctionId + ":price", new BigDecimal("100.00"));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<BigDecimal> finalPrices = new CopyOnWriteArrayList<>();

        int userCount = 50; // 50 个用户同时出价
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        for (int i = 0; i < userCount; i++) {
            final int userId = i + 1;
            final BigDecimal bidAmount = new BigDecimal(100 + i * 10);

            executor.submit(() -> {
                try {
                    boolean locked = distributedLockService.tryLock(lockKey, 3, 5, TimeUnit.SECONDS);
                    if (locked) {
                        try {
                            BigDecimal currentPrice = priceStorage.get("auction:" + auctionId + ":price");
                            if (bidAmount.compareTo(currentPrice) > 0) {
                                priceStorage.put("auction:" + auctionId + ":price", bidAmount);
                                successCount.incrementAndGet();
                                log.info("用户 {} 出价 {} 成功", userId, bidAmount);
                            } else {
                                conflictCount.incrementAndGet();
                                log.info("用户 {} 出价 {} 失败（价格过低）", userId, bidAmount);
                            }
                        } finally {
                            distributedLockService.unlock(lockKey);
                        }
                    } else {
                        conflictCount.incrementAndGet();
                        log.warn("用户 {} 获取锁失败", userId);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal finalPrice = priceStorage.get("auction:" + auctionId + ":price");
        log.info("========================================");
        log.info("并发测试结果:");
        log.info("参与用户: {}", userCount);
        log.info("成功出价: {}", successCount.get());
        log.info("冲突/失败: {}", conflictCount.get());
        log.info("最终价格: {}", finalPrice);
        log.info("========================================");

        // 验证：最终价格应该是最高的出价
        BigDecimal expectedPrice = new BigDecimal(100 + (userCount - 1) * 10);
        assert finalPrice.equals(expectedPrice) : "价格不一致！预期: " + expectedPrice + ", 实际: " + finalPrice;
    }

    /**
     * 性能压力测试
     */
    @Test
    public void testLockPerformance() throws InterruptedException {
        if (distributedLockService == null) {
            log.warn("DistributedLockService 未注入，跳过测试");
            return;
        }

        String lockKey = "test:perf:lock";
        int totalRequests = 1000;
        int threadCount = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    boolean result = distributedLockService.tryLock(lockKey, 2, 3, TimeUnit.SECONDS);
                    if (result) {
                        try {
                            // 模拟业务处理
                            Thread.sleep(10);
                            successCount.incrementAndGet();
                        } finally {
                            distributedLockService.unlock(lockKey);
                        }
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("请求异常: {}", requestId, e);
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(120, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("========================================");
        log.info("性能测试结果:");
        log.info("总请求数: {}", totalRequests);
        log.info("成功: {}, 失败: {}", successCount.get(), failCount.get());
        log.info("总耗时: {} ms", duration);
        log.info("平均 QPS: {}", (successCount.get() * 1000L) / duration);
        log.info("========================================");
    }
}
