package com.auction.repository;

import com.auction.domain.entity.RiskEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.auction.infrastructure.mapper.RiskEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RiskEventRepository {

    private final RiskEventMapper mapper;

    public RiskEvent save(RiskEvent event) {
        mapper.insert(event);
        return event;
    }

    public List<RiskEvent> findRecentByUserId(Long userId, int limit) {
        return mapper.selectList(
            new LambdaQueryWrapper<RiskEvent>()
                .eq(RiskEvent::getUserId, userId)
                .orderByDesc(RiskEvent::getCreatedAt)
                .last("LIMIT " + limit)
        );
    }

    public List<RiskEvent> findHighSeverityUnresolved() {
        return mapper.selectList(
            new LambdaQueryWrapper<RiskEvent>()
                .eq(RiskEvent::getSeverity, "HIGH")
                .orderByDesc(RiskEvent::getCreatedAt)
        );
    }

    public IPage<RiskEvent> findByPage(int pageNum, int pageSize) {
        Page<RiskEvent> page = new Page<>(pageNum, pageSize);
        return mapper.selectPage(page, null);
    }

    public List<RiskEvent> findBySeverity(String severity) {
        return mapper.selectList(
            new LambdaQueryWrapper<RiskEvent>()
                .eq(RiskEvent::getSeverity, severity)
                .orderByDesc(RiskEvent::getCreatedAt)
        );
    }
}
