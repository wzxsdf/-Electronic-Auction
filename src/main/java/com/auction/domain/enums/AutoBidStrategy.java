package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum AutoBidStrategy {
    LAST_SEC("最后时刻"),
    SMART("智能策略"),
    AGGRESSIVE("激进出价");

    private final String description;

    AutoBidStrategy(String description) {
        this.description = description;
    }
}
