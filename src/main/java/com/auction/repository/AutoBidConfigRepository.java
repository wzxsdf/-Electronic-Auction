package com.auction.repository;

import com.auction.domain.entity.AutoBidConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.auction.infrastructure.mapper.AutoBidConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AutoBidConfigRepository {

    private final AutoBidConfigMapper mapper;

    public AutoBidConfig save(AutoBidConfig config) {
        if (config.getId() == null) {
            mapper.insert(config);
        } else {
            mapper.updateById(config);
        }
        return config;
    }

    public AutoBidConfig findById(Long id) {
        return mapper.selectById(id);
    }

    public AutoBidConfig findByUserAndItem(Long userId, Long auctionItemId) {
        return mapper.selectOne(
            new LambdaQueryWrapper<AutoBidConfig>()
                .eq(AutoBidConfig::getUserId, userId)
                .eq(AutoBidConfig::getAuctionItemId, auctionItemId)
                .eq(AutoBidConfig::getStatus, "ACTIVE")
        );
    }

    public List<AutoBidConfig> findActiveByItemId(Long auctionItemId) {
        return mapper.selectList(
            new LambdaQueryWrapper<AutoBidConfig>()
                .eq(AutoBidConfig::getAuctionItemId, auctionItemId)
                .eq(AutoBidConfig::getStatus, "ACTIVE")
        );
    }

    public AutoBidConfig findByUserAndAuction(Long userId, Long auctionId) {
        // 注意：重构后不再直接通过auctionId查询，因为AutoBidConfig直接关联auctionItemId
        // 这个方法已废弃，建议使用 findByUserAndItem
        throw new UnsupportedOperationException(
                "findByUserAndAuction方法已废弃，重构后自动出价配置直接关联拍品ID。请使用 findByUserAndItem(userId, auctionItemId) 方法。");
    }

    public List<AutoBidConfig> findActiveAll() {
        return mapper.selectList(
            new LambdaQueryWrapper<AutoBidConfig>()
                .eq(AutoBidConfig::getStatus, "ACTIVE")
        );
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
