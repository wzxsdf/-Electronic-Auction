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
