package com.auction.repository;

import com.auction.domain.entity.BidRankingHistory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.auction.infrastructure.mapper.BidRankingHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞拍排行榜历史数据仓库
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BidRankingHistoryRepository {

    private final BidRankingHistoryMapper bidRankingHistoryMapper;

    /**
     * 保存排行榜历史记录
     */
    public BidRankingHistory save(BidRankingHistory history) {
        if (history.getId() == null) {
            history.setCreatedAt(LocalDateTime.now());
            bidRankingHistoryMapper.insert(history);
        } else {
            bidRankingHistoryMapper.updateById(history);
        }
        return history;
    }

    /**
     * 批量保存排行榜历史记录
     */
    public void batchSave(List<BidRankingHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return;
        }

        histories.forEach(history -> {
            if (history.getCreatedAt() == null) {
                history.setCreatedAt(LocalDateTime.now());
            }
        });

        histories.forEach(bidRankingHistoryMapper::insert);
        log.info("批量保存排行榜历史记录成功: 数量={}", histories.size());
    }

    /**
     * 查询指定拍品的历史排行榜记录
     */
    public List<BidRankingHistory> findByAuctionItemId(Long auctionItemId, int limit) {
        return bidRankingHistoryMapper.findByAuctionItemId(auctionItemId, limit);
    }

    /**
     * 查询指定时间范围内的排行榜历史
     */
    public List<BidRankingHistory> findByTimeRange(Long auctionItemId, LocalDateTime startTime, LocalDateTime endTime) {
        return bidRankingHistoryMapper.findByTimeRange(auctionItemId, startTime, endTime);
    }

    /**
     * 查询指定类型的排行榜历史
     */
    public List<BidRankingHistory> findBySnapshotType(Long auctionItemId, String snapshotType, int limit) {
        return bidRankingHistoryMapper.findBySnapshotType(auctionItemId, snapshotType, limit);
    }

    /**
     * 统计用户在指定拍品的历史最佳排名
     */
    public Integer findBestRankByUser(Long auctionItemId, Long userId) {
        Integer bestRank = bidRankingHistoryMapper.findBestRankByUser(auctionItemId, userId);
        return bestRank != null && bestRank > 0 ? bestRank : null;
    }

    /**
     * 查询用户的排行榜历史记录
     */
    public List<BidRankingHistory> findByUserId(Long userId, int limit) {
        return bidRankingHistoryMapper.findByUserId(userId, limit);
    }

    /**
     * 统计拍品的总参与人数历史趋势
     */
    public List<java.util.Map<String, Object>> findTrendsData(Long auctionItemId) {
        return bidRankingHistoryMapper.findTrendsData(auctionItemId);
    }

    /**
     * 查询最新的排行榜记录
     */
    public BidRankingHistory findLatestByAuctionItemId(Long auctionItemId) {
        List<BidRankingHistory> histories = findByAuctionItemId(auctionItemId, 1);
        return histories.isEmpty() ? null : histories.get(0);
    }

    /**
     * 删除指定拍品的旧历史记录（清理数据）
     */
    public int deleteOldHistories(Long auctionItemId, LocalDateTime beforeTime) {
        return bidRankingHistoryMapper.delete(
            new LambdaQueryWrapper<BidRankingHistory>()
                .eq(BidRankingHistory::getAuctionItemId, auctionItemId)
                .lt(BidRankingHistory::getSnapshotTime, beforeTime)
        );
    }

    /**
     * 统计指定拍品的历史记录数量
     */
    public long countByAuctionItemId(Long auctionItemId) {
        return bidRankingHistoryMapper.selectCount(
            new LambdaQueryWrapper<BidRankingHistory>()
                .eq(BidRankingHistory::getAuctionItemId, auctionItemId)
        );
    }
}
