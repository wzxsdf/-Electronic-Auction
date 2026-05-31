package com.auction.service.order;

import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Order;
import com.auction.domain.enums.OrderStatus;
import com.auction.repository.AuctionRepository;
import com.auction.repository.OrderRepository;
import com.auction.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单业务服务
 * 负责订单的创建、查询、状态管理等核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;

    /**
     * 根据订单ID查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情（包含关联的竞拍和商品信息）
     */
    public Order getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 加载关联信息
        enrichOrderInfo(order);
        return order;
    }

    /**
     * 根据用户ID查询订单列表
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    public List<Order> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        // 为每个订单加载关联信息
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    /**
     * 根据用户ID和状态查询订单
     *
     * @param userId 用户ID
     * @param status 订单状态
     * @return 订单列表
     */
    public List<Order> getUserOrdersByStatus(Long userId, OrderStatus status) {
        List<Order> orders = orderRepository.findByUserIdAndStatus(userId, status);
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    /**
     * 根据竞拍ID查询订单
     *
     * @param auctionId 竞拍ID
     * @return 订单信息
     */
    public Order getOrderByAuctionId(Long auctionId) {
        Order order = orderRepository.findByAuctionId(auctionId);
        if (order != null) {
            enrichOrderInfo(order);
        }
        return order;
    }

    /**
     * 更新订单状态
     *
     * @param orderId 订单ID
     * @param newStatus 新状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 验证状态转换是否合法
        validateStatusTransition(order.getStatusEnum(), newStatus);

        order.setStatusEnum(newStatus);
        orderRepository.save(order);

        log.info("订单状态更新成功: orderId={}, oldStatus={}, newStatus={}",
            orderId, order.getStatus(), newStatus);
    }

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @param reason 取消原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 只有待支付状态的订单才能取消
        if (order.getStatusEnum() != OrderStatus.PENDING_PAYMENT) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                "当前订单状态不允许取消");
        }

        order.setStatusEnum(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("订单取消成功: orderId={}, reason={}", orderId, reason);
    }

    /**
     * 创建订单（内部方法，供结算服务调用）
     *
     * @param order 订单实体
     * @return 保存后的订单
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Order order) {
        // 验证必要字段
        validateOrder(order);

        // 设置初始状态
        if (order.getStatusEnum() == null) {
            order.setStatusEnum(OrderStatus.PENDING_PAYMENT);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("订单创建成功: orderId={}, userId={}, amount={}",
            savedOrder.getId(), order.getUserId(), order.getFinalAmount());

        return savedOrder;
    }

    /**
     * 获取订单统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    public OrderStatistics getUserOrderStatistics(Long userId) {
        long totalOrders = orderRepository.countByUserId(userId);
        long pendingPaymentCount = orderRepository.countByUserIdAndStatus(userId, OrderStatus.PENDING_PAYMENT);
        long paidCount = orderRepository.countByUserIdAndStatus(userId, OrderStatus.PAID);
        long cancelledCount = orderRepository.countByUserIdAndStatus(userId, OrderStatus.CANCELLED);

        return new OrderStatistics(totalOrders, pendingPaymentCount, paidCount, cancelledCount);
    }

    /**
     * 验证订单数据
     */
    private void validateOrder(Order order) {
        if (order.getUserId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户ID不能为空");
        }
        if (order.getProductId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品ID不能为空");
        }
        if (order.getItemId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "竞拍ID不能为空");
        }
        if (order.getFinalAmount() == null || order.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "订单金额必须大于0");
        }
    }

    /**
     * 验证订单状态转换是否合法
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        switch (currentStatus) {
            case PENDING_PAYMENT:
                if (newStatus != OrderStatus.PAID && newStatus != OrderStatus.CANCELLED) {
                    throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                        "待支付订单只能变更为已支付或已取消状态");
                }
                break;
            case PAID:
                if (newStatus != OrderStatus.COMPLETED) {
                    throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                        "已支付订单只能变更为已完成状态");
                }
                break;
            case COMPLETED:
            case CANCELLED:
                throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                    "已完成或已取消的订单状态不可变更");
            default:
                break;
        }
    }

    /**
     * 查询所有订单（管理员功能）
     */
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    /**
     * 为订单加载关联信息（竞拍、商品等）
     */
    private void enrichOrderInfo(Order order) {
        // 加载竞拍信息
        if (order.getItemId() != null) {
            var auction = auctionRepository.findById(order.getItemId());
            if (auction != null) {
                order.setAuctionTitle(auction.getTitle());
                order.setAuctionStartTime(auction.getStartTime());
                order.setAuctionEndTime(auction.getEndTime());
            }
        }

        // 加载商品信息
        if (order.getProductId() != null) {
            var product = productRepository.findById(order.getProductId());
            if (product != null) {
                order.setProductName(product.getName());
                order.setProductImageUrl(product.getImageUrl());
                order.setProductDescription(product.getDescription());
            }
        }
    }

    /**
     * 订单统计信息
     */
    public record OrderStatistics(
        long totalOrders,        // 总订单数
        long pendingPaymentCount, // 待支付订单数
        long paidCount,          // 已支付订单数
        long cancelledCount      // 已取消订单数
    ) {}
}