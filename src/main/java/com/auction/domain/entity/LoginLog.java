package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.LoginStatus;
import com.auction.domain.enums.LoginType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
@TableName("login_logs")
public class LoginLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录类型
     */
    private String loginType;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 浏览器UA
     */
    private String userAgent;

    /**
     * 登录状态
     */
    private String status;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 获取登录类型枚举
     */
    public LoginType getLoginTypeEnum() {
        if (loginType == null) {
            return null;
        }
        try {
            return LoginType.valueOf(loginType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 设置登录类型枚举
     */
    public void setLoginTypeEnum(LoginType loginTypeEnum) {
        this.loginType = loginTypeEnum != null ? loginTypeEnum.name() : null;
    }

    /**
     * 获取登录状态枚举
     */
    public LoginStatus getLoginStatusEnum() {
        if (status == null) {
            return null;
        }
        try {
            return LoginStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 设置登录状态枚举
     */
    public void setLoginStatusEnum(LoginStatus loginStatus) {
        this.status = loginStatus != null ? loginStatus.name() : null;
    }
}
