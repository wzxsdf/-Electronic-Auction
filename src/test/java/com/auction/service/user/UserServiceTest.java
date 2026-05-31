package com.auction.service.user;

import com.auction.api.dto.request.UpdateUserRequest;
import com.auction.api.dto.response.UserResponse;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.User;
import com.auction.domain.enums.UserStatus;
import com.auction.repository.UserRepository;
import com.auction.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户服务测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setNickname("测试用户");
        mockUser.setEmail("test@example.com");
        mockUser.setStatusEnum(UserStatus.ACTIVE);

        updateRequest = new UpdateUserRequest();
        updateRequest.setNickname("新昵称");
        updateRequest.setEmail("newemail@example.com");
    }

    /**
     * 测试查询用户信息 - 正常流程
     */
    @Test
    void testFindById_Success() {
        // 安排
        when(userRepository.findById(1L)).thenReturn(mockUser);
        when(userRoleRepository.findRolesByUserId(1L)).thenReturn(List.of());

        // 执行
        UserResponse result = userService.findById(1L);

        // 验证
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("测试用户", result.getNickname());
        assertEquals("test@example.com", result.getEmail());
    }

    /**
     * 测试查询用户信息 - 用户不存在
     */
    @Test
    void testFindById_UserNotFound() {
        // 安排
        when(userRepository.findById(999L)).thenReturn(null);

        // 执行和验证
        BizException exception = assertThrows(BizException.class, () -> {
            userService.findById(999L);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getCode());
    }

    /**
     * 测试更新用户信息 - 正常流程
     */
    @Test
    void testUpdateUser_Success() {
        // 安排
        when(userRepository.findById(1L)).thenReturn(mockUser);
        when(userRepository.findByEmail("newemail@example.com")).thenReturn(null);
        when(userRepository.updateById(any(User.class))).thenReturn(mockUser);
        when(userRoleRepository.findRolesByUserId(1L)).thenReturn(List.of());

        // 执行
        UserResponse result = userService.updateUser(1L, updateRequest, 1L);

        // 验证
        assertNotNull(result);
        verify(userRepository, times(1)).updateById(any(User.class));
    }

    /**
     * 测试更新用户信息 - 用户不存在
     */
    @Test
    void testUpdateUser_UserNotFound() {
        // 安排
        when(userRepository.findById(999L)).thenReturn(null);

        // 执行和验证
        BizException exception = assertThrows(BizException.class, () -> {
            userService.updateUser(999L, updateRequest, 1L);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getCode());
        verify(userRepository, never()).updateById(any(User.class));
    }

    /**
     * 测试禁用用户 - 正常流程
     */
    @Test
    void testDisableUser_Success() {
        // 安排
        when(userRepository.findById(2L)).thenReturn(mockUser);
        when(userRepository.updateById(any(User.class))).thenReturn(mockUser);

        // 执行
        userService.disableUser(2L, 1L);

        // 验证
        verify(userRepository, times(1)).updateById(any(User.class));
        assertEquals(UserStatus.DISABLED, mockUser.getStatusEnum());
    }

    /**
     * 测试禁用用户 - 不能禁用自己
     */
    @Test
    void testDisableUser_CannotDisableSelf() {
        // 执行和验证
        BizException exception = assertThrows(BizException.class, () -> {
            userService.disableUser(1L, 1L);
        });

        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());
        verify(userRepository, never()).updateById(any(User.class));
    }

    /**
     * 测试启用用户 - 正常流程
     */
    @Test
    void testEnableUser_Success() {
        // 安排
        mockUser.setStatusEnum(UserStatus.DISABLED);
        when(userRepository.findById(2L)).thenReturn(mockUser);
        when(userRepository.updateById(any(User.class))).thenReturn(mockUser);

        // 执行
        userService.enableUser(2L, 1L);

        // 验证
        verify(userRepository, times(1)).updateById(any(User.class));
        assertEquals(UserStatus.ACTIVE, mockUser.getStatusEnum());
    }

    /**
     * 测试删除用户 - 正常流程
     */
    @Test
    void testDeleteUser_Success() {
        // 安排
        when(userRepository.findById(2L)).thenReturn(mockUser);
        when(userRepository.deleteById(2L)).thenReturn(true);

        // 执行
        userService.deleteUser(2L, 1L);

        // 验证
        verify(userRepository, times(1)).deleteById(2L);
    }

    /**
     * 测试删除用户 - 不能删除自己
     */
    @Test
    void testDeleteUser_CannotDeleteSelf() {
        // 执行和验证
        BizException exception = assertThrows(BizException.class, () -> {
            userService.deleteUser(1L, 1L);
        });

        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());
        verify(userRepository, never()).deleteById(any());
    }

    /**
     * 测试注销账户 - 正常流程
     */
    @Test
    void testDeactivateAccount_Success() {
        // 安排
        when(userRepository.findById(1L)).thenReturn(mockUser);
        when(userRepository.updateById(any(User.class))).thenReturn(mockUser);

        // 执行
        userService.deactivateAccount(1L);

        // 验证
        verify(userRepository, times(1)).updateById(any(User.class));
        assertEquals(UserStatus.DELETED, mockUser.getStatusEnum());
        assertTrue(mockUser.getPassword().isEmpty());
        assertNull(mockUser.getEmail());
        assertNull(mockUser.getPhone());
    }
}
