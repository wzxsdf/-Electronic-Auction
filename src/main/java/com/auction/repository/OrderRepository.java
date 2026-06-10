package com.auction.repository;

import com.auction.domain.entity.Order;
import com.auction.domain.enums.OrderStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.auction.infrastructure.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 订单仓储层
 * 负责订单数据的持久化和查询操作
 */
@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final OrderMapper orderMapper;

    /**
     * 保存订单（新增或更新）
     */
    public Order save(Order order) {
        if (order.getId() == null) {
            orderMapper.insert(order);
        } else {
            orderMapper.updateById(order);
        }
        return order;
    }

    /**
     * 根据ID查询订单
     */
    public Order findById(Long id) {
        return orderMapper.selectById(id);
    }

    /**
     * 根据用户ID查询订单列表
     */
    public List<Order> findByUserId(Long userId) {
        return orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt)
        );
    }

    /**
     * 根据用户ID和状态查询订单
     */
    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status) {
        return orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, status.name())
                .orderByDesc(Order::getCreatedAt)
        );
    }

    /**
     * 根据竞拍ID查询订单
     */
    public Order findByAuctionId(Long auctionId) {
        return orderMapper.selectOne(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getAuctionItemId, auctionId)
                .last("LIMIT 1")
        );
    }

    /**
     * 根据商品ID查询订单列表
     */
    public List<Order> findByProductId(Long productId) {
        return orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getProductId, productId)
                .orderByDesc(Order::getCreatedAt)
        );
    }

    /**
     * 更新订单状态
     */
    public boolean updateStatus(Long orderId, OrderStatus status) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatusEnum(status);
        return orderMapper.updateById(order) > 0;
    }

    /**
     * 查询所有订单
     */
    public List<Order> findAll() {
        return orderMapper.selectList(null);
    }

    /**
     * 根据状态查询订单列表
     */
    public List<Order> findByStatus(OrderStatus status) {
        return orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, status.name())
                .orderByDesc(Order::getCreatedAt)
        );
    }

    /**
     * 删除订单
     */
    public boolean deleteById(Long id) {
        return orderMapper.deleteById(id) > 0;
    }

    /**
     * 检查订单是否存在
     */
    public boolean existsById(Long id) {
        return orderMapper.selectById(id) != null;
    }

    /**
     * 统计用户订单数量
     */
    public long countByUserId(Long userId) {
        return orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
        );
    }

    /**
     * 统计用户订单数量（按状态）
     */
    public long countByUserIdAndStatus(Long userId, OrderStatus status) {
        return orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, status.name())
        );
    }
}