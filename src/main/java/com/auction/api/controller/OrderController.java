package com.auction.api.controller;

import com.auction.common.Result;
import com.auction.domain.entity.Order;
import com.auction.domain.enums.OrderStatus;
import com.auction.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器：管理竞拍成功后的订单，提供订单查询、状态更新和取消功能
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 查询订单详情：根据订单ID获取完整订单信息，包含关联的竞拍和商品数据
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    @GetMapping("/{orderId}")
    public Result<Order> getOrderDetail(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderDetail(orderId);
            return Result.ok(order);
        } catch (Exception e) {
            return Result.fail(404, "订单不存在: " + e.getMessage());
        }
    }

    /**
     * 查询用户订单：获取指定用户的所有订单记录，按创建时间倒序排列
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<Order>> getUserOrders(@PathVariable Long userId) {
        try {
            List<Order> orders = orderService.getUserOrders(userId);
            return Result.ok(orders);
        } catch (Exception e) {
            return Result.fail(500, "查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 按状态查询订单：筛选指定状态的订单（待支付、已支付、已完成、已取消、已退款）
     *
     * @param userId 用户ID
     * @param status 订单状态
     * @return 订单列表
     */
    @GetMapping("/user/{userId}/status/{status}")
    public Result<List<Order>> getUserOrdersByStatus(
        @PathVariable Long userId,
        @PathVariable OrderStatus status
    ) {
        try {
            List<Order> orders = orderService.getUserOrdersByStatus(userId, status);
            return Result.ok(orders);
        } catch (Exception e) {
            return Result.fail(500, "查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 根据竞拍ID查询订单：查找指定竞拍成交后生成的订单记录
     *
     * @param auctionId 竞拍ID
     * @return 订单信息
     */
    @GetMapping("/auction/{auctionId}")
    public Result<Order> getOrderByAuctionId(@PathVariable Long auctionId) {
        try {
            Order order = orderService.getOrderByAuctionId(auctionId);
            if (order == null) {
                return Result.fail(404, "订单不存在");
            }
            return Result.ok(order);
        } catch (Exception e) {
            return Result.fail(500, "查询订单失败: " + e.getMessage());
        }
    }

    /**
     * 更新订单状态：验证状态转换合法性 → 更新订单状态 → 记录操作日志
     *
     * @param orderId 订单ID
     * @param newStatus 新状态
     * @return 操作结果
     */
    @PutMapping("/{orderId}/status")
    public Result<Void> updateOrderStatus(
        @PathVariable Long orderId,
        @RequestParam OrderStatus newStatus
    ) {
        try {
            orderService.updateOrderStatus(orderId, newStatus);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(400, "更新订单状态失败: " + e.getMessage());
        }
    }

    /**
     * 取消订单：仅待支付状态的订单可取消，更新状态并记录取消原因
     *
     * @param orderId 订单ID
     * @param reason 取消原因
     * @return 操作结果
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(
        @PathVariable Long orderId,
        @RequestParam(required = false) String reason
    ) {
        try {
            orderService.cancelOrder(orderId, reason);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(400, "取消订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单统计：汇总用户订单总数、各状态订单数量等统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    @GetMapping("/user/{userId}/statistics")
    public Result<OrderService.OrderStatistics> getUserOrderStatistics(@PathVariable Long userId) {
        try {
            OrderService.OrderStatistics statistics = orderService.getUserOrderStatistics(userId);
            return Result.ok(statistics);
        } catch (Exception e) {
            return Result.fail(500, "获取统计信息失败: " + e.getMessage());
        }
    }
}