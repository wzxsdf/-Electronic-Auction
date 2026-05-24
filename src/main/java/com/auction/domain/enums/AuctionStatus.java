package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum AuctionStatus {
    PENDING("待开始"),
    ACTIVE("进行中"),
    PAUSED("已暂停"),
    CANCELLED("已取消"),
    COMPLETED("已结束");

    private final String description;

    AuctionStatus(String description) {
        this.description = description;
    }
}
