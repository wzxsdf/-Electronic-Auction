package com.auction.service.notification;

import com.auction.domain.entity.Auction;
import com.auction.domain.entity.User;
import com.auction.repository.AuctionRepository;
import com.auction.repository.UserRepository;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 通知服务
 * 统一管理各类业务通知的发送
 * 确保所有业务操作都有相应的通知机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WsMessageService wsMessageService;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 发送竞拍状态变更通知
     */
    public void notifyAuctionStatusChange(Long auctionId, String newStatus) {
        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            log.warn("竞拍不存在，无法发送状态变更通知: auctionId={}", auctionId);
            return;
        }

        switch (newStatus.toUpperCase()) {
            case "ACTIVE" ->
                wsMessageService.sendAuctionStarted(auctionId);
            case "COMPLETED" ->
                handleAuctionCompletedNotification(auction);
            case "CANCELLED" ->
                wsMessageService.sendAuctionCancelled(auctionId, "竞拍已取消");
            default ->
                log.debug("未知的竞拍状态: {}", newStatus);
        }
    }

    /**
     * 处理竞拍完成通知
     */
    private void handleAuctionCompletedNotification(Auction auction) {
        if (auction.getWinnerId() != null) {
            // 有成交者
            wsMessageService.sendYouWon(auction.getWinnerId(), auction.getId(),
                auction.getFinalPrice() != null ? auction.getFinalPrice() : auction.getCurrentPrice());
        }

        // 广播竞拍结束通知
        wsMessageService.sendAuctionEnded(auction.getId(), auction.getWinnerId(),
            auction.getFinalPrice() != null ? auction.getFinalPrice() : auction.getCurrentPrice(),
            auction.getWinnerId() != null);
    }

    /**
     * 发送出价失败通知
     */
    public void notifyBidFailed(Long userId, Long auctionId, String reason) {
        wsMessageService.sendBidFailed(userId, auctionId, reason);
    }

    /**
     * 发送支付成功通知
     */
    public void notifyPaymentSuccess(Long userId, Long orderId, BigDecimal amount) {
        wsMessageService.sendPaymentSuccess(userId, orderId, amount);
    }

    /**
     * 发送支付取消通知
     */
    public void notifyPaymentCancelled(Long userId, Long orderId) {
        wsMessageService.sendPaymentCancelled(userId, orderId);
    }

    /**
     * 发送被超越通知
     */
    public void notifyOvertaken(Long userId, Long auctionId, BigDecimal currentPrice) {
        wsMessageService.sendYouWereOvertaken(userId, auctionId, currentPrice);
    }

    /**
     * 发送领先通知
     */
    public void notifyLeading(Long userId, Long auctionId, BigDecimal amount) {
        wsMessageService.sendYouAreLeading(userId, auctionId, amount);
    }

    /**
     * 发送未成交通知
     */
    public void notifyLost(Long userId, Long auctionId) {
        wsMessageService.sendYouLost(userId, auctionId);
    }

    /**
     * 批量发送通知给竞拍参与者
     */
    public void notifyAuctionParticipants(Long auctionId, Long excludeUserId) {
        // 获取所有参与的用户ID
        List<Long> participantIds = getParticipantUserIds(auctionId);

        // 排除指定用户
        participantIds.stream()
            .filter(userId -> !userId.equals(excludeUserId))
            .forEach(userId -> notifyLost(userId, auctionId));
    }

    /**
     * 获取竞拍参与者用户ID列表
     */
    private List<Long> getParticipantUserIds(Long auctionId) {
        // TODO: 从Redis或数据库获取参与者列表
        // 这里可以扩展为从缓存中获取参与者集合
        return List.of();
    }

    /**
     * 发送系统公告
     */
    public void sendSystemAnnouncement(String message, List<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            log.info("系统广播: {}", message);
            // 这里可以实现全局广播
        } else {
            targetUserIds.forEach(userId ->
                log.info("发送系统通知给用户 {}: {}", userId, message));
        }
    }
}