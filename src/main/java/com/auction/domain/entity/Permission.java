package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.PermissionCode;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 权限实体
 */
@Data
@TableName("permissions")
public class Permission {

    /**
     * 权限ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限编码
     */
    private String code;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 资源路径
     */
    private String resource;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 权限描述
     */
    private String description;

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
     * 获取权限编码枚举
     */
    public PermissionCode getPermissionCodeEnum() {
        return PermissionCode.fromCode(this.code);
    }

    /**
     * 设置权限编码枚举
     */
    public void setPermissionCodeEnum(PermissionCode permissionCode) {
        this.code = permissionCode != null ? permissionCode.getCode() : null;
    }
}
