package com.auction.service.auth;

import com.auction.api.dto.request.LoginRequest;
import com.auction.api.dto.request.RegisterRequest;
import com.auction.api.dto.response.LoginResponse;
import com.auction.common.AuthException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.LoginLog;
import com.auction.domain.entity.Role;
import com.auction.domain.entity.User;
import com.auction.domain.entity.UserRole;
import com.auction.domain.enums.LoginStatus;
import com.auction.domain.enums.LoginType;
import com.auction.domain.enums.RoleCode;
import com.auction.domain.enums.UserStatus;
import com.auction.repository.LoginLogRepository;
import com.auction.repository.RoleRepository;
import com.auction.repository.UserRepository;
import com.auction.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final LoginLogRepository loginLogRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final String LOGIN_LOCK_KEY_PREFIX = "login_lock:";
    private static final String LOGIN_FAIL_COUNT_PREFIX = "login_fail_count:";
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    /**
     * 用户注册
     */
    @Transactional
    public User register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new AuthException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 2. 检查邮箱是否已存在
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User existingEmailUser = userRepository.findByEmail(request.getEmail());
            if (existingEmailUser != null) {
                throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        // 3. 检查手机号是否已存在
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            User existingPhoneUser = userRepository.findByPhone(request.getPhone());
            if (existingPhoneUser != null) {
                throw new AuthException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        // 4. 密码强度检查（已通过注解验证）
        // 5. 密码不能包含用户名
        if (request.getPassword().toLowerCase().contains(request.getUsername().toLowerCase())) {
            throw new AuthException(ErrorCode.PASSWORD_CONTAINS_USERNAME);
        }

        // 6. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatusEnum(UserStatus.ACTIVE);
        user.setTotalBids(0);
        user.setTotalWins(0);

        // 7. 保存用户
        user = userRepository.save(user);

        // 8. 根据请求分配角色
        RoleCode roleCode = request.getRoleType() != null ? request.getRoleType() : RoleCode.USER;

        // 9. 验证角色类型（不允许直接注册管理员和主播）
        if (roleCode == RoleCode.ADMIN || roleCode == RoleCode.STREAMER) {
            throw new AuthException(ErrorCode.BAD_REQUEST, "不允许直接注册该角色");
        }

        // 10. 分配角色
        assignRole(user.getId(), roleCode);

        log.info("用户注册成功: userId={}, username={}, role={}", user.getId(), user.getUsername(), roleCode);
        return user;
    }

    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 1. 检查登录锁
        checkLoginLock(username, ipAddress);

        // 2. 查询用户
        User user = userRepository.findByUsername(username);
        if (user == null) {
            // 记录登录失败
            recordLoginFailure(null, username, ipAddress, userAgent, "用户不存在");
            // 增加失败计数
            incrementLoginFailCount(username, ipAddress);
            throw new AuthException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 3. 检查用户状态
        UserStatus status = user.getStatusEnum();
        if (!status.canLogin()) {
            String reason = switch (status) {
                case DISABLED -> "账户已被禁用";
                case LOCKED -> "账户已被锁定";
                case DELETED -> "账户已注销";
                case PENDING_VERIFICATION -> "账户待验证";
                default -> "账户状态异常";
            };
            // 记录登录失败
            recordLoginFailure(user.getId(), username, ipAddress, userAgent, reason);
            throw new AuthException(status == UserStatus.DISABLED ? ErrorCode.ACCOUNT_DISABLED :
                                   status == UserStatus.LOCKED ? ErrorCode.ACCOUNT_LOCKED :
                                   ErrorCode.ACCOUNT_DISABLED);
        }

        // 4. 验证密码
        if (!PASSWORD_ENCODER.matches(password, user.getPassword())) {
            // 记录登录失败
            recordLoginFailure(user.getId(), username, ipAddress, userAgent, "密码错误");
            // 增加失败计数
            incrementLoginFailCount(username, ipAddress);
            throw new AuthException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 5. 清除登录失败计数
        clearLoginFailCount(username, ipAddress);

        // 6. 记录登录成功
        recordLoginSuccess(user.getId(), username, ipAddress, userAgent);

        // 7. 更新用户最后登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.updateById(user);

        // 8. 生成Token
        List<String> roles = getUserRoles(user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 9. 缓存Refresh Token到Redis
        String refreshKey = getRefreshTokenKey(user.getId());
        redisTemplate.opsForValue().set(refreshKey, refreshToken, 7, TimeUnit.DAYS);

        log.info("用户登录成功: userId={}, username={}, ip={}", user.getId(), user.getUsername(), ipAddress);

        // 10. 构建响应
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .userId(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .avatarUrl(user.getAvatarUrl())
            .roles(roles)
            .build();
    }

    /**
     * 用户登出
     */
    public void logout(Long userId) {
        // 将Refresh Token从Redis中删除
        String refreshKey = getRefreshTokenKey(userId);
        redisTemplate.delete(refreshKey);

        log.info("用户登出: userId={}", userId);
    }

    /**
     * 刷新Token
     */
    public String refreshToken(String refreshToken) {
        try {
            // 1. 验证Refresh Token
            if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                throw new AuthException(ErrorCode.TOKEN_INVALID);
            }

            // 2. 获取用户ID
            Long userId = jwtService.getUserIdFromToken(refreshToken);

            // 3. 检查Redis中的Refresh Token是否匹配
            String refreshKey = getRefreshTokenKey(userId);
            String storedToken = redisTemplate.opsForValue().get(refreshKey);
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                throw new AuthException(ErrorCode.TOKEN_REFRESH_FAILED);
            }

            // 4. 查询用户信息
            User user = userRepository.findById(userId);
            if (user == null || !user.isActive()) {
                throw new AuthException(ErrorCode.TOKEN_REFRESH_FAILED);
            }

            // 5. 生成新的Access Token
            List<String> roles = getUserRoles(userId);
            String newAccessToken = jwtService.generateAccessToken(userId, user.getUsername(), roles);

            log.info("Token刷新成功: userId={}", userId);
            return newAccessToken;

        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage());
            throw new AuthException(ErrorCode.TOKEN_REFRESH_FAILED);
        }
    }

    /**
     * 检查登录锁
     */
    private void checkLoginLock(String username, String ipAddress) {
        // 检查用户名锁定
        String userLockKey = LOGIN_LOCK_KEY_PREFIX + "user:" + username;
        Boolean userLocked = redisTemplate.hasKey(userLockKey);
        if (Boolean.TRUE.equals(userLocked)) {
            throw new AuthException(ErrorCode.ACCOUNT_LOCKED, "账户已被锁定，请" + LOCK_DURATION_MINUTES + "分钟后重试");
        }

        // 检查IP锁定
        String ipLockKey = LOGIN_LOCK_KEY_PREFIX + "ip:" + ipAddress;
        Boolean ipLocked = redisTemplate.hasKey(ipLockKey);
        if (Boolean.TRUE.equals(ipLocked)) {
            throw new AuthException(ErrorCode.ACCOUNT_LOCKED, "该IP登录失败次数过多，请" + LOCK_DURATION_MINUTES + "分钟后重试");
        }
    }

    /**
     * 增加登录失败计数
     */
    private void incrementLoginFailCount(String username, String ipAddress) {
        // 用户名失败计数
        String userCountKey = LOGIN_FAIL_COUNT_PREFIX + "user:" + username;
        Long userCount = redisTemplate.opsForValue().increment(userCountKey);
        if (userCount != null && userCount == 1) {
            redisTemplate.expire(userCountKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        // IP失败计数
        String ipCountKey = LOGIN_FAIL_COUNT_PREFIX + "ip:" + ipAddress;
        Long ipCount = redisTemplate.opsForValue().increment(ipCountKey);
        if (ipCount != null && ipCount == 1) {
            redisTemplate.expire(ipCountKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        // 检查是否需要锁定
        if (userCount != null && userCount >= MAX_LOGIN_FAILURES) {
            String userLockKey = LOGIN_LOCK_KEY_PREFIX + "user:" + username;
            redisTemplate.opsForValue().set(userLockKey, "locked", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            log.warn("用户登录失败过多，账户已锁定: username={}", username);
        }

        if (ipCount != null && ipCount >= MAX_LOGIN_FAILURES) {
            String ipLockKey = LOGIN_LOCK_KEY_PREFIX + "ip:" + ipAddress;
            redisTemplate.opsForValue().set(ipLockKey, "locked", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            log.warn("IP登录失败过多，IP已锁定: ip={}", ipAddress);
        }
    }

    /**
     * 清除登录失败计数
     */
    private void clearLoginFailCount(String username, String ipAddress) {
        redisTemplate.delete(LOGIN_FAIL_COUNT_PREFIX + "user:" + username);
        redisTemplate.delete(LOGIN_FAIL_COUNT_PREFIX + "ip:" + ipAddress);
    }

    /**
     * 记录登录成功
     */
    private void recordLoginSuccess(Long userId, String username, String ipAddress, String userAgent) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setLoginTypeEnum(LoginType.PASSWORD);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(limitUserAgentLength(userAgent));
        loginLog.setLoginStatusEnum(LoginStatus.SUCCESS);
        loginLogRepository.save(loginLog);
    }

    /**
     * 记录登录失败
     */
    private void recordLoginFailure(Long userId, String username, String ipAddress, String userAgent, String reason) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setLoginTypeEnum(LoginType.PASSWORD);
        loginLog.setIpAddress(ipAddress);
        loginLog.setUserAgent(limitUserAgentLength(userAgent));
        loginLog.setLoginStatusEnum(LoginStatus.FAILURE);
        loginLog.setFailureReason(reason);
        loginLogRepository.save(loginLog);
    }

    /**
     * 限制UserAgent长度
     */
    private String limitUserAgentLength(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }

    /**
     * 获取用户角色
     */
    private List<String> getUserRoles(Long userId) {
        List<Role> roles = userRoleRepository.findRolesByUserId(userId);
        return roles.stream().map(Role::getCode).toList();
    }

    /**
     * 分配角色
     */
    private void assignRole(Long userId, RoleCode roleCode) {
        // 1. 查询角色
        Role role = roleRepository.findByCode(roleCode);
        if (role == null) {
            throw new AuthException(ErrorCode.BAD_REQUEST, "角色不存在: " + roleCode);
        }

        // 2. 创建用户-角色关联
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());

        // 3. 保存关联
        userRoleRepository.save(userRole);

        log.info("角色分配成功: userId={}, roleId={}, roleCode={}", userId, role.getId(), roleCode);
    }

    /**
     * 获取Refresh Token的Redis key
     */
    private String getRefreshTokenKey(Long userId) {
        return "refresh_token:" + userId;
    }
}
