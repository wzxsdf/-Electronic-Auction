package com.auction.repository;

import com.auction.domain.entity.AuctionRoom;
import com.auction.infrastructure.mapper.AuctionRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuctionRoomRepository {

    private final AuctionRoomMapper auctionRoomMapper;

    public AuctionRoom findById(Long id) {
        return auctionRoomMapper.selectById(id);
    }

    public AuctionRoom getActiveRoom() {
        return auctionRoomMapper.getActiveRoom();
    }
}
