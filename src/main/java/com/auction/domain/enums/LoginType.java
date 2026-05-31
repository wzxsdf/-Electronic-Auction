package com.auction.domain.enums;

import lombok.Getter;

/**
 * 登录类型枚举
 */
@Getter
public enum LoginType {
    /**
     * 用户名密码登录
     */
    PASSWORD("用户名密码"),

    /**
     * 手机验证码登录
     */
    PHONE("手机验证码"),

    /**
     * 邮箱验证码登录
     */
    EMAIL("邮箱验证码"),

    /**
     * 第三方登录
     */
    OAUTH("第三方登录");

    private final String description;

    LoginType(String description) {
        this.description = description;
    }
}
