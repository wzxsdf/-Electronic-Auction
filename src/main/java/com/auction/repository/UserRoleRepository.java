package com.auction.repository;

import com.auction.domain.entity.UserRole;
import com.auction.domain.entity.Role;
import com.auction.infrastructure.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关联Repository
 */
@Repository
@RequiredArgsConstructor
public class UserRoleRepository {

    private final UserRoleMapper userRoleMapper;

    /**
     * 根据用户ID查询角色关联列表
     */
    public List<UserRole> findByUserId(Long userId) {
        return userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
    }

    /**
     * 根据用户ID查询角色列表（通过关联查询）
     */
    public List<Role> findRolesByUserId(Long userId) {
        return userRoleMapper.selectRolesByUserId(userId);
    }

    /**
     * 根据角色ID查询用户关联列表
     */
    public List<UserRole> findByRoleId(Long roleId) {
        return userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)
        );
    }

    /**
     * 查询用户-角色关联
     */
    public UserRole findByUserIdAndRoleId(Long userId, Long roleId) {
        return userRoleMapper.selectOne(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId)
        );
    }

    /**
     * 保存用户-角色关联
     */
    public UserRole save(UserRole userRole) {
        if (userRole.getId() == null) {
            userRoleMapper.insert(userRole);
        } else {
            userRoleMapper.updateById(userRole);
        }
        return userRole;
    }

    /**
     * 删除用户-角色关联
     */
    public boolean deleteByUserIdAndRoleId(Long userId, Long roleId) {
        return userRoleMapper.delete(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId)
        ) > 0;
    }

    /**
     * 删除用户的所有角色
     */
    public boolean deleteByUserId(Long userId) {
        return userRoleMapper.delete(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ) > 0;
    }
}
