package com.auction.api.controller;

import com.auction.common.Result;
import com.auction.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final RedisService redisService;

    /**
     * 健康检查接口
     */
    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        Map<String, String> data = new HashMap<>();
        data.put("status", "ok");
        data.put("message", "直播竞拍系统运行中");
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return Result.ok(data);
    }

    /**
     * 测试Redis写入功能
     */
    @PostMapping("/redis/set")
    public Result<Void> testRedisSet(@RequestParam String key, @RequestParam String value) {
        redisService.set(key, value, 60, java.util.concurrent.TimeUnit.SECONDS);
        return Result.ok();
    }

    /**
     * 测试Redis读取功能
     */
    @GetMapping("/redis/get")
    public Result<Object> testRedisGet(@RequestParam String key) {
        Object value = redisService.get(key);
        return Result.ok(value);
    }
}
