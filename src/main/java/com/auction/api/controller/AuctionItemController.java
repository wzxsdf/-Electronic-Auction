package com.auction.api.controller;

import com.auction.common.Result;
import com.auction.domain.entity.AuctionItem;
import com.auction.domain.enums.AuctionStatus;
import com.auction.infrastructure.lock.DistributedLockService;
import com.auction.repository.AuctionItemRepository;
import com.auction.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/auction-items")
@RequiredArgsConstructor
public class AuctionItemController {

    private final AuctionItemRepository auctionItemRepository;
    private final ProductRepository productRepository;
    private final DistributedLockService distributedLockService;

    /**
     * 根据ID查询拍品详情
     */
    @GetMapping("/{id}")
    public Result<AuctionItem> getById(@PathVariable Long id) {
        AuctionItem item = auctionItemRepository.findById(id);
        if (item == null) {
            return Result.fail(404, "Item not found");
        }
        return Result.ok(item);
    }

    /**
     * 查询所有拍品
     */
    @GetMapping
    public Result<List<AuctionItem>> listAll() {
        return Result.ok(auctionItemRepository.findAll());
    }

    /**
     * 查询所有活跃状态的拍品
     */
    @GetMapping("/active")
    public Result<List<AuctionItem>> listActive() {
        return Result.ok(auctionItemRepository.findByStatus(AuctionStatus.ACTIVE));
    }

    /**
     * 根据房间ID查询该房间的所有拍品
     */
    @GetMapping("/room/{roomId}")
    public Result<List<AuctionItem>> getByRoom(@PathVariable Long roomId) {
        return Result.ok(auctionItemRepository.findByRoomId(roomId));
    }

    /**
     * 开始拍品竞拍
     */
    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        AuctionItem item = auctionItemRepository.findById(id);
        if (item == null) {
            return Result.fail(404, "Item not found");
        }

        item.setStatusEnum(AuctionStatus.ACTIVE);
        auctionItemRepository.updateById(item);

        return Result.ok();
    }

    /**
     * 对指定拍品出价，使用分布式锁和乐观锁防止并发问题
     */
    @PostMapping("/{id}/bid")
    public Result<AuctionItem> placeBid(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam BigDecimal amount
    ) {
        // 使用分布式锁保护出价操作，防止并发问题
        String lockKey = DistributedLockService.auctionLockKey(id);

        return distributedLockService.executeWithLock(lockKey, () -> doPlaceBid(id, userId, amount));
    }

    private Result<AuctionItem> doPlaceBid(Long id, Long userId, BigDecimal amount) {
        AuctionItem item = auctionItemRepository.findById(id);
        if (item == null) {
            return Result.fail(404, "Item not found");
        }

        BigDecimal minPrice = item.getStartPrice().add(item.getBidIncrement());
        if (amount.compareTo(minPrice) < 0) {
            return Result.fail(400, "Bid amount too low");
        }

        item.setCurrentPrice(amount);
        item.setHighestBidder(userId);
        int currentCount = item.getBidCount() != null ? item.getBidCount() : 0;
        item.setBidCount(currentCount + 1);

        // 乐观锁：如果版本号不匹配，updateById 会抛出 OptimisticLockerException
        int rows = auctionItemRepository.updateById(item);
        if (rows == 0) {
            // 并发修改冲突，提示用户重试
            return Result.fail(409, "出价冲突，请重试");
        }

        return Result.ok(item);
    }
}
