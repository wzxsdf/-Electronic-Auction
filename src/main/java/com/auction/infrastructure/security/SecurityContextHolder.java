package com.auction.infrastructure.security;

/**
 * 安全上下文持有者
 * 使用ThreadLocal存储当前请求的用户信息
 */
public class SecurityContextHolder {

    /**
     * 使用ThreadLocal存储用户信息，确保线程安全
     */
    private static final ThreadLocal<SecurityContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置安全上下文
     */
    public static void setContext(SecurityContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取安全上下文
     */
    public static SecurityContext getContext() {
        SecurityContext context = CONTEXT_HOLDER.get();
        if (context == null) {
            // 返回空的安全上下文
            return new SecurityContext(null);
        }
        return context;
    }

    /**
     * 获取当前用户信息
     */
    public static UserPrincipal getCurrentUser() {
        return getContext().getUserPrincipal();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentUserRole() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    /**
     * 清除安全上下文
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
