package com.auction.repository;

import com.auction.domain.entity.Permission;
import com.auction.domain.enums.PermissionCode;
import com.auction.infrastructure.mapper.PermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限Repository
 */
@Repository
@RequiredArgsConstructor
public class PermissionRepository {

    private final PermissionMapper permissionMapper;

    /**
     * 根据ID查询权限
     */
    public Permission findById(Long id) {
        return permissionMapper.selectById(id);
    }

    /**
     * 根据编码查询权限
     */
    public Permission findByCode(String code) {
        return permissionMapper.selectOne(
            new LambdaQueryWrapper<Permission>().eq(Permission::getCode, code)
        );
    }

    /**
     * 根据编码枚举查询权限
     */
    public Permission findByCode(PermissionCode permissionCode) {
        if (permissionCode == null) {
            return null;
        }
        return findByCode(permissionCode.getCode());
    }

    /**
     * 查询所有权限
     */
    public List<Permission> findAll() {
        return permissionMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 保存或更新权限
     */
    public Permission save(Permission permission) {
        if (permission.getId() == null) {
            permissionMapper.insert(permission);
        } else {
            permissionMapper.updateById(permission);
        }
        return permission;
    }

    /**
     * 根据角色ID查询权限列表
     */
    public List<Permission> findByRoleId(Long roleId) {
        return permissionMapper.selectPermissionsByRoleId(roleId);
    }

    /**
     * 根据用户ID查询权限列表
     */
    public List<Permission> findByUserId(Long userId) {
        return permissionMapper.selectPermissionsByUserId(userId);
    }

    /**
     * 删除权限
     */
    public boolean deleteById(Long id) {
        return permissionMapper.deleteById(id) > 0;
    }
}
