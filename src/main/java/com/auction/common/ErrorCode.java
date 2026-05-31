package com.auction.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用错误码
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 业务错误码 1000-1999
    AUCTION_NOT_FOUND(1001, "竞拍不存在"),
    AUCTION_NOT_STARTED(1002, "竞拍未开始"),
    AUCTION_ALREADY_ENDED(1003, "竞拍已结束"),
    AUCTION_CANCELLED(1004, "竞拍已取消"),

    // 出价相关 2000-2999
    BID_AMOUNT_TOO_LOW(2001, "出价金额必须高于当前价格"),
    BID_AMOUNT_INVALID(2002, "出价金额不符合加价幅度要求"),
    BID_FREQUENCY_HIGH(2003, "出价频率过高，请稍后再试"),
    BID_USER_BLOCKED(2004, "账户已被限制，无法出价"),
    BID_EXCEED_MAX_PRICE(2005, "出价超过封顶价"),

    // 用户相关 3000-3999
    USER_NOT_FOUND(3001, "用户不存在"),
    USER_BALANCE_INSUFFICIENT(3002, "余额不足"),

    // 认证相关 3100-3199
    USERNAME_ALREADY_EXISTS(3101, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(3102, "邮箱已存在"),
    PHONE_ALREADY_EXISTS(3103, "手机号已存在"),
    USERNAME_OR_PASSWORD_ERROR(3104, "用户名或密码错误"),
    ACCOUNT_DISABLED(3105, "账户已被禁用"),
    ACCOUNT_LOCKED(3106, "账户已被锁定"),
    PASSWORD_STRENGTH_LOW(3107, "密码强度不足"),
    OLD_PASSWORD_ERROR(3108, "原密码错误"),
    PASSWORD_SAME_AS_OLD(3109, "新密码不能与原密码相同"),
    PASSWORD_CONTAINS_USERNAME(3110, "密码不能包含用户名"),

    // Token相关 3200-3299
    TOKEN_INVALID(3201, "Token无效"),
    TOKEN_EXPIRED(3202, "Token已过期"),
    TOKEN_REFRESH_FAILED(3203, "Token刷新失败"),
    TOKEN_NOT_FOUND(3204, "Token不存在"),

    // 权限相关 3300-3399
    PERMISSION_DENIED(3301, "权限不足"),
    ROLE_NOT_FOUND(3302, "角色不存在"),
    PERMISSION_NOT_FOUND(3303, "权限不存在"),
    USER_ALREADY_HAS_ROLE(3304, "用户已拥有该角色"),
    USER_DOES_NOT_HAVE_ROLE(3305, "用户未拥有该角色"),

    // 商品相关 4000-4999
    PRODUCT_NOT_FOUND(4001, "商品不存在"),
    PRODUCT_ALREADY_IN_AUCTION(4002, "商品已在竞拍中"),

    // 订单相关 5000-5999
    ORDER_NOT_FOUND(5001, "订单不存在"),
    ORDER_STATUS_INVALID(5002, "订单状态无效"),
    ORDER_ALREADY_PAID(5003, "订单已支付"),
    ORDER_ALREADY_CANCELLED(5004, "订单已取消"),
    PAYMENT_FAILED(5005, "支付失败"),
    PAYMENT_TIMEOUT(5006, "支付超时"),

    // 风控相关 6000-6999
    RISK_BLOCKED(6001, "触发风控拦截"),
    RISK_HIGH(6002, "账户风险等级过高，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
