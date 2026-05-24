package com.auction.repository;

import com.auction.domain.entity.Bid;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.auction.infrastructure.mapper.BidMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BidRepository {

    private final BidMapper bidMapper;

    public Bid save(Bid bid) {
        bidMapper.insert(bid);
        return bid;
    }

    public List<Bid> findByItemId(Long itemId) {
        return bidMapper.getBidsByItemId(itemId);
    }

    public List<Bid> findByRoomId(Long roomId) {
        return bidMapper.getBidsByRoomId(roomId);
    }

    public List<Bid> findByItemIdAndUserId(Long itemId, Long userId) {
        return bidMapper.selectList(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getItemId, itemId)
                .eq(Bid::getUserId, userId)
                .orderByDesc(Bid::getCreatedAt)
        );
    }

    public Bid findHighestByItemId(Long itemId) {
        return bidMapper.selectOne(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getItemId, itemId)
                .eq(Bid::getStatus, "ACTIVE")
                .orderByDesc(Bid::getAmount)
                .last("LIMIT 1")
        );
    }

    public List<Bid> findRecentByItemId(Long itemId, int limit) {
        return bidMapper.selectList(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getItemId, itemId)
                .orderByDesc(Bid::getCreatedAt)
                .last("LIMIT " + limit)
        );
    }

    public Long countByItemId(Long itemId) {
        return (long) bidMapper.countByItemId(itemId);
    }

    public IPage<Bid> findByItemIdPage(Long itemId, int pageNum, int pageSize) {
        Page<Bid> page = new Page<>(pageNum, pageSize);
        return bidMapper.selectPage(page,
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getItemId, itemId)
                .orderByDesc(Bid::getCreatedAt)
        );
    }
}
