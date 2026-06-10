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

/**
 * 竞拍活动Repository（重构后）
 * <p>
 * 提供活动的CRUD操作和复杂查询
 * 支持批量查询、统计查询和状态筛选
 */
@Repository
@RequiredArgsConstructor
public class AuctionRepository {

    private final AuctionMapper auctionMapper;

    /**
     * 保存或更新活动
     */
    public Auction save(Auction auction) {
        if (auction.getId() == null) {
            auctionMapper.insert(auction);
        } else {
            auctionMapper.updateById(auction);
        }
        return auction;
    }

    /**
     * 根据ID查询活动
     */
    public Auction findById(Long id) {
        return auctionMapper.selectById(id);
    }

    /**
     * 查询所有活动
     */
    public List<Auction> findAll() {
        return auctionMapper.selectList(null);
    }

    /**
     * 根据ID列表批量查询活动
     */
    public List<Auction> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return auctionMapper.selectBatchIds(ids);
    }

    /**
     * 根据状态查询活动列表
     */
    public List<Auction> findByStatus(AuctionStatus status) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, status.name())
                .orderByDesc(Auction::getCreatedAt)
        );
    }

    /**
     * 查询活跃的活动列表（按结束时间排序）
     */
    public List<Auction> findActiveAuctions() {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.ACTIVE.name())
                .orderByAsc(Auction::getEndTime)
        );
    }

    /**
     * 查询待开始的活动（开始时间在指定时间之前）
     */
    public List<Auction> findPendingAuctions(LocalDateTime before) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.PENDING.name())
                .le(Auction::getStartTime, before)
        );
    }

    /**
     * 根据创建者ID查询活动
     */
    public List<Auction> findByHostId(Long hostId) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getHostId, hostId)
                .orderByDesc(Auction::getCreatedAt)
        );
    }

    /**
     * 根据创建者ID分页查询活动
     */
    public IPage<Auction> findByHostIdPage(Integer page, Integer size, Long hostId) {
        Page<Auction> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Auction> wrapper = new LambdaQueryWrapper<Auction>()
                .eq(Auction::getHostId, hostId)
                .orderByDesc(Auction::getCreatedAt);
        return auctionMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 更新活动
     */
    public Auction updateById(Auction auction) {
        auctionMapper.updateById(auction);
        return auction;
    }

    /**
     * 删除活动
     */
    public void deleteById(Long id) {
        auctionMapper.deleteById(id);
    }

    /**
     * 检查活动是否存在
     */
    public boolean existsById(Long id) {
        return auctionMapper.selectById(id) != null;
    }

    /**
     * 查找已到期但仍为活跃状态的活动
     * 用于定时任务结算
     */
    public List<Auction> findExpiredActiveAuctions(LocalDateTime now) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.ACTIVE.name())
                .le(Auction::getEndTime, now)
        );
    }

    /**
     * 分页查询活动列表
     */
    public IPage<Auction> findByPage(Integer page, Integer size, AuctionStatus status, String keyword) {
        Page<Auction> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Auction> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Auction::getStatus, status.name());
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Auction::getTitle, keyword);
        }

        wrapper.orderByDesc(Auction::getCreatedAt);

        return auctionMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 统计指定用户参与的活动数量
     */
    public Long countByHostId(Long hostId) {
        return auctionMapper.selectCount(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getHostId, hostId)
        );
    }

    /**
     * 统计指定状态的活动数量
     */
    public Long countByStatus(AuctionStatus status) {
        return auctionMapper.selectCount(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, status.name())
        );
    }

    /**
     * 查找需要延期的活动（临近结束）
     * 注意：这个方法用于兼容旧的延时逻辑，实际延时应在AuctionItem级别处理
     */
    public List<Auction> findAuctionsNeedDelay(LocalDateTime now, int delaySeconds) {
        return auctionMapper.selectList(
            new LambdaQueryWrapper<Auction>()
                .eq(Auction::getStatus, AuctionStatus.ACTIVE.name())
                .le(Auction::getEndTime, now.plusSeconds(delaySeconds))
                .ge(Auction::getEndTime, now)
        );
    }
}
