package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum MessageType {
    CONNECT("连接成功"),
    DISCONNECT("断开连接"),
    PING("心跳请求"),
    PONG("心跳响应"),

    AUCTION_START("竞拍开始"),
    AUCTION_END("竞拍结束"),
    AUCTION_EXTENDED("竞拍延时"),
    AUCTION_PAUSED("竞拍暂停"),
    AUCTION_CANCELLED("竞拍取消"),

    NEW_BID("新出价"),
    PRICE_UPDATE("价格更新"),
    LEADERBOARD_UPDATE("排行榜更新"),

    YOU_ARE_LEADING("你领先了"),
    YOU_WERE_OVERTAKEN("你被超越了"),
    YOU_LOST("很遗憾，您未成交"),

    ERROR("错误消息");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }
}
