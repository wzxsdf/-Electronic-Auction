package com.auction.service.ranking;

import com.auction.api.dto.response.BidRankingListResponse;
import com.auction.api.dto.response.BidRankingResponse;
import com.auction.domain.entity.AuctionItem;
import com.auction.domain.entity.BidRankingHistory;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.redis.RedisService;
import com.auction.repository.AuctionItemRepository;
import com.auction.repository.BidRankingHistoryRepository;
import com.auction.repository.BidRepository;
import com.auction.repository.UserRepository;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 竞拍排行榜服务（Redis缓存优化 + WebSocket推送 + 历史数据分析）
 * <p>
 * 核心功能：
 * 1. Redis ZSet缓存排行榜数据，提供高性能查询
 * 2. WebSocket实时推送排行榜变化
 * 3. 定时保存历史排行快照，支持数据分析
 * 4. 多维度排行榜分析和趋势预测
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BidRankingService {

    private final RedisService redisService;
    private final WsMessageService wsMessageService;
    private final BidRepository bidRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final UserRepository userRepository;
    private final BidRankingHistoryRepository bidRankingHistoryRepository;

    // Redis Key 前缀
    private static final String RANKING_KEY_PREFIX = "auction:ranking:";
    private static final String RANKING_CACHE_KEY = "auction:ranking:cache:";
    private static final String RANKING_CHANGE_LOG = "auction:ranking:change:";

    // 缓存时间配置
    private static final long CACHE_EXPIRE_MINUTES = 5; // 排行榜缓存5分钟
    private static final long HISTORY_EXPIRE_DAYS = 30;  // 历史数据保留30天

    /**
     * 获取实时排行榜（Redis缓存优化）
     * <p>
     * 查询流程：
     * 1. 先从Redis缓存获取排行榜数据
     * 2. 如果缓存不存在，从数据库查询并缓存到Redis
     * 3. 设置缓存过期时间，确保数据时效性
     *
     * @param auctionItemId 拍品ID
     * @param limit 返回数量限制
     * @return 排行榜数据
     */
    public BidRankingListResponse getRealTimeRanking(Long auctionItemId, int limit) {
        try {
            String cacheKey = RANKING_CACHE_KEY + auctionItemId;

            // 1. 尝试从Redis缓存获取
            BidRankingListResponse cached = redisService.get(cacheKey, BidRankingListResponse.class);
            if (cached != null) {
                log.debug("从Redis缓存获取排行榜数据: auctionItemId={}", auctionItemId);
                return limitRankingResponse(cached, limit);
            }

            // 2. 缓存不存在，从数据库查询
            log.debug("缓存未命中，从数据库查询排行榜: auctionItemId={}", auctionItemId);
            BidRankingListResponse ranking = buildRankingFromDatabase(auctionItemId, limit);

            // 3. 缓存到Redis
            if (ranking != null) {
                redisService.set(cacheKey, ranking, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                log.debug("排行榜数据已缓存到Redis: auctionItemId={}", auctionItemId);
            }

            return ranking;

        } catch (Exception e) {
            log.error("获取排行榜失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
            return buildEmptyRanking(auctionItemId);
        }
    }

    /**
     * 更新排行榜（出价后调用）
     * <p>
     * 更新流程：
     * 1. 删除Redis缓存，强制下次重新查询
     * 2. 更新Redis ZSet中的用户排名
     * 3. 通过WebSocket推送排行榜变化
     * 4. 记录排行榜变化日志
     *
     * @param auctionItemId 拍品ID
     * @param userId 出价用户ID
     * @param amount 出价金额
     */
    public void updateRanking(Long auctionItemId, Long userId, BigDecimal amount) {
        try {
            // 1. 删除缓存，强制下次重新查询
            String cacheKey = RANKING_CACHE_KEY + auctionItemId;
            redisService.delete(cacheKey);

            // 2. 更新Redis ZSet排行榜（分数=出价金额）
            String zSetKey = RANKING_KEY_PREFIX + auctionItemId;
            redisService.zAdd(zSetKey, userId, amount.doubleValue());

            // 3. 记录变化日志
            String changeLogKey = RANKING_CHANGE_LOG + auctionItemId;
            Map<String, Object> changeLog = Map.of(
                "userId", userId,
                "amount", amount,
                "timestamp", System.currentTimeMillis(),
                "action", "BID_PLACED"
            );
            redisService.sAdd(changeLogKey, changeLog);

            // 4. 通过WebSocket推送排行榜更新
            broadcastRankingUpdate(auctionItemId, userId, amount);

            log.info("排行榜更新成功: auctionItemId={}, userId={}, amount={}", auctionItemId, userId, amount);

        } catch (Exception e) {
            log.error("更新排行榜失败: auctionItemId={}, userId={}, error={}", auctionItemId, userId, e.getMessage(), e);
        }
    }

    /**
     * 通过WebSocket推送排行榜更新
     */
    private void broadcastRankingUpdate(Long auctionItemId, Long userId, BigDecimal amount) {
        try {
            // 获取最新的排行榜数据
            BidRankingListResponse ranking = getRealTimeRanking(auctionItemId, 10);

            // 构建推送消息
            Map<String, Object> rankingData = new HashMap<>();
            rankingData.put("auctionItemId", auctionItemId);
            rankingData.put("totalParticipants", ranking.getTotalParticipants());
            rankingData.put("currentPrice", ranking.getCurrentPrice());
            rankingData.put("bidIncrement", ranking.getBidIncrement());
            rankingData.put("topRankings", ranking.getRankings().stream()
                .limit(5) // 只推送前5名
                .collect(Collectors.toList()));

            // 使用新的WebSocket推送方法
            wsMessageService.sendRankingUpdate(auctionItemId, rankingData);

            log.debug("排行榜更新已推送到WebSocket: auctionItemId={}", auctionItemId);

        } catch (Exception e) {
            log.error("推送排行榜更新失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
        }
    }

    /**
     * 保存排行榜历史快照（定时任务调用）
     * <p>
     * 保存策略：
     * 1. 每10分钟保存一次活跃拍品的排行榜快照
     * 2. 拍品结束时保存最终结果
     * 3. 关键事件时保存快照（如价格突破、领先者变更）
     */
    @Scheduled(cron = "0 */10 * * * *") // 每10分钟执行一次
    public void saveRankingSnapshot() {
        try {
            log.info("开始保存排行榜历史快照...");

            // 获取所有活跃拍品
            List<AuctionItem> activeItems = auctionItemRepository.findByStatus(AuctionStatus.ACTIVE);

            int savedCount = 0;
            for (AuctionItem item : activeItems) {
                try {
                    // 保存当前排行榜快照
                    saveRankingSnapshot(item.getId(), BidRankingHistory.SnapshotType.HOURLY.name());
                    savedCount++;
                } catch (Exception e) {
                    log.error("保存拍品排行榜快照失败: auctionItemId={}, error={}", item.getId(), e.getMessage());
                }
            }

            log.info("排行榜历史快照保存完成: 总数={}", savedCount);

        } catch (Exception e) {
            log.error("保存排行榜历史快照失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 保存指定拍品的排行榜历史快照
     */
    public void saveRankingSnapshot(Long auctionItemId, String snapshotType) {
        try {
            AuctionItem item = auctionItemRepository.findById(auctionItemId);
            if (item == null) {
                log.warn("拍品不存在，无法保存排行榜快照: auctionItemId={}", auctionItemId);
                return;
            }

            // 获取当前排行榜数据
            BidRankingListResponse ranking = getRealTimeRanking(auctionItemId, 100);

            if (ranking == null || ranking.getRankings().isEmpty()) {
                log.debug("排行榜为空，跳过保存: auctionItemId={}", auctionItemId);
                return;
            }

            // 批量保存排行榜历史记录
            List<BidRankingHistory> histories = new ArrayList<>();
            LocalDateTime snapshotTime = LocalDateTime.now();

            for (BidRankingResponse rankData : ranking.getRankings()) {
                BidRankingHistory history = BidRankingHistory.builder()
                    .auctionItemId(auctionItemId)
                    .auctionId(item.getAuctionId())
                    .auctionItemTitle(item.getTitle())
                    .rankingPosition(rankData.getRank())
                    .userId(rankData.getUserId())
                    .maskedUsername(rankData.getMaskedUsername())
                    .highestBidAmount(rankData.getHighestBidAmount())
                    .bidCount(rankData.getBidCount())
                    .lastBidTime(rankData.getLastBidTime())
                    .isHighestBidder(rankData.getIsHighestBidder())
                    .currentPrice(ranking.getCurrentPrice())
                    .totalParticipants(ranking.getTotalParticipants().intValue())
                    .itemStatus(item.getStatus())
                    .snapshotTime(snapshotTime)
                    .snapshotType(snapshotType)
                    .build();

                histories.add(history);
            }

            // 批量保存到数据库
            bidRankingHistoryRepository.batchSave(histories);

            log.info("排行榜快照保存成功: auctionItemId={}, 记录数={}, 类型={}",
                auctionItemId, histories.size(), snapshotType);

        } catch (Exception e) {
            log.error("保存排行榜快照失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
        }
    }

    /**
     * 拍品结束时保存最终排行榜结果
     */
    public void saveFinalRanking(Long auctionItemId) {
        saveRankingSnapshot(auctionItemId, BidRankingHistory.SnapshotType.FINAL.name());
        log.info("最终排行榜结果已保存: auctionItemId={}", auctionItemId);
    }

    /**
     * 获取排行榜历史分析数据
     */
    public Map<String, Object> getRankingAnalysis(Long auctionItemId) {
        try {
            // 1. 获取历史趋势数据
            List<Map<String, Object>> trends = bidRankingHistoryRepository.findTrendsData(auctionItemId);

            // 2. 获取最新排行榜
            BidRankingListResponse currentRanking = getRealTimeRanking(auctionItemId, 10);

            // 3. 统计参与用户的历史最佳排名
            Set<Long> participantIds = currentRanking.getRankings().stream()
                .map(BidRankingResponse::getUserId)
                .collect(Collectors.toSet());

            Map<Long, Integer> bestRanks = new HashMap<>();
            for (Long userId : participantIds) {
                Integer bestRank = bidRankingHistoryRepository.findBestRankByUser(auctionItemId, userId);
                if (bestRank != null) {
                    bestRanks.put(userId, bestRank);
                }
            }

            // 4. 统计总出价次数和参与人数变化
            long totalHistoryRecords = bidRankingHistoryRepository.countByAuctionItemId(auctionItemId);

            return Map.of(
                "auctionItemId", auctionItemId,
                "currentRanking", currentRanking,
                "trends", trends,
                "participantBestRanks", bestRanks,
                "totalHistoryRecords", totalHistoryRecords,
                "analysisTime", LocalDateTime.now()
            );

        } catch (Exception e) {
            log.error("获取排行榜分析数据失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取用户的竞拍历史表现
     */
    public Map<String, Object> getUserRankingHistory(Long userId, int limit) {
        try {
            List<BidRankingHistory> userHistories = bidRankingHistoryRepository.findByUserId(userId, limit);

            // 统计用户的表现数据
            Map<Long, List<Integer>> itemRanks = userHistories.stream()
                .collect(Collectors.groupingBy(
                    BidRankingHistory::getAuctionItemId,
                    Collectors.mapping(BidRankingHistory::getRankingPosition, Collectors.toList())
                ));

            // 计算每个拍品的最佳排名
            Map<Long, Integer> bestRanks = new HashMap<>();
            for (Map.Entry<Long, List<Integer>> entry : itemRanks.entrySet()) {
                bestRanks.put(entry.getKey(), Collections.min(entry.getValue()));
            }

            // 统计总体表现
            long totalParticipations = itemRanks.size();
            long totalFirstPlace = bestRanks.values().stream().filter(rank -> rank == 1).count();
            double avgRank = bestRanks.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

            return Map.of(
                "userId", userId,
                "totalParticipations", totalParticipations,
                "totalFirstPlace", totalFirstPlace,
                "averageRank", String.format("%.2f", avgRank),
                "bestRanksByItem", bestRanks,
                "recentHistories", userHistories,
                "analysisTime", LocalDateTime.now()
            );

        } catch (Exception e) {
            log.error("获取用户竞拍历史失败: userId={}, error={}", userId, e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 清理过期的排行榜缓存和历史数据
     */
    @Scheduled(cron = "0 0 2 * * *") // 每天凌晨2点执行
    public void cleanupExpiredData() {
        try {
            log.info("开始清理过期排行榜数据...");

            LocalDateTime cleanupTime = LocalDateTime.now().minusDays(HISTORY_EXPIRE_DAYS);

            // 获取所有拍品ID（这里简化处理，实际应该分批处理）
            List<AuctionItem> allItems = auctionItemRepository.findAll();

            int cleanedCount = 0;
            for (AuctionItem item : allItems) {
                try {
                    int deleted = bidRankingHistoryRepository.deleteOldHistories(item.getId(), cleanupTime);
                    cleanedCount += deleted;
                } catch (Exception e) {
                    log.error("清理拍品历史数据失败: auctionItemId={}, error={}", item.getId(), e.getMessage());
                }
            }

            log.info("过期排行榜数据清理完成: 清理数量={}", cleanedCount);

        } catch (Exception e) {
            log.error("清理过期排行榜数据失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 从数据库构建排行榜数据
     */
    private BidRankingListResponse buildRankingFromDatabase(Long auctionItemId, int limit) {
        try {
            // 获取拍品信息
            AuctionItem item = auctionItemRepository.findById(auctionItemId);
            if (item == null) {
                return buildEmptyRanking(auctionItemId);
            }

            // 获取排行榜数据（已按用户分组和排序）
            List<Map<String, Object>> rankingData = bidRepository.getBidRanking(auctionItemId, limit);

            if (rankingData == null || rankingData.isEmpty()) {
                return buildEmptyRanking(auctionItemId);
            }

            // 构建排行榜列表
            List<BidRankingResponse> rankingList = new ArrayList<>();
            Long currentHighestBidder = item.getHighestBidder();

            int rank = 1;
            for (Map<String, Object> userData : rankingData) {
                Long userId = ((Number) userData.get("user_id")).longValue();
                BigDecimal highestBidAmount = (BigDecimal) userData.get("highest_bid_amount");
                Long bidCount = ((Number) userData.get("bid_count")).longValue();
                java.sql.Timestamp lastBidTime = (java.sql.Timestamp) userData.get("last_bid_time");

                // 获取用户信息
                com.auction.domain.entity.User user = userRepository.findById(userId);
                String username = user != null ? user.getNickname() : "未知用户";
                String maskedUsername = maskUsername(username);

                BidRankingResponse ranking = BidRankingResponse.builder()
                    .rank(rank++)
                    .userId(userId)
                    .maskedUsername(maskedUsername)
                    .highestBidAmount(highestBidAmount)
                    .bidCount(bidCount.intValue())
                    .lastBidTime(lastBidTime != null ? lastBidTime.toLocalDateTime() : null)
                    .isHighestBidder(currentHighestBidder != null && currentHighestBidder.equals(userId))
                    .hasAutoBidEnabled(false)
                    .build();

                rankingList.add(ranking);
            }

            return BidRankingListResponse.builder()
                .auctionItemId(auctionItemId)
                .auctionId(item.getAuctionId())
                .totalParticipants((long) rankingList.size())
                .rankings(rankingList)
                .currentPrice(item.getCurrentPrice())
                .bidIncrement(item.getBidIncrement())
                .build();

        } catch (Exception e) {
            log.error("从数据库构建排行榜失败: auctionItemId={}, error={}", auctionItemId, e.getMessage(), e);
            return buildEmptyRanking(auctionItemId);
        }
    }

    /**
     * 构建空排行榜
     */
    private BidRankingListResponse buildEmptyRanking(Long auctionItemId) {
        return BidRankingListResponse.builder()
            .auctionItemId(auctionItemId)
            .totalParticipants(0L)
            .rankings(new ArrayList<>())
            .currentPrice(BigDecimal.ZERO)
            .bidIncrement(BigDecimal.ZERO)
            .build();
    }

    /**
     * 限制排行榜返回数量
     */
    private BidRankingListResponse limitRankingResponse(BidRankingListResponse ranking, int limit) {
        if (ranking == null || ranking.getRankings() == null) {
            return ranking;
        }

        List<BidRankingResponse> limitedRankings = ranking.getRankings().stream()
            .limit(limit)
            .collect(Collectors.toList());

        ranking.setRankings(limitedRankings);
        return ranking;
    }

    /**
     * 用户名脱敏处理
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return "***";
        }
        return username.substring(0, 2) + "***";
    }
}
