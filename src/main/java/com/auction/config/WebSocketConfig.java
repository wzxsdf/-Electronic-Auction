package com.auction.config;

import com.auction.service.websocket.AuctionWebSocketHandler;
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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 拍品 WebSocket 端点
        registry.addHandler(auctionWebSocketHandler, "/ws/item/{itemId}")
            .setAllowedOrigins("*");

        // 直播间 WebSocket 端点（可选）
        registry.addHandler(auctionWebSocketHandler, "/ws/room/{roomId}")
            .setAllowedOrigins("*");
    }
}
