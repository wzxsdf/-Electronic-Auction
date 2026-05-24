package com.auction.api.controller;

import com.auction.annotation.RateLimit;
import com.auction.api.dto.request.PlaceBidRequest;
import com.auction.api.dto.response.BidResultResponse;
import com.auction.common.Result;
import com.auction.service.bid.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    /**
     * 出价接口
     * 限流：每分钟最多 30 次请求
     */
    @PostMapping
    @RateLimit(key = "bid", time = 60, count = 30, message = "出价过于频繁，请稍后再试")
    public Result<BidResultResponse> placeBid(@Valid @RequestBody PlaceBidRequest request) {
        return Result.ok(bidService.placeBid(request));
    }

    /**
     * 查询指定竞拍的出价历史记录
     */
    @GetMapping("/auction/{auctionId}")
    public Result<List> getBidHistory(@PathVariable Long auctionId) {
        // TODO: 返回出价记录
        return Result.ok(List.of());
    }
}
