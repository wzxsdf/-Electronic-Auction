package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Getter
@Setter
@TableName("users")
public class User {

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户状态
     */
    private String status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 总出价次数
     */
    private Integer totalBids;

    /**
     * 总获胜次数
     */
    private Integer totalWins;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    // ==================== 非持久化字段 ====================

    /**
     * 用户角色列表（不持久化）
     */
    @TableField(exist = false)
    private java.util.List<String> roles;

    /**
     * 用户权限列表（不持久化）
     */
    @TableField(exist = false)
    private java.util.Set<String> permissions;

    // ==================== 枚举转换方法 ====================

    /**
     * 获取用户状态枚举
     */
    public UserStatus getStatusEnum() {
        if (status == null) {
            return UserStatus.ACTIVE;
        }
        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return UserStatus.ACTIVE;
        }
    }

    /**
     * 设置用户状态枚举
     */
    public void setStatusEnum(UserStatus userStatus) {
        this.status = userStatus != null ? userStatus.name() : UserStatus.ACTIVE.name();
    }

    /**
     * 判断用户是否可以登录
     */
    public boolean canLogin() {
        return getStatusEnum().canLogin();
    }

    /**
     * 判断用户是否正常状态
     */
    public boolean isActive() {
        return getStatusEnum().isActive();
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }
}
