package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum AutoBidConfigStatus {
    ACTIVE("激活"),
    PAUSED("暂停"),
    COMPLETED("完成"),
    CANCELLED("取消");

    private final String description;

    AutoBidConfigStatus(String description) {
        this.description = description;
    }
}
