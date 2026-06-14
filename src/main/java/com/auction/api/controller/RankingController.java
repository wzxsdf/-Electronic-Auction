package com.auction.api.controller;

import com.auction.api.dto.response.BidRankingListResponse;
import com.auction.common.Result;
import com.auction.service.ranking.BidRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 排行榜分析控制器
 * <p>
 * 提供排行榜历史数据分析和趋势查询接口
 */
@Slf4j
@RestController
@RequestMapping("/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final BidRankingService bidRankingService;

    /**
     * 获取排行榜历史分析数据
     * GET /rankings/analysis/{auctionItemId}
     *
     * @param auctionItemId 拍品ID
     * @return 排行榜历史分析数据
     */
    @GetMapping("/analysis/{auctionItemId}")
    public Result<Map<String, Object>> getRankingAnalysis(@PathVariable Long auctionItemId) {
        try {
            if (auctionItemId == null || auctionItemId <= 0) {
                return Result.fail(400, "拍品ID无效");
            }

            Map<String, Object> analysis = bidRankingService.getRankingAnalysis(auctionItemId);
            return Result.ok(analysis);

        } catch (Exception e) {
            log.error("获取排行榜分析失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
            return Result.fail(500, "获取排行榜分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的竞拍历史表现
     * GET /rankings/user/{userId}/history
     *
     * @param userId 用户ID
     * @param limit 返回记录数量限制（默认20条，最大100条）
     * @return 用户竞拍历史表现数据
     */
    @GetMapping("/user/{userId}/history")
    public Result<Map<String, Object>> getUserRankingHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            if (userId == null || userId <= 0) {
                return Result.fail(400, "用户ID无效");
            }
            if (limit <= 0) {
                return Result.fail(400, "limit参数必须大于0");
            }
            int actualLimit = Math.min(limit, 100);

            Map<String, Object> history = bidRankingService.getUserRankingHistory(userId, actualLimit);
            return Result.ok(history);

        } catch (Exception e) {
            log.error("获取用户竞拍历史失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail(500, "获取用户竞拍历史失败: " + e.getMessage());
        }
    }

    /**
     * 手动保存排行榜快照（管理员功能）
     * POST /rankings/snapshot/{auctionItemId}
     *
     * @param auctionItemId 拍品ID
     * @param snapshotType 快照类型（HOURLY, DAILY, EVENT, FINAL）
     * @return 操作结果
     */
    @PostMapping("/snapshot/{auctionItemId}")
    public Result<Void> saveRankingSnapshot(
            @PathVariable Long auctionItemId,
            @RequestParam(defaultValue = "EVENT") String snapshotType) {
        try {
            if (auctionItemId == null || auctionItemId <= 0) {
                return Result.fail(400, "拍品ID无效");
            }

            bidRankingService.saveRankingSnapshot(auctionItemId, snapshotType);
            return Result.ok();

        } catch (Exception e) {
            log.error("保存排行榜快照失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
            return Result.fail(500, "保存排行榜快照失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时排行榜（支持WebSocket订阅）
     * GET /rankings/realtime/{auctionItemId}
     *
     * @param auctionItemId 拍品ID
     * @param limit 返回数量限制（默认10条，最大50条）
     * @return 实时排行榜数据
     */
    @GetMapping("/realtime/{auctionItemId}")
    public Result<BidRankingListResponse> getRealTimeRanking(
            @PathVariable Long auctionItemId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (auctionItemId == null || auctionItemId <= 0) {
                return Result.fail(400, "拍品ID无效");
            }
            if (limit <= 0) {
                return Result.fail(400, "limit参数必须大于0");
            }
            int actualLimit = Math.min(limit, 50);

            BidRankingListResponse ranking = bidRankingService.getRealTimeRanking(auctionItemId, actualLimit);
            return Result.ok(ranking);

        } catch (Exception e) {
            log.error("获取实时排行榜失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
            return Result.fail(500, "获取实时排行榜失败: " + e.getMessage());
        }
    }

    /**
     * WebSocket推送排行榜更新
     * <p>
     * 客户端可以通过WebSocket订阅排行榜实时更新：
     * 1. 连接到WebSocket服务器
     * 2. 加入房间：item:{auctionItemId}
     * 3. 接收LEADERBOARD_UPDATE类型的消息
     * <p>
     * 消息格式：
     * {
     *   "type": "LEADERBOARD_UPDATE",
     *   "data": {
     *     "auctionItemId": 123,
     *     "totalParticipants": 8,
     *     "currentPrice": 1500.00,
     *     "topRankings": [...]
     *   },
     *   "timestamp": 1234567890
     * }
     */
    @GetMapping("/websocket-info")
    public Result<Map<String, Object>> getWebSocketInfo() {
        try {
            Map<String, Object> info = Map.of(
                "websocketEndpoint", "ws://localhost:8080/ws",
                "messageTypes", Map.of(
                    "LEADERBOARD_UPDATE", "排行榜更新消息",
                    "NEW_BID", "新出价消息",
                    "PRICE_UPDATE", "价格更新消息"
                ),
                "subscriptionFormat", "item:{auctionItemId}",
                "example", Map.of(
                    "action", "subscribe_to_ranking",
                    "auctionItemId", 123,
                    "description", "订阅拍品ID为123的排行榜更新"
                ),
                "messageExample", Map.of(
                    "type", "LEADERBOARD_UPDATE",
                    "data", Map.of(
                        "auctionItemId", 123,
                        "totalParticipants", 8,
                        "currentPrice", 1500.00,
                        "topRankings", "前5名排行榜数据"
                    ),
                    "timestamp", "消息时间戳"
                )
            );

            return Result.ok(info);

        } catch (Exception e) {
            log.error("获取WebSocket信息失败: error={}", e.getMessage(), e);
            return Result.fail(500, "获取WebSocket信息失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期数据（管理员功能）
     * DELETE /rankings/cleanup
     *
     * @return 操作结果
     */
    @DeleteMapping("/cleanup")
    public Result<Map<String, Object>> cleanupExpiredData() {
        try {
            bidRankingService.cleanupExpiredData();

            Map<String, Object> result = Map.of(
                "message", "过期数据清理任务已启动",
                "executeTime", "每天凌晨2点自动执行",
                "retentionDays", 30
            );

            return Result.ok(result);

        } catch (Exception e) {
            log.error("清理过期数据失败: error={}", e.getMessage(), e);
            return Result.fail(500, "清理过期数据失败: " + e.getMessage());
        }
    }
}
