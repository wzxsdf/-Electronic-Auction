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

@RestController
@RequestMapping("/auction-rooms")
@RequiredArgsConstructor
public class AuctionRoomController {

    private final AuctionRoomRepository auctionRoomRepository;
    private final AuctionItemRepository auctionItemRepository;

    /**
     * 根据ID查询拍卖房间及其包含的拍品列表
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
     * 获取当前活跃的拍卖房间及其拍品列表
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
