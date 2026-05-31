package com.auction.service.settlement;

import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Auction;
import com.auction.domain.entity.Bid;
import com.auction.domain.entity.Order;
import com.auction.domain.enums.AuctionStatus;
import com.auction.domain.enums.OrderStatus;
import com.auction.repository.AuctionRepository;
import com.auction.repository.BidRepository;
import com.auction.repository.OrderRepository;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞拍结算服务
 * 负责处理竞拍结束后的结算逻辑，包括判定成交者、生成订单等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSettlementService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OrderRepository orderRepository;
    private final WsMessageService wsMessageService;

    /**
     * 结算指定的竞拍
     * 1. 验证竞拍状态
     * 2. 查找最高出价者作为成交者
     * 3. 生成订单
     * 4. 更新竞拍状态为已完成
     * 5. 发送WebSocket通知
     *
     * @param auctionId 竞拍ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void settleAuction(Long auctionId) {
        log.info("开始结算竞拍: auctionId={}", auctionId);

        // 1. 获取竞拍信息
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            log.warn("竞拍不存在，跳过结算: auctionId={}", auctionId);
            return;
        }

        // 2. 验证竞拍状态
        if (auction.getStatusEnum() != AuctionStatus.ACTIVE) {
            log.info("竞拍非活跃状态，跳过结算: auctionId={}, status={}",
                auctionId, auction.getStatus());
            return;
        }

        // 3. 查找最高出价者
        Bid highestBid = findHighestBid(auctionId);
        if (highestBid == null) {
            log.info("竞拍无有效出价，流拍: auctionId={}", auctionId);
            handleNoBidAuction(auction);
            return;
        }

        // 4. 检查是否达到封顶价
        if (auction.getMaxPrice() != null &&
            highestBid.getAmount().compareTo(auction.getMaxPrice()) >= 0) {
            log.info("竞拍达到封顶价，提前成交: auctionId={}, maxPrice={}",
                auctionId, auction.getMaxPrice());
        }

        // 5. 生成订单
        Order order = createOrder(auction, highestBid);
        order = orderRepository.save(order);
        log.info("生成订单成功: orderId={}, auctionId={}, userId={}, amount={}",
            order.getId(), auctionId, highestBid.getUserId(), highestBid.getAmount());

        // 6. 更新竞拍状态
        auction.setStatusEnum(AuctionStatus.COMPLETED);
        auction.setFinalPrice(highestBid.getAmount());
        auction.setWinnerId(highestBid.getUserId());
        auction.setSettledAt(LocalDateTime.now());
        auctionRepository.updateById(auction);

        // 7. 发送成交通知
        wsMessageService.sendYouWon(highestBid.getUserId(), auctionId, highestBid.getAmount());
        wsMessageService.sendAuctionEnded(auctionId, highestBid.getUserId(),
            highestBid.getAmount(), true);

        // 8. 发送未成交通知给其他参与者
        notifyOtherParticipants(auctionId, highestBid.getUserId());

        log.info("竞拍结算完成: auctionId={}, winnerId={}, finalAmount={}",
            auctionId, highestBid.getUserId(), highestBid.getAmount());
    }

    /**
     * 查找竞拍的最高出价
     */
    private Bid findHighestBid(Long auctionId) {
        List<Bid> bids = bidRepository.findRecentByItemId(auctionId, 100);
        if (bids == null || bids.isEmpty()) {
            return null;
        }

        // 返回金额最高的出价
        return bids.stream()
            .filter(b -> "ACTIVE".equals(b.getStatus()))
            .max((b1, b2) -> b1.getAmount().compareTo(b2.getAmount()))
            .orElse(null);
    }

    /**
     * 创建订单
     */
    private Order createOrder(Auction auction, Bid highestBid) {
        Order order = new Order();
        order.setRoomId(auction.getRoomId());
        order.setItemId(auction.getId());
        order.setUserId(highestBid.getUserId());
        order.setProductId(auction.getProductId());
        order.setFinalAmount(highestBid.getAmount());
        order.setStatusEnum(OrderStatus.PENDING_PAYMENT);
        return order;
    }

    /**
     * 处理流拍（无人出价的情况）
     */
    private void handleNoBidAuction(Auction auction) {
        // 更新竞拍状态为已完成
        auction.setStatusEnum(AuctionStatus.COMPLETED);
        auction.setSettledAt(LocalDateTime.now());
        auctionRepository.updateById(auction);

        // 发送流拍通知
        wsMessageService.sendAuctionEnded(auction.getId(), null,
            auction.getCurrentPrice(), false);

        log.info("竞拍流拍处理完成: auctionId={}", auction.getId());
    }

    /**
     * 通知其他参与者未成交
     */
    private void notifyOtherParticipants(Long auctionId, Long winnerId) {
        List<Bid> allBids = bidRepository.findRecentByItemId(auctionId, 1000);
        if (allBids == null || allBids.isEmpty()) {
            return;
        }

        // 获取所有参与用户ID（去重）
        allBids.stream()
            .map(Bid::getUserId)
            .filter(userId -> !userId.equals(winnerId))
            .distinct()
            .forEach(userId -> wsMessageService.sendYouLost(userId, auctionId));
    }

    /**
     * 批量结算到期竞拍
     * 定时任务调用此方法
     */
    @Transactional(rollbackFor = Exception.class)
    public int settleExpiredAuctions() {
        log.info("开始批量结算到期竞拍");

        LocalDateTime now = LocalDateTime.now();
        List<Auction> expiredAuctions = auctionRepository.findExpiredActiveAuctions(now);

        if (expiredAuctions == null || expiredAuctions.isEmpty()) {
            log.info("无到期竞拍需要结算");
            return 0;
        }

        int settledCount = 0;
        for (Auction auction : expiredAuctions) {
            try {
                settleAuction(auction.getId());
                settledCount++;
            } catch (Exception e) {
                log.error("结算竞拍失败: auctionId={}", auction.getId(), e);
            }
        }

        log.info("批量结算完成: 总数={}, 成功={}", expiredAuctions.size(), settledCount);
        return settledCount;
    }
}