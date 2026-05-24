package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("待支付"),
    PAID("已支付"),
    CANCELLED("已取消"),
    REFUNDED("已退款");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }
}
