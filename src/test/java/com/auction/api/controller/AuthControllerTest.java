package com.auction.api.controller;

import com.auction.api.dto.request.LoginRequest;
import com.auction.api.dto.request.RegisterRequest;
import com.auction.api.dto.response.LoginResponse;
import com.auction.api.dto.response.UserResponse;
import com.auction.common.Result;
import com.auction.domain.entity.User;
import com.auction.service.auth.AuthService;
import com.auction.service.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证控制器测试
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
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
        mockUser.setNickname("测试用户");

        loginResponse = LoginResponse.builder()
            .accessToken("mock-access-token")
            .refreshToken("mock-refresh-token")
            .userId(1L)
            .username("testuser")
            .nickname("测试用户")
            .build();
    }

    /**
     * 测试用户注册 - 正常流程
     */
    @Test
    void testRegister_Success() throws Exception {
        // 安排
        when(authService.register(any(RegisterRequest.class))).thenReturn(mockUser);
        when(userService.findById(1L)).thenReturn(UserResponse.builder()
            .id(1L)
            .username("testuser")
            .nickname("测试用户")
            .build());

        // 执行和验证
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));
    }

    /**
     * 测试用户注册 - 参数验证失败
     */
    @Test
    void testRegister_ValidationFailed() throws Exception {
        // 设置无效的用户名（太短）
        registerRequest.setUsername("ab");

        // 执行和验证
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试用户登录 - 正常流程
     */
    @Test
    void testLogin_Success() throws Exception {
        // 安排
        when(authService.login(any(LoginRequest.class), anyString(), anyString()))
            .thenReturn(loginResponse);

        // 执行和验证
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    /**
     * 测试用户登录 - 参数验证失败
     */
    @Test
    void testLogin_ValidationFailed() throws Exception {
        // 设置空的密码
        loginRequest.setPassword("");

        // 执行和验证
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试获取当前用户信息 - 正常流程
     */
    @Test
    void testGetCurrentUser_Success() throws Exception {
        // 安排
        when(userService.findCurrentUser(1L)).thenReturn(UserResponse.builder()
            .id(1L)
            .username("testuser")
            .nickname("测试用户")
            .build());

        // 执行和验证
        mockMvc.perform(get("/auth/me")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));
    }

    /**
     * 测试用户登出 - 正常流程
     */
    @Test
    void testLogout_Success() throws Exception {
        // 执行和验证
        mockMvc.perform(post("/auth/logout")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试刷新Token - 正常流程
     */
    @Test
    void testRefreshToken_Success() throws Exception {
        // 安排
        when(authService.refreshToken(anyString())).thenReturn("new-access-token");

        // 执行和验证
        mockMvc.perform(post("/auth/refresh")
                .header("Authorization", "Bearer mock-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("new-access-token"));
    }
}
