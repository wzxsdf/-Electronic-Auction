package com.auction.service.messaging;

import com.auction.config.RabbitMQConfig;
import com.auction.domain.entity.AuctionItem;
import com.auction.domain.entity.Bid;
import com.auction.domain.entity.Order;
import com.auction.domain.event.BidEvent;
import com.auction.repository.AuctionItemRepository;
import com.auction.repository.BidRepository;
import com.auction.service.order.OrderService;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 出价事件消费者（MQ 消费者）
 * <p>
 * 负责异步处理出价事件：
 * 1. 持久化出价记录到数据库
 * 2. 更新拍品状态快照
 * 3. 推送 WebSocket 实时消息
 * 4. 检查并触发订单生成
 * <p>
 * 容错机制：
 * - 自动重试（最多3次）
 * - 死信队列处理
 * - 幂等性保证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidEventConsumer {

    private final BidRepository bidRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final OrderService orderService;
    private final WsMessageService wsMessageService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    /**
     * 消费出价事件（批量处理）
     * <p>
     * 并发度：10 个消费者同时处理
     * 重试机制：失败自动重试 3 次
     */
    @RabbitListener(
            queues = RabbitMQConfig.BID_EVENT_QUEUE,
            concurrency = "10",
            autoStartup = "true"
    )
    @Transactional(rollbackFor = Exception.class)
    public void handleBidEvent(
            BidEvent event,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        try {
            log.info("消费出价事件: eventId={}, itemId={}, userId={}, amount={}",
                    event.getEventId(), event.getAuctionItemId(), event.getUserId(), event.getAmount());

            // 1. 幂等性检查（防止重复消费）
            if (isEventProcessed(event.getEventId())) {
                log.warn("事件已处理，跳过: eventId={}", event.getEventId());
                return;
            }

            // 2. 持久化出价记录到数据库
            persistBidRecord(event);

            // 3. 更新拍品状态快照（数据库兜底）
            updateItemSnapshot(event);

            // 4. 推送 WebSocket 实时消息
            broadcastBidUpdate(event);

            // 5. 检查是否需要触发订单生成（拍品结束时）
            checkAndCreateOrder(event);

            // 6. 标记事件已处理
            markEventProcessed(event.getEventId());

            log.info("出价事件处理完成: eventId={}, itemId={}, userId={}",
                    event.getEventId(), event.getAuctionItemId(), event.getUserId());

        } catch (Exception e) {
            log.error("处理出价事件失败: eventId={}, error={}", event.getEventId(), e.getMessage(), e);
            throw e;  // 抛出异常触发重试
        }
    }

    /**
     * 幂等性检查：防止重复消费
     */
    private boolean isEventProcessed(String eventId) {
        // 使用 Redis 存储已处理的事件ID（24小时过期）
        String key = "auction:bid:processed:" + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 持久化出价记录到数据库
     */
    private void persistBidRecord(BidEvent event) {
        Bid bid = new Bid();
        bid.setAuctionItemId(event.getAuctionItemId());
        bid.setAuctionId(event.getAuctionId());
        bid.setUserId(event.getUserId());
        bid.setAmount(event.getAmount());
        bid.setIsAutoBid(event.getIsAutoBid());
        bid.setStatus("ACTIVE");
        bid.setCreatedAt(event.getBidTime());

        bidRepository.save(bid);

        log.debug("出价记录已持久化: bidId={}, eventId={}", bid.getId(), event.getEventId());
    }

    /**
     * 更新拍品状态快照（数据库兜底）
     */
    private void updateItemSnapshot(BidEvent event) {
        AuctionItem item = auctionItemRepository.findById(event.getAuctionItemId());
        if (item == null) {
            log.warn("拍品不存在，跳过更新: itemId={}", event.getAuctionItemId());
            return;
        }

        // 使用乐观锁更新（避免并发冲突）
        int updated = auctionItemRepository.updateItemSnapshot(
                event.getAuctionItemId(),
                event.getCurrentPrice(),
                event.getHighestBidder(),
                event.getBidCount(),
                event.getEndTime(),
                event.getDelayCount(),
                item.getVersion()  // 乐观锁版本号
        );

        if (updated == 0) {
            log.warn("拍品状态更新失败（版本冲突）: itemId={}, version={}",
                    event.getAuctionItemId(), item.getVersion());
            // 版本冲突不影响出价结果，Redis 数据已是最新
        } else {
            log.debug("拍品状态已更新: itemId={}, newPrice={}, endTime={}",
                    event.getAuctionItemId(), event.getCurrentPrice(), event.getEndTime());
        }
    }

    /**
     * 推送 WebSocket 实时消息
     */
    private void broadcastBidUpdate(BidEvent event) {
        try {
            // 构建出价记录（用于广播）
            Bid bid = new Bid();
            bid.setId(null);  // bidId 尚未生成
            bid.setAuctionItemId(event.getAuctionItemId());
            bid.setUserId(event.getUserId());
            bid.setAmount(event.getAmount());
            bid.setIsAutoBid(event.getIsAutoBid());
            bid.setCreatedAt(event.getBidTime());

            // 构建拍品信息
            AuctionItem item = new AuctionItem();
            item.setId(event.getAuctionItemId());
            item.setAuctionId(event.getAuctionId());
            item.setCurrentPrice(event.getCurrentPrice());
            item.setHighestBidder(event.getHighestBidder());
            item.setEndTime(event.getEndTime());

            // 广播新出价消息
            wsMessageService.broadcastNewBid(item, bid, event.getUsername(), 1);

            // 广播价格更新
            wsMessageService.broadcastItemPriceUpdate(event.getAuctionItemId());

            // 如果触发了延时，广播延时通知
            if (event.getDelayCount() != null && event.getDelayCount() > 0) {
                wsMessageService.sendAuctionItemDelay(
                        event.getAuctionItemId(),
                        event.getEndTime(),
                        event.getDelayCount()
                );
            }

            // 如果达到封顶价，广播封顶价成交通知
            if (event.getMaxPriceReached() != null && event.getMaxPriceReached()) {
                wsMessageService.sendMaxPriceReached(
                        event.getAuctionItemId(),
                        event.getCurrentPrice(),
                        event.getUserId(),
                        event.getUsername()
                );
            }

            log.debug("WebSocket 消息已推送: itemId={}, userId={}",
                    event.getAuctionItemId(), event.getUserId());

        } catch (Exception e) {
            log.error("推送 WebSocket 消息失败: itemId={}, error={}",
                    event.getAuctionItemId(), e.getMessage(), e);
            // WebSocket 推送失败不影响主流程
        }
    }

    /**
     * 检查并创建订单（拍品结束时或封顶价成交时）
     */
    private void checkAndCreateOrder(BidEvent event) {
        // 如果达到封顶价自动成交，或结束时间已到，且有人出价，则创建订单
        boolean shouldCreateOrder = false;

        // 检查封顶价成交
        if (event.getMaxPriceReached() != null && event.getMaxPriceReached()) {
            shouldCreateOrder = true;
            log.info("检测到封顶价成交，准备创建订单: itemId={}, userId={}, amount={}",
                    event.getAuctionItemId(), event.getHighestBidder(), event.getCurrentPrice());
        }
        // 检查正常结束
        else if (event.getEndTime() != null && event.getEndTime().isBefore(java.time.LocalDateTime.now())) {
            shouldCreateOrder = true;
        }

        if (shouldCreateOrder && event.getHighestBidder() != null && event.getCurrentPrice() != null) {
            try {
                createOrderForAuctionItem(event);
            } catch (Exception e) {
                log.error("创建订单失败: itemId={}, error={}",
                        event.getAuctionItemId(), e.getMessage(), e);
                // 订单创建失败单独处理，不影响出价
            }
        }
    }

    /**
     * 为成交的拍品创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    protected void createOrderForAuctionItem(BidEvent event) {
        Order order = new Order();
        order.setAuctionId(event.getAuctionId());
        order.setAuctionItemId(event.getAuctionItemId());
        order.setUserId(event.getHighestBidder());
        order.setFinalAmount(event.getCurrentPrice());
        order.setPayableAmount(event.getCurrentPrice());

        orderService.createOrder(order);

        // 广播拍品结束消息
        wsMessageService.sendAuctionItemEnded(
                event.getAuctionItemId(),
                event.getHighestBidder(),
                event.getCurrentPrice(),
                event.getUsername(),
                true
        );

        log.info("订单已生成: itemId={}, winnerId={}, amount={}",
                event.getAuctionItemId(), event.getHighestBidder(), event.getCurrentPrice());
    }

    /**
     * 标记事件已处理
     */
    private void markEventProcessed(String eventId) {
        String key = "auction:bid:processed:" + eventId;
        redisTemplate.opsForValue().set(key, "1", 24, java.util.concurrent.TimeUnit.HOURS);
    }
}
