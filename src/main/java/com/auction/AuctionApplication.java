package com.auction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan({"com.auction.infrastructure.mapper", "com.auction.repository"})
@EnableScheduling
public class AuctionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionApplication.class, args);
        System.out.println("""

            ========================================
               🎯 直播竞拍全栈系统启动成功！
               API 地址: http://localhost:8080/api
               WebSocket: ws://localhost:8080/api/ws
            ========================================
            """);
    }
}
