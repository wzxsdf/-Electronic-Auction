package com.auction.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 基于 Redis 实现分布式限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流键的前缀
     */
    String key() default "rate_limit";

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 时间窗口内最大请求次数
     */
    int count() default 100;

    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
