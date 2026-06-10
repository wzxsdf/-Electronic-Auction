package com.auction.config;

import com.auction.service.websocket.AuctionWebSocketHandler;
import com.auction.service.websocket.LiveRoomWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AuctionWebSocketHandler auctionWebSocketHandler;
    private final LiveRoomWebSocketHandler liveRoomWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 拍品 WebSocket 端点 - 用于拍品详情页
        // 连接格式: ws://host/ws/item/{itemId}?userId={userId}
        // roomKey格式: item:{itemId}
        registry.addHandler(auctionWebSocketHandler, "/ws/item/{itemId}")
            .setAllowedOrigins("*");

        // 直播间 WebSocket 端点 - 用于直播间页面
        // 连接格式: ws://host/ws/live/{auctionId}?userId={userId}&username={username}
        // roomKey格式: live:{auctionId}
        registry.addHandler(liveRoomWebSocketHandler, "/ws/live/{auctionId}")
            .setAllowedOrigins("*");
    }
}
