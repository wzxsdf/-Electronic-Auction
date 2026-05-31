package com.auction.service.payment;

import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Order;
import com.auction.domain.enums.OrderStatus;
import com.auction.repository.OrderRepository;
import com.auction.service.order.OrderService;
import com.auction.service.websocket.WsMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 模拟支付服务
 * 提供模拟支付功能，用于测试和演示
 * 支持模拟支付延迟、随机失败等情况
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final WsMessageService wsMessageService;
    private final Random random = new Random();

    /**
     * 模拟支付订单
     * 异步处理，模拟真实支付场景的延迟
     *
     * @param orderId 订单ID
     * @param userId 支付用户ID
     * @return 支付结果
     */
    @Async("paymentTaskExecutor")
    public CompletableFuture<PaymentResult> processPayment(Long orderId, Long userId) {
        log.info("开始处理支付: orderId={}, userId={}", orderId, userId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 验证订单
                Order order = validateOrderForPayment(orderId, userId);

                // 2. 模拟支付处理延迟（1-3秒）
                simulatePaymentDelay();

                // 3. 模拟支付处理（随机失败，10%失败率用于测试）
                boolean success = simulatePaymentProcessing(order);

                if (success) {
                    // 4. 支付成功，更新订单状态
                    completePayment(order);
                    log.info("支付处理成功: orderId={}, userId={}", orderId, userId);
                } else {
                    // 5. 支付失败
                    log.warn("支付处理失败: orderId={}, userId={}", orderId, userId);
                    throw new BizException(ErrorCode.PAYMENT_FAILED, "支付处理失败，请重试");
                }

                return PaymentResult.success(orderId, order.getFinalAmount());

            } catch (BizException e) {
                log.error("支付处理失败: orderId={}, error={}", orderId, e.getMessage());
                return PaymentResult.failure(orderId, e.getMessage());
            } catch (Exception e) {
                log.error("支付处理异常: orderId={}", orderId, e);
                return PaymentResult.failure(orderId, "支付处理异常");
            }
        });
    }

    /**
     * 同步支付接口（用于简单场景）
     *
     * @param orderId 订单ID
     * @param userId 支付用户ID
     * @return 支付结果
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentResult payOrderSync(Long orderId, Long userId) {
        log.info("同步支付开始: orderId={}, userId={}", orderId, userId);

        try {
            // 1. 验证订单
            Order order = validateOrderForPayment(orderId, userId);

            // 2. 直接支付处理（90%成功率）
            boolean success = random.nextDouble() > 0.1;

            if (success) {
                // 3. 支付成功
                completePayment(order);
                log.info("同步支付成功: orderId={}, amount={}", orderId, order.getFinalAmount());
            } else {
                // 4. 支付失败
                log.warn("同步支付失败: orderId={}", orderId);
                throw new BizException(ErrorCode.PAYMENT_FAILED, "支付失败，请重试");
            }

            return PaymentResult.success(orderId, order.getFinalAmount());

        } catch (BizException e) {
            log.error("同步支付失败: orderId={}, error={}", orderId, e.getMessage());
            return PaymentResult.failure(orderId, e.getMessage());
        }
    }

    /**
     * 查询支付结果
     *
     * @param orderId 订单ID
     * @return 支付状态
     */
    public PaymentStatus getPaymentStatus(Long orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        return switch (order.getStatusEnum()) {
            case PENDING_PAYMENT -> PaymentStatus.PENDING;
            case PAID, COMPLETED -> PaymentStatus.SUCCESS;
            case CANCELLED -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.UNKNOWN;
        };
    }

    /**
     * 验证订单是否可以支付
     */
    private Order validateOrderForPayment(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 验证订单所属用户
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作此订单");
        }

        // 验证订单状态
        if (order.getStatusEnum() != OrderStatus.PENDING_PAYMENT) {
            if (order.getStatusEnum() == OrderStatus.PAID) {
                throw new BizException(ErrorCode.ORDER_ALREADY_PAID);
            } else if (order.getStatusEnum() == OrderStatus.CANCELLED) {
                throw new BizException(ErrorCode.ORDER_ALREADY_CANCELLED);
            } else {
                throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不允许支付");
            }
        }

        return order;
    }

    /**
     * 模拟支付处理延迟（1-3秒）
     */
    private void simulatePaymentDelay() {
        try {
            int delayMs = 1000 + random.nextInt(2000); // 1-3秒随机延迟
            TimeUnit.MILLISECONDS.sleep(delayMs);
            log.debug("模拟支付延迟: {}ms", delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("支付延迟被中断");
        }
    }

    /**
     * 模拟支付处理逻辑
     *
     * @param order 订单信息
     * @return 是否成功
     */
    private boolean simulatePaymentProcessing(Order order) {
        // 90%成功率，10%失败率用于测试异常场景
        boolean success = random.nextDouble() > 0.1;

        if (success) {
            log.info("支付处理模拟成功: orderId={}, amount={}",
                order.getId(), order.getFinalAmount());
        } else {
            log.warn("支付处理模拟失败: orderId={}, amount={}",
                order.getId(), order.getFinalAmount());
        }

        return success;
    }

    /**
     * 完成支付，更新订单状态并发送通知
     */
    private void completePayment(Order order) {
        // 1. 更新订单状态为已支付
        order.setStatusEnum(OrderStatus.PAID);
        orderRepository.save(order);

        // 2. 发送WebSocket通知
        wsMessageService.sendPaymentSuccess(order.getUserId(), order.getId(),
            order.getFinalAmount());

        // 3. 记录支付日志
        log.info("订单支付完成: orderId={}, userId={}, amount={}, paidTime={}",
            order.getId(), order.getUserId(), order.getFinalAmount(), LocalDateTime.now());
    }

    /**
     * 取消支付（关闭订单）
     *
     * @param orderId 订单ID
     * @param userId 操作用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelPayment(Long orderId, Long userId) {
        log.info("取消支付: orderId={}, userId={}", orderId, userId);

        // 验证并取消订单
        orderService.cancelOrder(orderId, "用户取消支付");

        // 发送取消通知
        wsMessageService.sendPaymentCancelled(userId, orderId);
    }

    /**
     * 支付结果
     */
    public record PaymentResult(
        boolean success,
        String orderId,
        BigDecimal amount,
        String message,
        LocalDateTime timestamp
    ) {
        public static PaymentResult success(Long orderId, BigDecimal amount) {
            return new PaymentResult(true, orderId.toString(), amount,
                "支付成功", LocalDateTime.now());
        }

        public static PaymentResult failure(Long orderId, String errorMessage) {
            return new PaymentResult(false, orderId.toString(), null,
                errorMessage, LocalDateTime.now());
        }
    }

    /**
     * 支付状态
     */
    public enum PaymentStatus {
        PENDING("待支付"),
        SUCCESS("支付成功"),
        CANCELLED("已取消"),
        UNKNOWN("未知状态");

        private final String description;

        PaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}