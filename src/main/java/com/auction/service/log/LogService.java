package com.auction.service.log;

import com.auction.domain.entity.OperationLog;
import com.auction.repository.OperationLogRepository;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final OperationLogRepository operationLogRepository;

    /**
     * 异步记录操作日志
     */
    @Async
    public void recordOperation(Long userId, String username, String module, String operation,
                                String method, String params, String ipAddress, String status,
                                String errorMsg, Integer duration) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(userId);
            operationLog.setUsername(username);
            operationLog.setModule(module);
            operationLog.setOperation(operation);
            operationLog.setMethod(method);
            operationLog.setParams(truncateParams(params));
            operationLog.setIpAddress(ipAddress);
            operationLog.setStatus(status);
            operationLog.setErrorMsg(errorMsg);
            operationLog.setDuration(duration);

            operationLogRepository.save(operationLog);
        } catch (Exception e) {
            // 日志记录失败不应影响主流程
            log.error("操作日志记录失败: {}", e.getMessage());
        }
    }

    /**
     * 记录成功操作
     */
    @Async
    public void recordSuccess(Long userId, String username, String module, String operation,
                              String method, String params, String ipAddress, Integer duration) {
        recordOperation(userId, username, module, operation, method, params, ipAddress, "SUCCESS", null, duration);
    }

    /**
     * 记录失败操作
     */
    @Async
    public void recordFailure(Long userId, String username, String module, String operation,
                              String method, String params, String ipAddress, String errorMsg) {
        recordOperation(userId, username, module, operation, method, params, ipAddress, "FAILURE", errorMsg, null);
    }

    /**
     * 分页查询操作日志
     */
    public Page<OperationLog> findOperationLogs(int pageNum, int pageSize) {
        return operationLogRepository.findPage(pageNum, pageSize);
    }

    /**
     * 根据用户ID分页查询操作日志
     */
    public Page<OperationLog> findOperationLogsByUserId(Long userId, int pageNum, int pageSize) {
        return operationLogRepository.findPageByUserId(userId, pageNum, pageSize);
    }

    /**
     * 清理过期日志（保留最近30天）
     */
    public void cleanExpiredLogs() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        operationLogRepository.deleteBefore(thirtyDaysAgo);
        log.info("已清理30天前的操作日志");
    }

    /**
     * 截断参数长度（防止日志过长）
     */
    private String truncateParams(String params) {
        if (params == null) {
            return null;
        }
        // 移除敏感信息
        String sanitized = params.replaceAll("\"password\":\"[^\"]+\"", "\"password\":\"***\"");
        sanitized = sanitized.replaceAll("\"token\":\"[^\"]+\"", "\"token\":\"***\"");
        // 限制长度
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }
}
