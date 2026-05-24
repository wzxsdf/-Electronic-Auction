package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum RiskAction {
    ALLOW("允许"),
    WARN("警告"),
    BLOCK("拒绝"),
    MANUAL_REVIEW("人工审核");

    private final String description;

    RiskAction(String description) {
        this.description = description;
    }

    public static RiskAction fromLevel(RiskLevel level) {
        return switch (level) {
            case LOW -> ALLOW;
            case MEDIUM -> WARN;
            case HIGH -> BLOCK;
            case CRITICAL -> MANUAL_REVIEW;
        };
    }
}
