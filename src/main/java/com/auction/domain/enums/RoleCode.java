package com.auction.domain.enums;

import lombok.Getter;

/**
 * 角色编码枚举
 */
@Getter
public enum RoleCode {
    /**
     * 管理员
     */
    ADMIN("管理员"),

    /**
     * 商家
     */
    MERCHANT("商家"),

    /**
     * 主播
     */
    STREAMER("主播"),

    /**
     * 普通用户
     */
    USER("普通用户");

    private final String description;

    RoleCode(String description) {
        this.description = description;
    }

    /**
     * 根据编码获取枚举
     */
    public static RoleCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RoleCode roleCode : values()) {
            if (roleCode.name().equals(code)) {
                return roleCode;
            }
        }
        return null;
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
}
