package com.auction.api.controller;

import com.auction.api.dto.response.BidRankingListResponse;
import com.auction.common.Result;
import com.auction.service.ranking.BidRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 排行榜系统测试控制器
 * <p>
 * 用于测试和验证排行榜功能的控制器
 */
@Slf4j
@RestController
@RequestMapping("/test/ranking")
@RequiredArgsConstructor
public class RankingTestController {

    private final BidRankingService bidRankingService;

    /**
     * 测试实时排行榜功能
     */
    @GetMapping("/realtime/{auctionItemId}")
    public Result<BidRankingListResponse> testRealTimeRanking(
            @PathVariable Long auctionItemId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("测试实时排行榜: auctionItemId={}, limit={}", auctionItemId, limit);

            BidRankingListResponse ranking = bidRankingService.getRealTimeRanking(auctionItemId, limit);

            return Result.ok(ranking);

        } catch (Exception e) {
            log.error("测试实时排行榜失败: error={}", e.getMessage(), e);
            return Result.fail(500, "测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试排行榜更新功能
     */
    @PostMapping("/update/{auctionItemId}")
    public Result<String> testUpdateRanking(
            @PathVariable Long auctionItemId,
            @RequestParam Long userId,
            @RequestParam java.math.BigDecimal amount) {
        try {
            log.info("测试排行榜更新: auctionItemId={}, userId={}, amount={}",
                auctionItemId, userId, amount);

            bidRankingService.updateRanking(auctionItemId, userId, amount);

            return Result.ok("排行榜更新成功");

        } catch (Exception e) {
            log.error("测试排行榜更新失败: error={}", e.getMessage(), e);
            return Result.fail(500, "测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试排行榜快照保存功能
     */
    @PostMapping("/snapshot/{auctionItemId}")
    public Result<String> testSaveSnapshot(
            @PathVariable Long auctionItemId,
            @RequestParam(defaultValue = "TEST") String snapshotType) {
        try {
            log.info("测试排行榜快照保存: auctionItemId={}, snapshotType={}",
                auctionItemId, snapshotType);

            bidRankingService.saveRankingSnapshot(auctionItemId, snapshotType);

            return Result.ok("排行榜快照保存成功");

        } catch (Exception e) {
            log.error("测试排行榜快照保存失败: error={}", e.getMessage(), e);
            return Result.fail(500, "测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试排行榜分析功能
     */
    @GetMapping("/analysis/{auctionItemId}")
    public Result<Map<String, Object>> testRankingAnalysis(@PathVariable Long auctionItemId) {
        try {
            log.info("测试排行榜分析: auctionItemId={}", auctionItemId);

            Map<String, Object> analysis = bidRankingService.getRankingAnalysis(auctionItemId);

            return Result.ok(analysis);

        } catch (Exception e) {
            log.error("测试排行榜分析失败: error={}", e.getMessage(), e);
            return Result.fail(500, "测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试用户历史表现功能
     */
    @GetMapping("/user-history/{userId}")
    public Result<Map<String, Object>> testUserRankingHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            log.info("测试用户竞拍历史: userId={}, limit={}", userId, limit);

            Map<String, Object> history = bidRankingService.getUserRankingHistory(userId, limit);

            return Result.ok(history);

        } catch (Exception e) {
            log.error("测试用户竞拍历史失败: error={}", e.getMessage(), e);
            return Result.fail(500, "测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试数据清理功能
     */
    @PostMapping("/cleanup")
    public Result<String> testCleanup() {
        try {
            log.info("测试数据清理功能");

            bidRankingService.cleanupExpiredData();

            return Result.ok("数据清理测试完成");

        } catch (Exception e) {
            log.error("测试数据清理失败: error={}", e.getMessage(), e);
            return Result.fail(500, "测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取排行榜系统状态信息
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = Map.of(
                "system", "竞拍排行榜系统",
                "version", "1.0.0",
                "features", Map.of(
                    "redisCache", "已启用",
                    "websocketPush", "已启用",
                    "historyAnalysis", "已启用",
                    "scheduledTasks", "已启用"
                ),
                "cacheConfig", Map.of(
                    "expireMinutes", 5,
                    "historyExpireDays", 30
                ),
                "scheduledTasks", Map.of(
                    "snapshotCron", "0 */10 * * * *",
                    "cleanupCron", "0 0 2 * * *"
                ),
                "endpoints", Map.of(
                    "realtimeRanking", "/bids/auction-item/{auctionItemId}/ranking",
                    "rankingAnalysis", "/rankings/analysis/{auctionItemId}",
                    "userHistory", "/rankings/user/{userId}/history",
                    "websocketInfo", "/rankings/websocket-info"
                )
            );

            return Result.ok(status);

        } catch (Exception e) {
            log.error("获取系统状态失败: error={}", e.getMessage(), e);
            return Result.fail(500, "获取系统状态失败: " + e.getMessage());
        }
    }
}
