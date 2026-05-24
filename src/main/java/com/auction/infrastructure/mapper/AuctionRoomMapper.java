package com.auction.infrastructure.mapper;

import com.auction.domain.entity.AuctionRoom;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuctionRoomMapper extends BaseMapper<AuctionRoom> {

    @Select("SELECT * FROM auction_rooms WHERE status = 'LIVE' LIMIT 1")
    AuctionRoom getActiveRoom();

    @Select("SELECT * FROM auction_rooms WHERE status = 'PENDING' ORDER BY start_time ASC")
    List<AuctionRoom> getPendingRooms();

    @Select("SELECT * FROM auction_rooms WHERE start_time <= NOW() AND end_time >= NOW()")
    List<AuctionRoom> getActiveRoomsInRange();
}
