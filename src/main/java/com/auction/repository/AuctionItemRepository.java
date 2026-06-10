package com.auction.repository;

import com.auction.domain.entity.AuctionItem;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.mapper.AuctionItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 竞拍项Repository（重构后）
 * <p>
 * 提供拍品的CRUD操作和复杂查询
 * 支持按活动ID、商品ID、状态等维度查询
 */
@Repository
@RequiredArgsConstructor
public class AuctionItemRepository {

    private final AuctionItemMapper auctionItemMapper;

    /**
     * 保存拍品
     */
    public AuctionItem save(AuctionItem item) {
        if (item.getId() == null) {
            auctionItemMapper.insert(item);
        } else {
            auctionItemMapper.updateById(item);
        }
        return item;
    }

    /**
     * 根据ID查询拍品
     */
    public AuctionItem findById(Long id) {
        return auctionItemMapper.getDetailById(id);
    }

    /**
     * 查询所有拍品
     */
    public List<AuctionItem> findAll() {
        return auctionItemMapper.getAllItems();
    }

    /**
     * 根据状态查询拍品列表
     */
    public List<AuctionItem> findByStatus(AuctionStatus status) {
        return auctionItemMapper.selectList(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getStatus, status.name())
                .orderByAsc(AuctionItem::getDisplayOrder)
                .orderByAsc(AuctionItem::getId)
        );
    }

    /**
     * 查询活跃拍品
     */
    public List<AuctionItem> findActiveItems() {
        return auctionItemMapper.getActiveItems();
    }

    /**
     * 根据活动ID查询拍品列表（重构：替代findByRoomId）
     */
    public List<AuctionItem> findByAuctionId(Long auctionId) {
        return auctionItemMapper.selectList(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getAuctionId, auctionId)
                .orderByAsc(AuctionItem::getDisplayOrder)
                .orderByAsc(AuctionItem::getId)
        );
    }

    /**
     * 根据活动ID和状态查询拍品列表
     */
    public List<AuctionItem> findByAuctionIdAndStatus(Long auctionId, AuctionStatus status) {
        return auctionItemMapper.selectList(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getAuctionId, auctionId)
                .eq(AuctionItem::getStatus, status.name())
                .orderByAsc(AuctionItem::getDisplayOrder)
        );
    }

    /**
     * 查询活动下的活跃拍品
     */
    public List<AuctionItem> findActiveItemsByAuctionId(Long auctionId) {
        return auctionItemMapper.selectList(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getAuctionId, auctionId)
                .eq(AuctionItem::getStatus, AuctionStatus.ACTIVE.name())
                .orderByAsc(AuctionItem::getDisplayOrder)
        );
    }

    /**
     * 根据商品ID查询拍品
     */
    public List<AuctionItem> findByProductId(Long productId) {
        return auctionItemMapper.selectList(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getProductId, productId)
        );
    }

    /**
     * 更新拍卖品
     * @return 影响的行数（用于乐观锁检测）
     */
    public int updateById(AuctionItem item) {
        return auctionItemMapper.updateById(item);
    }

    /**
     * 删除拍品
     */
    public void deleteById(Long id) {
        auctionItemMapper.deleteById(id);
    }

    /**
     * 检查拍品是否存在
     */
    public boolean existsById(Long id) {
        return auctionItemMapper.selectById(id) != null;
    }

    /**
     * 统计活动下的拍品数量
     */
    public Long countByAuctionId(Long auctionId) {
        return auctionItemMapper.selectCount(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getAuctionId, auctionId)
        );
    }

    /**
     * 查询到期活跃拍品（结束时间已过但状态仍为ACTIVE）
     */
    public List<AuctionItem> findExpiredActiveItems(java.time.LocalDateTime expiredBefore) {
        return auctionItemMapper.selectList(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getStatus, AuctionStatus.ACTIVE.name())
                .lt(AuctionItem::getEndTime, expiredBefore)
        );
    }

    /**
     * 查询待开始的拍品（开始时间已到但状态仍为PENDING）
     */
    public List<AuctionItem> findPendingItemsToStart(java.time.LocalDateTime startAfter) {
        return auctionItemMapper.selectList(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getStatus, AuctionStatus.PENDING.name())
                .le(AuctionItem::getStartTime, startAfter)
                .orderByAsc(AuctionItem::getStartTime)
        );
    }
}
