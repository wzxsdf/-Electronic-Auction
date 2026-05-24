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

    public AutoBidConfig findByUserAndAuction(Long userId, Long auctionId) {
        return mapper.selectOne(
            new LambdaQueryWrapper<AutoBidConfig>()
                .eq(AutoBidConfig::getUserId, userId)
                .eq(AutoBidConfig::getAuctionId, auctionId)
        );
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
