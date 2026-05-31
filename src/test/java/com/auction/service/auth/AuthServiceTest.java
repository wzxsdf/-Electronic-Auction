package com.auction.service.auth;

import com.auction.api.dto.request.LoginRequest;
import com.auction.api.dto.request.RegisterRequest;
import com.auction.common.AuthException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.User;
import com.auction.repository.UserRepository;
import com.auction.repository.LoginLogRepository;
import com.auction.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 认证服务测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private LoginLogRepository loginLogRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("Test123456");
        registerRequest.setNickname("测试用户");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test123456");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPassword(new BCryptPasswordEncoder().encode("Test123456"));
        mockUser.setNickname("测试用户");
    }

    /**
     * 测试用户注册 - 正常流程
     */
    @Test
    void testRegister_Success() {
        // 安排
        when(userRepository.findByUsername("testuser")).thenReturn(null);
        when(userRepository.findByEmail(any())).thenReturn(null);
        when(userRepository.findByPhone(any())).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // 执行
        User result = authService.register(registerRequest);

        // 验证
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * 测试用户注册 - 用户名已存在
     */
    @Test
    void testRegister_UsernameAlreadyExists() {
        // 安排
        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);

        // 执行和验证
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS, exception.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * 测试用户注册 - 邮箱已存在
     */
    @Test
    void testRegister_EmailAlreadyExists() {
        // 安排
        registerRequest.setEmail("existing@example.com");
        when(userRepository.findByUsername("testuser")).thenReturn(null);
        when(userRepository.findByEmail("existing@example.com")).thenReturn(mockUser);

        // 执行和验证
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * 测试用户登录 - 正常流程
     */
    @Test
    void testLogin_Success() {
        // 安排
        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);
        when(jwtService.generateAccessToken(anyLong(), anyString(), anyList()))
            .thenReturn("mock-access-token");
        when(jwtService.generateRefreshToken(anyLong()))
            .thenReturn("mock-refresh-token");
        when(userRoleRepository.findRolesByUserId(anyLong())).thenReturn(java.util.List.of());

        // 执行
        var result = authService.login(loginRequest, "127.0.0.1", "Mozilla/5.0");

        // 验证
        assertNotNull(result);
        assertEquals("mock-access-token", result.getAccessToken());
        assertEquals("mock-refresh-token", result.getRefreshToken());
        assertEquals(1L, result.getUserId());
        assertEquals("testuser", result.getUsername());

        // 验证日志记录
        verify(loginLogRepository, times(1)).save(any());
    }

    /**
     * 测试用户登录 - 用户不存在
     */
    @Test
    void testLogin_UserNotFound() {
        // 安排
        when(userRepository.findByUsername("testuser")).thenReturn(null);

        // 执行和验证
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login(loginRequest, "127.0.0.1", "Mozilla/5.0");
        });

        assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR, exception.getCode());
        verify(loginLogRepository, times(1)).save(any());
    }

    /**
     * 测试用户登录 - 密码错误
     */
    @Test
    void testLogin_WrongPassword() {
        // 安排
        loginRequest.setPassword("WrongPassword123");
        when(userRepository.findByUsername("testuser")).thenReturn(mockUser);

        // 执行和验证
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login(loginRequest, "127.0.0.1", "Mozilla/5.0");
        });

        assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR, exception.getCode());
        verify(loginLogRepository, times(1)).save(any());
    }
}
