package com.auction.infrastructure.redis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

/**
 * Redis Lua 脚本执行服务
 * <p>
 * 负责执行原子性的 Lua 脚本操作，确保高并发场景下的数据一致性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLuaScriptService {

    private final StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<List> placeBidScript;

    /**
     * 初始化 Lua 脚本
     */
    @PostConstruct
    public void init() {
        // 加载出价脚本
        placeBidScript = new DefaultRedisScript<>();
        placeBidScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/place_bid.lua")));
        placeBidScript.setResultType(List.class);

        log.info("Redis Lua 脚本加载完成");
    }

    /**
     * 执行出价 Lua 脚本（原子操作）
     *
     * @param auctionItemId 拍品ID
     * @param userId 用户ID
     * @param bidAmount 出价金额
     * @param bidIncrement 加价幅度
     * @param maxPrice 封顶价（可为null）
     * @param endTime 结束时间（可为null）
     * @param delaySeconds 延时秒数
     * @return 脚本执行结果
     */
    public BidScriptResult executePlaceBid(
            Long auctionItemId,
            Long userId,
            BigDecimal bidAmount,
            BigDecimal bidIncrement,
            BigDecimal maxPrice,
            LocalDateTime endTime,
            Integer delaySeconds
    ) {
        String itemKey = "auction:item:" + auctionItemId;

        // 准备参数
        Long endTimeTimestamp = null;
        if (endTime != null) {
            // LocalDateTime -> ZonedDateTime -> Instant -> epoch second
            java.time.Instant instant = endTime.atZone(ZoneId.systemDefault()).toInstant();
            endTimeTimestamp = instant.getEpochSecond();
        }

        List<String> keys = Collections.singletonList(itemKey);
        List<Object> args = List.of(
                userId.toString(),
                bidAmount.toString(),
                bidIncrement.toString(),
                maxPrice != null ? maxPrice.toString() : "0",
                endTimeTimestamp != null ? endTimeTimestamp.toString() : "0",
                delaySeconds != null ? delaySeconds.toString() : "15",
                String.valueOf(System.currentTimeMillis() / 1000)
        );

        log.debug("执行出价脚本: itemId={}, userId={}, amount={}", auctionItemId, userId, bidAmount);

        // 执行 Lua 脚本（原子操作）
        List<Object> result = stringRedisTemplate.execute(
                placeBidScript,
                keys,
                args.toArray(new Object[0])
        );

        return parseScriptResult(result);
    }

    /**
     * 解析脚本执行结果
     */
    private BidScriptResult parseScriptResult(List<Object> result) {
        if (result == null || result.isEmpty()) {
            return BidScriptResult.error(-999, "脚本执行失败");
        }

        int code = Integer.parseInt(result.get(0).toString());
        String message = result.size() > 1 ? result.get(1).toString() : "";

        // code = 1: 正常出价成功, code = 2: 封顶价成交成功
        if (code != 1 && code != 2) {
            return BidScriptResult.error(code, message);
        }

        // 成功结果
        BigDecimal newPrice = result.size() > 2 ? new BigDecimal(result.get(2).toString()) : null;
        Long newEndTime = result.size() > 3 ? Long.parseLong(result.get(3).toString()) : null;
        Integer delayCount = result.size() > 4 ? Integer.parseInt(result.get(4).toString()) : null;
        Integer bidCount = result.size() > 5 ? Integer.parseInt(result.get(5).toString()) : null;

        LocalDateTime newEndTimeDateTime = null;
        if (newEndTime != null && newEndTime > 0) {
            newEndTimeDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(newEndTime),
                    ZoneId.systemDefault()
            );
        }

        // code = 2 表示达到封顶价自动成交
        boolean maxPriceReached = (code == 2);
        return BidScriptResult.success(newPrice, newEndTimeDateTime, delayCount, bidCount, maxPriceReached);
    }

    /**
     * 脚本执行结果封装
     */
    public static class BidScriptResult {
        private final boolean success;
        private final int code;
        private final String message;
        private final BigDecimal newPrice;
        private final LocalDateTime newEndTime;
        private final Integer delayCount;
        private final Integer bidCount;
        private final boolean maxPriceReached;  // 是否达到封顶价成交

        private BidScriptResult(boolean success, int code, String message,
                               BigDecimal newPrice, LocalDateTime newEndTime,
                               Integer delayCount, Integer bidCount, boolean maxPriceReached) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.newPrice = newPrice;
            this.newEndTime = newEndTime;
            this.delayCount = delayCount;
            this.bidCount = bidCount;
            this.maxPriceReached = maxPriceReached;
        }

        public static BidScriptResult success(BigDecimal newPrice, LocalDateTime newEndTime,
                                            Integer delayCount, Integer bidCount, boolean maxPriceReached) {
            return new BidScriptResult(true, maxPriceReached ? 2 : 1,
                    maxPriceReached ? "达到封顶价，自动成交" : "出价成功",
                    newPrice, newEndTime, delayCount, bidCount, maxPriceReached);
        }

        public static BidScriptResult error(int code, String message) {
            return new BidScriptResult(false, code, message, null, null, null, null, false);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public int getCode() { return code; }
        public String getMessage() { return message; }
        public BigDecimal getNewPrice() { return newPrice; }
        public LocalDateTime getNewEndTime() { return newEndTime; }
        public Integer getDelayCount() { return delayCount; }
        public Integer getBidCount() { return bidCount; }
        public boolean isMaxPriceReached() { return maxPriceReached; }

        @Override
        public String toString() {
            return "BidScriptResult{" +
                    "success=" + success +
                    ", code=" + code +
                    ", message='" + message + '\'' +
                    ", newPrice=" + newPrice +
                    ", newEndTime=" + newEndTime +
                    ", delayCount=" + delayCount +
                    ", bidCount=" + bidCount +
                    ", maxPriceReached=" + maxPriceReached +
                    '}';
        }
    }
}
