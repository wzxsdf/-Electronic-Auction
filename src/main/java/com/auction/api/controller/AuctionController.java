package com.auction.api.controller;

import com.auction.api.dto.request.CreateAuctionRequest;
import com.auction.api.dto.response.AuctionResponse;
import com.auction.common.Result;
import com.auction.domain.entity.Auction;
import com.auction.domain.enums.AuctionStatus;
import com.auction.repository.AuctionRepository;
import com.auction.repository.ProductRepository;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final WsMessageService wsMessageService;

    /**
     * 创建竞拍活动：验证商品和时间 → 初始化竞拍参数 → 保存为待开始状态
     */
    @PostMapping
    public Result<Auction> create(@Valid @RequestBody CreateAuctionRequest request) {
        // 验证商品存在
        if (!productRepository.existsById(request.getProductId())) {
            return Result.fail(400, "商品不存在");
        }

        // 验证时间
        if (request.getEndTime().isBefore(request.getStartTime())) {
            return Result.fail(400, "结束时间不能早于开始时间");
        }

        Auction auction = new Auction();
        auction.setProductId(request.getProductId());
        auction.setTitle(request.getTitle());
        auction.setStartPrice(request.getStartPrice());
        auction.setBidIncrement(request.getBidIncrement());
        auction.setMaxPrice(request.getMaxPrice());
        auction.setDelaySeconds(request.getDelaySeconds() != null ? request.getDelaySeconds() : 15);
        auction.setStartTime(request.getStartTime());
        auction.setEndTime(request.getEndTime());
        auction.setOriginalEndTime(request.getEndTime());
        auction.setCurrentPrice(request.getStartPrice());
        auction.setStatusEnum(AuctionStatus.PENDING);

        auction = auctionRepository.save(auction);

        System.out.println("创建竞拍: auctionId=" + auction.getId());
        return Result.ok(auction);
    }

    /**
     * 查询竞拍详情：根据ID获取竞拍信息并加载关联商品数据（名称、图片、描述）
     */
    @GetMapping("/{id}")
    public Result<Auction> getById(@PathVariable Long id) {
        Auction auction = auctionRepository.findById(id);
        if (auction == null) {
            return Result.fail(404, "竞拍不存在");
        }

        // 加载商品信息
        var product = productRepository.findById(auction.getProductId());
        if (product != null) {
            auction.setProductName(product.getName());
            auction.setProductImageUrl(product.getImageUrl());
            auction.setDescription(product.getDescription());
        }

        return Result.ok(auction);
    }

    /**
     * 查询所有竞拍活动：返回系统中全部竞拍记录，包含各种状态
     */
    @GetMapping
    public Result<List<Auction>> listAll() {
        return Result.ok(auctionRepository.findAll());
    }

    /**
     * 查询活跃竞拍：返回正在进行中、可接受出价的竞拍活动列表
     */
    @GetMapping("/active")
    public Result<List<Auction>> listActive() {
        return Result.ok(auctionRepository.findByStatus(AuctionStatus.ACTIVE));
    }

    /**
     * 开始竞拍：更新状态为活跃 → 通过WebSocket通知所有用户竞拍已开始
     */
    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        Auction auction = auctionRepository.findById(id);
        if (auction == null) {
            return Result.fail(404, "竞拍不存在");
        }

        auction.setStatusEnum(AuctionStatus.ACTIVE);
        auctionRepository.updateById(auction);

        // 发送 WebSocket 通知所有用户
        wsMessageService.sendAuctionStarted(id);
        System.out.println("开始竞拍: auctionId=" + id);
        return Result.ok();
    }

    /**
     * 取消竞拍：更新状态为已取消 → 通过WebSocket通知所有用户并说明原因
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        Auction auction = auctionRepository.findById(id);
        if (auction == null) {
            return Result.fail(404, "竞拍不存在");
        }

        auction.setStatusEnum(AuctionStatus.CANCELLED);
        auctionRepository.updateById(auction);

        // 发送 WebSocket 通知所有用户
        wsMessageService.sendAuctionCancelled(id, reason);
        System.out.println("取消竞拍: auctionId=" + id + ", reason=" + reason);
        return Result.ok();
    }
}
