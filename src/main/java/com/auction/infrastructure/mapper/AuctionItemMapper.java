package com.auction.infrastructure.mapper;

import com.auction.domain.entity.AuctionItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuctionItemMapper extends BaseMapper<AuctionItem> {

    @Select("SELECT ai.*, p.name as product_name, p.image_url as product_image_url, p.description, " +
            "ar.title as room_title FROM auction_items ai " +
            "LEFT JOIN products p ON ai.product_id = p.id " +
            "LEFT JOIN auction_rooms ar ON ai.room_id = ar.id " +
            "WHERE ai.id = #{itemId}")
    AuctionItem getDetailById(@Param("itemId") Long itemId);

    @Select("SELECT ai.*, p.name as product_name, p.image_url as product_image_url " +
            "FROM auction_items ai " +
            "LEFT JOIN products p ON ai.product_id = p.id " +
            "WHERE ai.room_id = #{roomId} AND ai.status = #{status} " +
            "ORDER BY ai.display_order ASC, ai.id ASC")
    List<AuctionItem> getItemsByRoomAndStatus(@Param("roomId") Long roomId, @Param("status") String status);

    @Select("SELECT ai.*, p.name as product_name, p.image_url as product_image_url " +
            "FROM auction_items ai " +
            "LEFT JOIN products p ON ai.product_id = p.id " +
            "WHERE ai.status = 'ACTIVE' ORDER BY ai.display_order ASC, ai.id ASC")
    List<AuctionItem> getActiveItems();

    @Select("SELECT ai.*, p.name as product_name, p.image_url as product_image_url " +
            "FROM auction_items ai " +
            "LEFT JOIN products p ON ai.product_id = p.id " +
            "ORDER BY ai.display_order ASC, ai.id ASC")
    List<AuctionItem> getAllItems();
}
