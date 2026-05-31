package com.auction.domain.enums;

import lombok.Getter;

/**
 * 登录状态枚举
 */
@Getter
public enum LoginStatus {
    /**
     * 成功
     */
    SUCCESS("成功"),

    /**
     * 失败
     */
    FAILURE("失败"),

    /**
     * 账户锁定
     */
    LOCKED("账户锁定"),

    /**
     * 账户禁用
     */
    DISABLED("账户禁用");

    private final String description;

    LoginStatus(String description) {
        this.description = description;
    }
}
