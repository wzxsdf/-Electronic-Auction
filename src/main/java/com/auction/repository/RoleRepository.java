package com.auction.repository;

import com.auction.domain.entity.Role;
import com.auction.domain.enums.RoleCode;
import com.auction.infrastructure.mapper.RoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色Repository
 */
@Repository
@RequiredArgsConstructor
public class RoleRepository {

    private final RoleMapper roleMapper;

    /**
     * 根据ID查询角色
     */
    public Role findById(Long id) {
        return roleMapper.selectById(id);
    }

    /**
     * 根据编码查询角色
     */
    public Role findByCode(String code) {
        return roleMapper.selectOne(
            new LambdaQueryWrapper<Role>().eq(Role::getCode, code)
        );
    }

    /**
     * 根据编码枚举查询角色
     */
    public Role findByCode(RoleCode roleCode) {
        if (roleCode == null) {
            return null;
        }
        return findByCode(roleCode.name());
    }

    /**
     * 查询所有角色
     */
    public List<Role> findAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 保存或更新角色
     */
    public Role save(Role role) {
        if (role.getId() == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
        return role;
    }

    /**
     * 根据用户ID查询角色列表
     */
    public List<Role> findByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    /**
     * 删除角色
     */
    public boolean deleteById(Long id) {
        return roleMapper.deleteById(id) > 0;
    }
}
