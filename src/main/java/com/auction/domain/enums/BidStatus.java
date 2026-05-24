package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum BidStatus {
    ACTIVE("有效"),
    SUPPLANTED("被超越"),
    WINNING("领先"),
    INVALID("无效");

    private final String description;

    BidStatus(String description) {
        this.description = description;
    }
}
