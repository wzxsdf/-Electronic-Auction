package com.auction.domain.enums;

import lombok.Getter;

@Getter
public enum MessageType {
    CONNECT("连接成功"),
    DISCONNECT("断开连接"),
    PING("心跳请求"),
    PONG("心跳响应"),

    // 竞拍状态相关
    AUCTION_START("竞拍开始"),
    AUCTION_STARTED("竞拍已开始"),
    AUCTION_END("竞拍结束"),
    AUCTION_ENDED("竞拍已结束"),
    AUCTION_EXTENDED("竞拍延时"),
    AUCTION_DELAYED("竞拍已延时"),
    AUCTION_PAUSED("竞拍暂停"),
    AUCTION_CANCELLED("竞拍取消"),

    // 出价相关
    NEW_BID("新出价"),
    PRICE_UPDATE("价格更新"),
    LEADERBOARD_UPDATE("排行榜更新"),
    BID_FAILED("出价失败"),

    // 用户通知相关
    YOU_ARE_LEADING("你领先了"),
    YOU_WERE_OVERTAKEN("你被超越了"),
    YOU_LOST("很遗憾，您未成交"),
    YOU_WON("恭喜您竞拍成功"),

    // 支付相关
    PAYMENT_SUCCESS("支付成功"),
    PAYMENT_CANCELLED("支付取消"),
    PAYMENT_FAILED("支付失败"),

    ERROR("错误消息");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }
}
