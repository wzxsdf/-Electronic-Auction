package com.auction.repository;

import com.auction.domain.entity.AuctionItem;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.mapper.AuctionItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuctionItemRepository {

    private final AuctionItemMapper auctionItemMapper;

    public AuctionItem save(AuctionItem item) {
        if (item.getId() == null) {
            auctionItemMapper.insert(item);
        } else {
            auctionItemMapper.updateById(item);
        }
        return item;
    }

    public AuctionItem findById(Long id) {
        return auctionItemMapper.getDetailById(id);
    }

    public List<AuctionItem> findAll() {
        return auctionItemMapper.getAllItems();
    }

    public List<AuctionItem> findByStatus(AuctionStatus status) {
        return auctionItemMapper.selectList(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getStatus, status.name())
                .orderByAsc(AuctionItem::getDisplayOrder)
                .orderByAsc(AuctionItem::getId)
        );
    }

    public List<AuctionItem> findActiveItems() {
        return auctionItemMapper.getActiveItems();
    }

    public List<AuctionItem> findByRoomId(Long roomId) {
        return auctionItemMapper.selectList(
            new LambdaQueryWrapper<AuctionItem>()
                .eq(AuctionItem::getRoomId, roomId)
                .orderByAsc(AuctionItem::getDisplayOrder)
                .orderByAsc(AuctionItem::getId)
        );
    }

    public List<AuctionItem> findByRoomIdAndStatus(Long roomId, AuctionStatus status) {
        return auctionItemMapper.getItemsByRoomAndStatus(roomId, status.name());
    }

    /**
     * 更新拍卖品
     * @return 影响的行数（用于乐观锁检测）
     */
    public int updateById(AuctionItem item) {
        return auctionItemMapper.updateById(item);
    }

    public void deleteById(Long id) {
        auctionItemMapper.deleteById(id);
    }
}
