package com.auction.api.controller;

import com.auction.common.Result;
import com.auction.domain.entity.AuctionRoom;
import com.auction.domain.entity.AuctionItem;
import com.auction.repository.AuctionRoomRepository;
import com.auction.repository.AuctionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拍卖房间控制器：管理拍卖房间和房间内的拍品，支持房间查询和状态管理
 */
@RestController
@RequestMapping("/auction-rooms")
@RequiredArgsConstructor
public class AuctionRoomController {

    private final AuctionRoomRepository auctionRoomRepository;
    private final AuctionItemRepository auctionItemRepository;

    /**
     * 查询房间详情：获取拍卖房间信息和该房间内的所有拍品列表
     */
    @GetMapping("/{id}")
    public Result<Object> getRoomById(@PathVariable Long id) {
        AuctionRoom roomEntity = auctionRoomRepository.findById(id);
        if (roomEntity == null) {
            return Result.fail(404, "Room not found");
        }

        List<AuctionItem> itemsList = auctionItemRepository.findByRoomId(id);

        return Result.ok(new Object() {
            public final AuctionRoom room = roomEntity;
            public final List<AuctionItem> items = itemsList;
        });
    }

    /**
     * 查询活跃房间：获取当前正在进行中的拍卖房间及其包含的拍品
     */
    @GetMapping("/active")
    public Result<Object> getActiveRoom() {
        AuctionRoom roomEntity = auctionRoomRepository.getActiveRoom();
        if (roomEntity == null) {
            return Result.fail(404, "No active room");
        }

        List<AuctionItem> itemsList = auctionItemRepository.findByRoomId(roomEntity.getId());

        return Result.ok(new Object() {
            public final AuctionRoom room = roomEntity;
            public final List<AuctionItem> items = itemsList;
        });
    }
}
