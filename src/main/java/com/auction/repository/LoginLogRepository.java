package com.auction.repository;

import com.auction.domain.entity.LoginLog;
import com.auction.infrastructure.mapper.LoginLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志Repository
 */
@Repository
@RequiredArgsConstructor
public class LoginLogRepository {

    private final LoginLogMapper loginLogMapper;

    /**
     * 保存登录日志
     */
    public LoginLog save(LoginLog loginLog) {
        if (loginLog.getId() == null) {
            loginLogMapper.insert(loginLog);
        } else {
            loginLogMapper.updateById(loginLog);
        }
        return loginLog;
    }

    /**
     * 根据用户ID查询登录日志
     */
    public List<LoginLog> findByUserId(Long userId) {
        return loginLogMapper.selectList(
            new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getCreatedAt)
        );
    }

    /**
     * 根据用户ID查询最近登录日志
     */
    public List<LoginLog> findRecentByUserId(Long userId, int limit) {
        return loginLogMapper.selectList(
            new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getCreatedAt)
                .last("LIMIT " + limit)
        );
    }

    /**
     * 统计用户在指定时间范围内登录失败次数
     */
    public long countFailedLogins(Long userId, LocalDateTime since) {
        return loginLogMapper.selectCount(
            new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId)
                .eq(LoginLog::getStatus, "FAILURE")
                .ge(LoginLog::getCreatedAt, since)
        );
    }

    /**
     * 统计指定IP在最近时间范围内的登录失败次数
     */
    public long countFailedLoginsByIp(String ipAddress, LocalDateTime since) {
        return loginLogMapper.selectCount(
            new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getIpAddress, ipAddress)
                .eq(LoginLog::getStatus, "FAILURE")
                .ge(LoginLog::getCreatedAt, since)
        );
    }
}
