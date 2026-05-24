package com.auction.service.room;

import com.auction.domain.entity.AuctionRoom;
import com.auction.infrastructure.mapper.AuctionRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionRoomService {

    private final AuctionRoomMapper auctionRoomMapper;

    public AuctionRoom getById(Long id) {
        return auctionRoomMapper.selectById(id);
    }

    public AuctionRoom getActiveRoom() {
        return auctionRoomMapper.getActiveRoom();
    }

    public List<AuctionRoom> getPendingRooms() {
        return auctionRoomMapper.getPendingRooms();
    }

    public List<AuctionRoom> getActiveRoomsInRange() {
        return auctionRoomMapper.getActiveRoomsInRange();
    }

    public void createRoom(AuctionRoom room) {
        auctionRoomMapper.insert(room);
    }

    public void updateRoom(AuctionRoom room) {
        auctionRoomMapper.updateById(room);
    }

    public void endRoom(Long roomId) {
        AuctionRoom room = new AuctionRoom();
        room.setId(roomId);
        room.setStatus(AuctionRoom.RoomStatus.ENDED.name());
        auctionRoomMapper.updateById(room);
    }
}
