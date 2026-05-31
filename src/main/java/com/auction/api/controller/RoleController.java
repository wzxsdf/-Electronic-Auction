package com.auction.api.controller;

import com.auction.api.dto.response.RoleResponse;
import com.auction.common.Result;
import com.auction.service.user.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 查询所有角色（管理员）
     */
    @GetMapping
    public Result<java.util.List<RoleResponse>> listRoles(
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可查询角色列表
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        return Result.ok(roleService.findAll());
    }

    /**
     * 查询角色详情（管理员）
     */
    @GetMapping("/{id}")
    public Result<RoleResponse> getRole(@PathVariable Long id,
                                       @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可查询角色详情
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        return Result.ok(roleService.findById(id));
    }

    /**
     * 为用户分配角色（管理员）
     */
    @PostMapping("/users/{userId}/assign")
    public Result<Void> assignRole(@PathVariable Long userId,
                                   @RequestParam Long roleId,
                                   @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可分配角色
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        roleService.assignRoleToUser(userId, roleId);
        return Result.ok();
    }

    /**
     * 移除用户角色（管理员）
     */
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public Result<Void> removeRole(@PathVariable Long userId,
                                   @PathVariable Long roleId,
                                   @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可移除角色
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        roleService.removeRoleFromUser(userId, roleId);
        return Result.ok();
    }

    /**
     * 查询用户的所有角色（管理员）
     */
    @GetMapping("/users/{userId}")
    public Result<java.util.List<RoleResponse>> getUserRoles(@PathVariable Long userId,
                                                           @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可查询用户角色
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        return Result.ok(roleService.findUserRoles(userId));
    }

    /**
     * 判断用户是否为管理员（简化实现）
     */
    private boolean isAdmin(Long userId) {
        // 简化处理：假设用户ID为1的是管理员
        // 实际应用中应该通过UserRoleRepository查询用户角色
        return userId != null && userId.equals(1L);
    }
}
