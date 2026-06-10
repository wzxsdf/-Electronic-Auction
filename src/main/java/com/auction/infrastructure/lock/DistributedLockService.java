package com.auction.infrastructure.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务
 * 基于 Redisson 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * 执行带锁的操作
     *
     * @param lockKey 锁的键
     * @param action  要执行的操作
     * @param <T>     返回类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，最多等待 5 秒，锁自动释放时间 10 秒
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("获取锁失败，系统繁忙，请稍后重试");
            }
            try {
                return action.get();
            } finally {
                // 检查当前线程是否持有锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取锁被中断", e);
        }
    }

    /**
     * 执行带锁的操作（无返回值）
     *
     * @param lockKey 锁的键
     * @param action  要执行的操作
     */
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁的键
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param unit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的键
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 生成拍卖出价的锁键
     */
    public static String bidLockKey(Long auctionId) {
        return "auction:bid:lock:" + auctionId;
    }

    /**
     * 生成拍卖更新的锁键
     */
    public static String auctionLockKey(Long auctionId) {
        return "auction:update:lock:" + auctionId;
    }

    /**
     * 生成拍品出价的锁键
     */
    public static String auctionItemLockKey(Long auctionItemId) {
        return "auction:item:bid:lock:" + auctionItemId;
    }
}
