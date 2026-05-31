package com.auction.repository;

import com.auction.domain.entity.OperationLog;
import com.auction.infrastructure.mapper.OperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Repository
 */
@Repository
@RequiredArgsConstructor
public class OperationLogRepository {

    private final OperationLogMapper operationLogMapper;

    /**
     * 保存操作日志
     */
    public OperationLog save(OperationLog operationLog) {
        if (operationLog.getId() == null) {
            operationLogMapper.insert(operationLog);
        } else {
            operationLogMapper.updateById(operationLog);
        }
        return operationLog;
    }

    /**
     * 根据用户ID查询操作日志
     */
    public List<OperationLog> findByUserId(Long userId) {
        return operationLogMapper.selectList(
            new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getUserId, userId)
                .orderByDesc(OperationLog::getCreatedAt)
        );
    }

    /**
     * 根据模块查询操作日志
     */
    public List<OperationLog> findByModule(String module) {
        return operationLogMapper.selectList(
            new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getModule, module)
                .orderByDesc(OperationLog::getCreatedAt)
        );
    }

    /**
     * 分页查询操作日志
     */
    public Page<OperationLog> findPage(int pageNum, int pageSize) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        return operationLogMapper.selectPage(page, new LambdaQueryWrapper<OperationLog>()
            .orderByDesc(OperationLog::getCreatedAt));
    }

    /**
     * 根据用户ID分页查询操作日志
     */
    public Page<OperationLog> findPageByUserId(Long userId, int pageNum, int pageSize) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        return operationLogMapper.selectPage(page, new LambdaQueryWrapper<OperationLog>()
            .eq(OperationLog::getUserId, userId)
            .orderByDesc(OperationLog::getCreatedAt));
    }

    /**
     * 统计指定时间范围内的操作日志数量
     */
    public long countByTimeRange(LocalDateTime start, LocalDateTime end) {
        return operationLogMapper.selectCount(
            new LambdaQueryWrapper<OperationLog>()
                .ge(OperationLog::getCreatedAt, start)
                .le(OperationLog::getCreatedAt, end)
        );
    }

    /**
     * 删除指定时间之前的日志
     */
    public boolean deleteBefore(LocalDateTime before) {
        return operationLogMapper.delete(
            new LambdaQueryWrapper<OperationLog>()
                .lt(OperationLog::getCreatedAt, before)
        ) > 0;
    }
}
