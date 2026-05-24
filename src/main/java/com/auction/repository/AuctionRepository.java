package com.auction.repository;

import com.auction.domain.entity.Auction;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.mapper.AuctionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuctionRepository {

    private final AuctionMapper auctionMapper;

    public Auction save(Auction auction) {
        if (auction.getId() == null) {
            auctionMapper.insert(auction);
        } else {
            auctionMapper.updateById(auction);
        }
        return auction;
    }

    public Auction findById(Long id) {
        return auctionMapper.selectById(id);
    }

    public List<Auction> findAll() {
        return auctionMapper.selectList(null);
    }

    public List<Auction> findByStatus(AuctionStatus status) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, status.name())
                .orderByDesc(Auction::getCreatedAt)
        );
    }

    public List<Auction> findActiveAuctions() {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.ACTIVE.name())
                .orderByAsc(Auction::getEndTime)
        );
    }

    public List<Auction> findPendingAuctions(LocalDateTime before) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.PENDING.name())
                .le(Auction::getStartTime, before)
        );
    }

    public Auction updateById(Auction auction) {
        auctionMapper.updateById(auction);
        return auction;
    }

    public void deleteById(Long id) {
        auctionMapper.deleteById(id);
    }
}
