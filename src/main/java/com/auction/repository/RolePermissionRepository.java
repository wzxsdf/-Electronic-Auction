package com.auction.repository;

import com.auction.domain.entity.RolePermission;
import com.auction.infrastructure.mapper.RolePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色权限关联Repository
 */
@Repository
@RequiredArgsConstructor
public class RolePermissionRepository {

    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 根据角色ID查询权限关联列表
     */
    public List<RolePermission> findByRoleId(Long roleId) {
        return rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );
    }

    /**
     * 根据权限ID查询角色关联列表
     */
    public List<RolePermission> findByPermissionId(Long permissionId) {
        return rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getPermissionId, permissionId)
        );
    }

    /**
     * 查询角色-权限关联
     */
    public RolePermission findByRoleIdAndPermissionId(Long roleId, Long permissionId) {
        return rolePermissionMapper.selectOne(
            new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId)
                .eq(RolePermission::getPermissionId, permissionId)
        );
    }

    /**
     * 保存角色-权限关联
     */
    public RolePermission save(RolePermission rolePermission) {
        if (rolePermission.getId() == null) {
            rolePermissionMapper.insert(rolePermission);
        } else {
            rolePermissionMapper.updateById(rolePermission);
        }
        return rolePermission;
    }

    /**
     * 批量保存角色-权限关联
     */
    public void batchSave(List<RolePermission> rolePermissions) {
        rolePermissions.forEach(this::save);
    }

    /**
     * 删除角色-权限关联
     */
    public boolean deleteByRoleIdAndPermissionId(Long roleId, Long permissionId) {
        return rolePermissionMapper.delete(
            new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId)
                .eq(RolePermission::getPermissionId, permissionId)
        ) > 0;
    }

    /**
     * 删除角色的所有权限
     */
    public boolean deleteByRoleId(Long roleId) {
        return rolePermissionMapper.delete(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        ) > 0;
    }
}
