package com.auction.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户主体信息
 * 存储当前认证用户的详细信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPrincipal {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * 判断是否为商家
     */
    public boolean isMerchant() {
        return "MERCHANT".equals(role);
    }

    /**
     * 判断是否为普通用户
     */
    public boolean isUser() {
        return "USER".equals(role);
    }
}
