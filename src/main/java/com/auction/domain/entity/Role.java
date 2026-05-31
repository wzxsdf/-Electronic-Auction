package com.auction.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.auction.domain.enums.RoleCode;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@TableName("roles")
public class Role {

    /**
     * 角色ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色描述
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
     * 获取角色编码枚举
     */
    public RoleCode getRoleCodeEnum() {
        return RoleCode.fromCode(this.code);
    }

    /**
     * 设置角色编码枚举
     */
    public void setRoleCodeEnum(RoleCode roleCode) {
        this.code = roleCode != null ? roleCode.name() : null;
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return RoleCode.ADMIN.name().equals(this.code);
    }
}
