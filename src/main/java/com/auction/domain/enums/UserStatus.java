package com.auction.domain.enums;

import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
public enum UserStatus {
    /**
     * 正常
     */
    ACTIVE("正常"),

    /**
     * 禁用
     */
    DISABLED("禁用"),

    /**
     * 锁定
     */
    LOCKED("锁定"),

    /**
     * 待验证
     */
    PENDING_VERIFICATION("待验证"),

    /**
     * 已注销
     */
    DELETED("已注销");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    /**
     * 判断用户是否可以登录
     */
    public boolean canLogin() {
        return this == ACTIVE;
    }

    /**
     * 判断用户是否正常状态
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
}
