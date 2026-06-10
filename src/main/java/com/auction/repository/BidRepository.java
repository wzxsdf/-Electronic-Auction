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

    public List<Bid> findByItemId(Long auctionItemId) {
        return bidMapper.getBidsByItemId(auctionItemId);
    }

    public List<Bid> findByRoomId(Long auctionId) {
        return bidMapper.getBidsByRoomId(auctionId);
    }

    public List<Bid> findByItemIdAndUserId(Long itemId, Long userId) {
        return bidMapper.selectList(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionItemId, itemId)
                .eq(Bid::getUserId, userId)
                .orderByDesc(Bid::getCreatedAt)
        );
    }

    public Bid findHighestByItemId(Long itemId) {
        return bidMapper.selectOne(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionItemId, itemId)
                .eq(Bid::getStatus, "ACTIVE")
                .orderByDesc(Bid::getAmount)
                .last("LIMIT 1")
        );
    }

    public List<Bid> findRecentByItemId(Long itemId, int limit) {
        return bidMapper.selectList(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionItemId, itemId)
                .orderByDesc(Bid::getCreatedAt)
                .last("LIMIT " + limit)
        );
    }

    public Long countByItemId(Long auctionItemId) {
        return (long) bidMapper.countByItemId(auctionItemId);
    }

    public IPage<Bid> findByItemIdPage(Long itemId, int pageNum, int pageSize) {
        Page<Bid> page = new Page<>(pageNum, pageSize);
        return bidMapper.selectPage(page,
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionItemId, itemId)
                .orderByDesc(Bid::getCreatedAt)
        );
    }

    /**
     * 查询用户参与过的拍品ID列表（去重）
     *
     * @param userId 用户ID
     * @return 参与过的拍品ID列表
     */
    public java.util.List<Long> findParticipatedItemIds(Long userId) {
        return bidMapper.selectList(
            new LambdaQueryWrapper<Bid>()
                .select(Bid::getAuctionItemId)
                .eq(Bid::getUserId, userId)
                .eq(Bid::getStatus, "ACTIVE")
                .groupBy(Bid::getAuctionItemId)
        ).stream()
         .map(Bid::getAuctionItemId)
         .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 统计用户在指定拍品的出价次数
     *
     * @param auctionItemId 拍品ID
     * @param userId 用户ID
     * @return 出价次数
     */
    public Long countBidByItemIdAndUserId(Long auctionItemId, Long userId) {
        return bidMapper.selectCount(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionItemId, auctionItemId)
                .eq(Bid::getUserId, userId)
                .eq(Bid::getStatus, "ACTIVE")
        );
    }

    /**
     * 查询用户在指定拍品的最高出价
     *
     * @param auctionItemId 拍品ID
     * @param userId 用户ID
     * @return 最高出价记录，如果没有出价则返回null
     */
    public Bid findHighestBidByItemIdAndUserId(Long auctionItemId, Long userId) {
        return bidMapper.selectOne(
            new LambdaQueryWrapper<Bid>()
                .eq(Bid::getAuctionItemId, auctionItemId)
                .eq(Bid::getUserId, userId)
                .eq(Bid::getStatus, "ACTIVE")
                .orderByDesc(Bid::getAmount)
                .last("LIMIT 1")
        );
    }

    /**
     * 获取用户在指定拍品的最高出价金额
     *
     * @param auctionItemId 拍品ID
     * @param userId 用户ID
     * @return 最高出价金额，如果没有出价则返回null
     */
    public java.math.BigDecimal getUserMaxBidAmount(Long auctionItemId, Long userId) {
        Bid highestBid = findHighestBidByItemIdAndUserId(auctionItemId, userId);
        return highestBid != null ? highestBid.getAmount() : null;
    }

    /**
     * 统计出价高于指定金额的不同用户数量
     *
     * @param auctionItemId 拍品ID
     * @param amount 比较金额
     * @return 用户数量
     */
    public Long countUsersWithHigherBid(Long auctionItemId, java.math.BigDecimal amount) {
        return bidMapper.countUsersWithHigherBid(auctionItemId, amount);
    }
}
