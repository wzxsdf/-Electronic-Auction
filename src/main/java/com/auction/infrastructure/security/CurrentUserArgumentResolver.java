package com.auction.infrastructure.security;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 当前用户参数解析器
 * 解析@CurrentUser注解，自动注入当前用户信息
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 检查参数是否有@CurrentUser注解且类型为UserPrincipal
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(UserPrincipal.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        // 获取@CurrentUser注解
        CurrentUser currentUserAnnotation = parameter.getParameterAnnotation(CurrentUser.class);

        // 从SecurityContext获取当前用户
        UserPrincipal userPrincipal = SecurityContextHolder.getCurrentUser();

        // 如果required=true且没有用户信息，抛出异常
        if (currentUserAnnotation != null && currentUserAnnotation.required()) {
            if (userPrincipal == null || userPrincipal.getUserId() == null) {
                throw new RuntimeException("未登录或认证信息无效");
            }
        }

        // 返回用户信息（可能为null，当required=false时）
        return userPrincipal;
    }
}
