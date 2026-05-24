package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum RiskLevel {
    LOW(0.3, "低风险"),
    MEDIUM(0.6, "中风险"),
    HIGH(0.8, "高风险"),
    CRITICAL(1.0, "极高风险");

    private final double threshold;
    private final String description;

    RiskLevel(double threshold, String description) {
        this.threshold = threshold;
        this.description = description;
    }

    public static RiskLevel fromScore(double score) {
        if (score >= CRITICAL.threshold) return CRITICAL;
        if (score >= HIGH.threshold) return HIGH;
        if (score >= MEDIUM.threshold) return MEDIUM;
        return LOW;
    }
}
