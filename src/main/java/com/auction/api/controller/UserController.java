package com.auction.api.controller;

import com.auction.annotation.RateLimit;
import com.auction.api.dto.request.ChangePasswordRequest;
import com.auction.api.dto.request.UpdateUserRequest;
import com.auction.api.dto.response.UserResponse;
import com.auction.common.Result;
import com.auction.domain.enums.UserStatus;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.auction.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询用户信息（本人或管理员）
     */
    @GetMapping("/{id}")
    public Result<UserResponse> getUser(@PathVariable Long id,
                                       @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：只能查看自己的信息，或者是管理员
        if (!id.equals(currentUserId) && !isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }
        UserResponse response = userService.findById(id);
        return Result.ok(response);
    }

    /**
     * 分页查询用户列表（管理员）
     */
    @GetMapping
    @RateLimit(key = "user_list", time = 60, count = 30, message = "查询过于频繁，请稍后再试")
    public Result<Page<UserResponse>> listUsers(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) String status,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可查询用户列表
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        Page<UserResponse> response;
        if (status != null && !status.isEmpty()) {
            UserStatus userStatus = UserStatus.valueOf(status);
            response = userService.findPageByStatus(userStatus, pageNum, pageSize);
        } else {
            response = userService.findPage(pageNum, pageSize);
        }
        return Result.ok(response);
    }

    /**
     * 搜索用户（管理员）
     */
    @GetMapping("/search")
    @RateLimit(key = "user_search", time = 60, count = 20, message = "搜索过于频繁，请稍后再试")
    public Result<java.util.List<UserResponse>> searchUsers(@RequestParam String keyword,
                                                            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可搜索用户
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        return Result.ok(userService.searchUsers(keyword));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result<UserResponse> updateUser(@PathVariable Long id,
                                           @Valid @RequestBody UpdateUserRequest request,
                                           @RequestHeader("X-User-Id") Long currentUserId) {
        UserResponse response = userService.updateUser(id, request, currentUserId);
        return Result.ok(response);
    }

    /**
     * 修改密码
     */
    @PutMapping("/me/password")
    @RateLimit(key = "change_password", time = 3600, count = 5, message = "修改密码过于频繁，请稍后再试")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                       @RequestHeader("X-User-Id") Long userId) {
        userService.changePassword(userId, request);
        return Result.ok();
    }

    /**
     * 禁用用户（管理员）
     */
    @PutMapping("/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id,
                                    @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可禁用用户
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        userService.disableUser(id, currentUserId);
        return Result.ok();
    }

    /**
     * 启用用户（管理员）
     */
    @PutMapping("/{id}/enable")
    public Result<Void> enableUser(@PathVariable Long id,
                                   @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可启用用户
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        userService.enableUser(id, currentUserId);
        return Result.ok();
    }

    /**
     * 删除用户（管理员）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id,
                                   @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        // 权限检查：仅管理员可删除用户
        if (!isAdmin(currentUserId)) {
            return Result.fail(403, "权限不足");
        }

        userService.deleteUser(id, currentUserId);
        return Result.ok();
    }

    /**
     * 注销账户（用户自己）
     */
    @DeleteMapping("/me")
    public Result<Void> deactivateAccount(@RequestHeader("X-User-Id") Long userId) {
        userService.deactivateAccount(userId);
        return Result.ok();
    }

    /**
     * 判断用户是否为管理员（简化实现）
     * 实际应用中应该通过查询用户角色来判断
     */
    private boolean isAdmin(Long userId) {
        // 简化处理：假设用户ID为1的是管理员
        // 实际应用中应该通过UserRoleRepository查询用户角色
        return userId != null && userId.equals(1L);
    }
}
