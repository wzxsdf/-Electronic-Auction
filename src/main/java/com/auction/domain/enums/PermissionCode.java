package com.auction.domain.enums;

import lombok.Getter;

/**
 * 权限编码枚举
 */
@Getter
public enum PermissionCode {
    // ==================== 用户权限 ====================
    /**
     * 查看用户
     */
    USER_READ("user:read", "查看用户"),

    /**
     * 更新用户信息
     */
    USER_UPDATE("user:update", "更新用户信息"),

    /**
     * 删除用户
     */
    USER_DELETE("user:delete", "删除用户"),

    // ==================== 拍卖权限 ====================
    /**
     * 创建拍卖
     */
    AUCTION_CREATE("auction:create", "创建拍卖"),

    /**
     * 管理拍卖
     */
    AUCTION_MANAGE("auction:manage", "管理拍卖"),

    /**
     * 参与拍卖
     */
    AUCTION_BID("auction:bid", "参与拍卖"),

    // ==================== 管理权限 ====================
    /**
     * 管理用户
     */
    ADMIN_USER_MANAGE("admin:user:manage", "管理用户"),

    /**
     * 系统配置
     */
    ADMIN_SYSTEM_CONFIG("admin:system:config", "系统配置"),

    /**
     * 数据导出
     */
    ADMIN_DATA_EXPORT("admin:data:export", "数据导出");

    private final String code;
    private final String description;

    PermissionCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举
     */
    public static PermissionCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PermissionCode permission : values()) {
            if (permission.code.equals(code)) {
                return permission;
            }
        }
        return null;
    }
}
