package com.auction.service.user;

import com.auction.api.dto.request.ChangePasswordRequest;
import com.auction.api.dto.request.UpdateUserRequest;
import com.auction.api.dto.response.UserResponse;
import com.auction.common.AuthException;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Role;
import com.auction.domain.entity.User;
import com.auction.domain.enums.UserStatus;
import com.auction.repository.UserRoleRepository;
import com.auction.repository.UserRepository;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 根据ID查询用户
     */
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToResponse(user);
    }

    /**
     * 查询当前登录用户信息
     */
    public UserResponse findCurrentUser(Long currentUserId) {
        User user = userRepository.findById(currentUserId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToResponse(user);
    }

    /**
     * 分页查询用户列表（管理员）
     */
    public Page<UserResponse> findPage(int pageNum, int pageSize) {
        Page<User> userPage = userRepository.findPage(pageNum, pageSize);
        Page<UserResponse> responsePage = new Page<>(pageNum, pageSize, userPage.getTotal());
        responsePage.setRecords(userPage.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList()));
        return responsePage;
    }

    /**
     * 根据状态分页查询用户列表
     */
    public Page<UserResponse> findPageByStatus(UserStatus status, int pageNum, int pageSize) {
        Page<User> userPage = userRepository.findPageByStatus(status, pageNum, pageSize);
        Page<UserResponse> responsePage = new Page<>(pageNum, pageSize, userPage.getTotal());
        responsePage.setRecords(userPage.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList()));
        return responsePage;
    }

    /**
     * 搜索用户
     */
    public List<UserResponse> searchUsers(String keyword) {
        List<User> users = userRepository.searchUsers(keyword);
        return users.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request, Long currentUserId) {
        // 权限检查：只能更新自己的信息，或者是管理员
        if (!userId.equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new BizException(ErrorCode.PERMISSION_DENIED);
        }

        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查用户状态
        if (!user.isActive()) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 更新昵称
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            user.setNickname(request.getNickname());
        }

        // 更新头像
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // 更新邮箱（需要检查唯一性）
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!request.getEmail().equals(user.getEmail())) {
                // 检查邮箱是否已被其他用户使用
                User existingUser = userRepository.findByEmail(request.getEmail());
                if (existingUser != null && !existingUser.getId().equals(userId)) {
                    throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS);
                }
                user.setEmail(request.getEmail());
            }
        }

        // 更新手机号（需要检查唯一性）
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (!request.getPhone().equals(user.getPhone())) {
                // 检查手机号是否已被其他用户使用
                User existingUser = userRepository.findByPhone(request.getPhone());
                if (existingUser != null && !existingUser.getId().equals(userId)) {
                    throw new AuthException(ErrorCode.PHONE_ALREADY_EXISTS);
                }
                user.setPhone(request.getPhone());
            }
        }

        // 保存更新
        user = userRepository.updateById(user);

        log.info("用户信息更新成功: userId={}, operatorId={}", userId, currentUserId);
        return convertToResponse(user);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查用户状态
        if (!user.isActive()) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 验证原密码
        if (!PASSWORD_ENCODER.matches(request.getOldPassword(), user.getPassword())) {
            throw new AuthException(ErrorCode.OLD_PASSWORD_ERROR);
        }

        // 确认新密码
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        // 检查新密码是否与原密码相同
        if (PASSWORD_ENCODER.matches(request.getNewPassword(), user.getPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }

        // 检查密码是否包含用户名
        if (request.getNewPassword().toLowerCase().contains(user.getUsername().toLowerCase())) {
            throw new AuthException(ErrorCode.PASSWORD_CONTAINS_USERNAME);
        }

        // 更新密码
        user.setPassword(PASSWORD_ENCODER.encode(request.getNewPassword()));
        userRepository.updateById(user);

        log.info("用户密码修改成功: userId={}", userId);
    }

    /**
     * 禁用用户（管理员）
     */
    @Transactional
    public void disableUser(Long userId, Long operatorId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 不能禁用自己
        if (userId.equals(operatorId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能禁用自己的账户");
        }

        user.setStatusEnum(UserStatus.DISABLED);
        userRepository.updateById(user);

        log.info("用户已禁用: userId={}, operatorId={}", userId, operatorId);
    }

    /**
     * 启用用户（管理员）
     */
    @Transactional
    public void enableUser(Long userId, Long operatorId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatusEnum(UserStatus.ACTIVE);
        userRepository.updateById(user);

        log.info("用户已启用: userId={}, operatorId={}", userId, operatorId);
    }

    /**
     * 删除用户（管理员）
     */
    @Transactional
    public void deleteUser(Long userId, Long operatorId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 不能删除自己
        if (userId.equals(operatorId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能删除自己的账户");
        }

        // 删除用户角色关联
        userRoleRepository.deleteByUserId(userId);

        // 删除用户
        userRepository.deleteById(userId);

        log.info("用户已删除: userId={}, operatorId={}", userId, operatorId);
    }

    /**
     * 注销账户（用户自己）
     */
    @Transactional
    public void deactivateAccount(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 逻辑删除：将状态改为已注销
        user.setStatusEnum(UserStatus.DELETED);
        // 清空敏感信息
        user.setPassword("");
        user.setEmail(null);
        user.setPhone(null);
        userRepository.updateById(user);

        log.info("用户账户已注销: userId={}", userId);
    }

    /**
     * 判断用户是否为管理员
     */
    private boolean isAdmin(Long userId) {
        List<Role> roles = userRoleRepository.findRolesByUserId(userId);
        return roles.stream().anyMatch(role -> "ADMIN".equals(role.getCode()));
    }

    /**
     * 转换为响应DTO
     */
    private UserResponse convertToResponse(User user) {
        // 获取用户角色
        List<Role> roles = userRoleRepository.findRolesByUserId(user.getId());
        List<String> roleCodes = roles.stream()
            .map(Role::getCode)
            .collect(Collectors.toList());

        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .avatarUrl(user.getAvatarUrl())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus())
            .statusDesc(user.getStatusEnum().getDescription())
            .lastLoginAt(user.getLastLoginAt())
            .totalBids(user.getTotalBids())
            .totalWins(user.getTotalWins())
            .roles(roleCodes)
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}
