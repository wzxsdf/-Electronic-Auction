package com.auction.infrastructure.mapper;

import com.auction.domain.entity.Auction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuctionMapper extends BaseMapper<Auction> {
}
