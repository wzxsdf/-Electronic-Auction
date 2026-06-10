package com.auction.common.defensive;

import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.enums.ProductStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 防御性编程检查工具类
 * 提供常见的业务规则验证和权限检查
 */
@Slf4j
public class DefensiveCheck {

    /**
     * 检查对象是否为null
     */
    public static <T> T notNull(T obj, String fieldName) {
        if (obj == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + "不能为空");
        }
        return obj;
    }

    /**
     * 检查字符串是否为空白
     */
    public static String notBlank(String str, String fieldName) {
        if (str == null || str.trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + "不能为空");
        }
        return str;
    }

    /**
     * 检查数值是否为正数
     */
    public static <T extends Number> T positive(T num, String fieldName) {
        if (num == null || num.longValue() <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + "必须大于0");
        }
        return num;
    }

    /**
     * 检查数值是否为非负数
     */
    public static <T extends Number> T nonNegative(T num, String fieldName) {
        if (num == null || num.longValue() < 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + "不能为负数");
        }
        return num;
    }

    /**
     * 检查ID是否有效（不为null且大于0）
     */
    public static Long validId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + "无效");
        }
        return id;
    }

    /**
     * 检查资源是否存在
     */
    public static <T> T exists(T resource, String resourceType) {
        if (resource == null) {
            throw new BizException(ErrorCode.NOT_FOUND, resourceType + "不存在");
        }
        return resource;
    }

    /**
     * 检查资源所有权
     */
    public static void ownership(Long resourceOwnerId, Long currentUserId, String resourceType) {
        if (!resourceOwnerId.equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该" + resourceType);
        }
    }

    /**
     * 检查商品状态转换是否合法
     */
    public static void statusTransition(ProductStatus currentStatus, ProductStatus newStatus) {
        if (currentStatus == newStatus) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品已经是该状态，无需变更");
        }

        switch (currentStatus) {
            case PENDING_REVIEW:
                if (newStatus != ProductStatus.LISTED && newStatus != ProductStatus.DELISTED) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "待审核商品只能上架或下架");
                }
                break;
            case LISTED:
                if (newStatus != ProductStatus.DELISTED) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "已上架商品只能下架");
                }
                break;
            case DELISTED:
                if (newStatus != ProductStatus.LISTED) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "已下架商品只能上架");
                }
                break;
            default:
                throw new BizException(ErrorCode.BAD_REQUEST, "无效的状态转换");
        }
    }

    /**
     * 检查字符串长度
     */
    public static String length(String str, int maxLen, String fieldName) {
        if (str != null && str.length() > maxLen) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + "长度不能超过" + maxLen);
        }
        return str;
    }

    /**
     * 检查数值范围
     */
    public static <T extends Number> T range(T num, long min, long max, String fieldName) {
        if (num == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + "不能为空");
        }
        long value = num.longValue();
        if (value < min || value > max) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + "必须在" + min + "-" + max + "之间");
        }
        return num;
    }

    /**
     * 防止SQL注入（简单检查）
     */
    public static String safeSql(String str, String fieldName) {
        if (str != null) {
            // 检查常见的SQL注入关键词
            String[] sqlKeywords = {"'", "\"", ";", "--", "/*", "*/", "xp_", "exec", "execute"};
            for (String keyword : sqlKeywords) {
                if (str.toLowerCase().contains(keyword)) {
                    throw new BizException(ErrorCode.BAD_REQUEST, fieldName + "包含非法字符");
                }
            }
        }
        return str;
    }

    /**
     * 记录安全审计日志
     */
    public static void auditLog(String operation, Long userId, Long resourceId, String details) {
        log.info("安全审计 - 操作:{}, 用户ID:{}, 资源ID:{}, 详情:{}",
                 operation, userId, resourceId, details);
    }
}
