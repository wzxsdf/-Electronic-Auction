package com.auction.infrastructure.security;

import java.lang.annotation.*;

/**
 * 当前用户注解
 * 用于方法参数，自动注入当前登录用户信息
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {

    /**
     * 是否必须认证
     * true：必须登录，未登录返回401
     * false：可选认证，未登录时注入null
     */
    boolean required() default true;
}
