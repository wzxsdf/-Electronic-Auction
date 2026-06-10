package com.auction.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 安全上下文
 * 存储当前请求的安全信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecurityContext {

    /**
     * 当前认证用户信息
     */
    private UserPrincipal userPrincipal;

    /**
     * 判断是否已认证
     */
    public boolean isAuthenticated() {
        return userPrincipal != null && userPrincipal.getUserId() != null;
    }

    /**
     * 判断是否有指定角色
     */
    public boolean hasRole(String role) {
        return userPrincipal != null && role.equals(userPrincipal.getRole());
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 判断是否为商家
     */
    public boolean isMerchant() {
        return hasRole("MERCHANT");
    }

    /**
     * 判断是否为普通用户
     */
    public boolean isUser() {
        return hasRole("USER");
    }
}
