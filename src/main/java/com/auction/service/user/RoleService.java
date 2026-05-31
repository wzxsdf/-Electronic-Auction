package com.auction.service.user;

import com.auction.api.dto.response.RoleResponse;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Role;
import com.auction.domain.entity.RolePermission;
import com.auction.domain.entity.UserRole;
import com.auction.domain.enums.RoleCode;
import com.auction.repository.RoleRepository;
import com.auction.repository.RolePermissionRepository;
import com.auction.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * 查询所有角色
     */
    public List<RoleResponse> findAll() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * 根据ID查询角色
     */
    public RoleResponse findById(Long id) {
        Role role = roleRepository.findById(id);
        if (role == null) {
            throw new BizException(ErrorCode.ROLE_NOT_FOUND);
        }
        return convertToResponse(role);
    }

    /**
     * 根据编码查询角色
     */
    public RoleResponse findByCode(RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode);
        if (role == null) {
            throw new BizException(ErrorCode.ROLE_NOT_FOUND);
        }
        return convertToResponse(role);
    }

    /**
     * 为用户分配角色
     */
    @Transactional
    public void assignRoleToUser(Long userId, Long roleId) {
        // 检查角色是否存在
        Role role = roleRepository.findById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 检查用户是否已有该角色
        UserRole existing = userRoleRepository.findByUserIdAndRoleId(userId, roleId);
        if (existing != null) {
            throw new BizException(ErrorCode.USER_ALREADY_HAS_ROLE);
        }

        // 创建用户-角色关联
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleRepository.save(userRole);

        log.info("用户角色分配成功: userId={}, roleId={}", userId, roleId);
    }

    /**
     * 移除用户角色
     */
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        // 检查用户是否拥有该角色
        UserRole existing = userRoleRepository.findByUserIdAndRoleId(userId, roleId);
        if (existing == null) {
            throw new BizException(ErrorCode.USER_DOES_NOT_HAVE_ROLE);
        }

        // 删除用户-角色关联
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);

        log.info("用户角色移除成功: userId={}, roleId={}", userId, roleId);
    }

    /**
     * 查询用户的所有角色
     */
    public List<RoleResponse> findUserRoles(Long userId) {
        List<Role> roles = userRoleRepository.findRolesByUserId(userId);
        return roles.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * 查询角色的所有权限
     */
    public List<String> findRolePermissions(Long roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
            .map(rp -> {
                // 这里需要通过PermissionRepository查询Permission
                // 简化处理：直接返回权限ID列表
                return "PERMISSION_" + rp.getPermissionId();
            })
            .collect(Collectors.toList());
    }

    /**
     * 转换为响应DTO
     */
    private RoleResponse convertToResponse(Role role) {
        return RoleResponse.builder()
            .id(role.getId())
            .code(role.getCode())
            .name(role.getName())
            .description(role.getDescription())
            .createdAt(role.getCreatedAt())
            .updatedAt(role.getUpdatedAt())
            .build();
    }
}
