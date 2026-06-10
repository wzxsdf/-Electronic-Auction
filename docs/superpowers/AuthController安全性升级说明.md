# AuthController 安全性升级说明

## 修改概述

将 `AuthController` 中的 `@RequestHeader("X-User-Id")` 改为 `@CurrentUser UserPrincipal`，提升安全性。

## 修改对比

### 修改前（不安全）

```java
@PostMapping("/logout")
public Result<Void> logout(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
    if (userId != null) {
        authService.logout(userId);
    }
    return Result.ok();
}

@GetMapping("/me")
public Result<UserResponse> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
    UserResponse response = userService.findCurrentUser(userId);
    return Result.ok(response);
}
```

**安全问题：**
- ❌ 客户端可以伪造 `X-User-Id` 请求头
- ❌ 可以访问其他用户的资源
- ❌ 存在越权访问风险

### 修改后（安全）

```java
@PostMapping("/logout")
public Result<Void> logout(@CurrentUser UserPrincipal currentUser) {
    if (currentUser != null) {
        authService.logout(currentUser.getUserId());
    }
    return Result.ok();
}

@GetMapping("/me")
public Result<UserResponse> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
    UserResponse response = userService.findCurrentUser(currentUser.getUserId());
    return Result.ok(response);
}
```

**安全优势：**
- ✅ 从 JWT Token 中自动解析用户信息
- ✅ Token 无法伪造，保证身份真实性
- ✅ 防止越权访问
- ✅ 代码更简洁统一

## 工作原理

### @CurrentUser 注解的工作流程

```
1. 客户端请求
   ↓
   Header: Authorization: Bearer <jwt_token>
   ↓
2. JwtAuthenticationFilter 拦截
   ↓
3. 验证 JWT Token
   ↓
4. 解析用户信息（userId, username, roles）
   ↓
5. 存入 SecurityContext
   ↓
6. CurrentUserArgumentResolver 解析
   ↓
7. 自动注入 @CurrentUser 参数
   ↓
8. Controller 方法执行
```

### 代码示例

**前端请求：**
```javascript
// 自动携带 JWT Token
axios.get('/auth/me', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

**后端处理：**
```java
@GetMapping("/me")
public Result<UserResponse> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
    // currentUser 自动从 JWT Token 中注入
    Long userId = currentUser.getUserId();
    String username = currentUser.getUsername();
    List<String> roles = currentUser.getRoles();
    
    return Result.ok(userService.findCurrentUser(userId));
}
```

## 修改的接口

| 接口 | 修改前 | 修改后 |
|------|--------|--------|
| POST /auth/logout | `@RequestHeader("X-User-Id")` | `@CurrentUser UserPrincipal` |
| GET /auth/me | `@RequestHeader("X-User-Id")` | `@CurrentUser UserPrincipal` |

**注意：** POST /auth/refresh 接口仍使用 `@RequestHeader("Authorization")`，因为需要从请求头获取 Refresh Token。

## UserPrincipal 对象

`UserPrincipal` 包含以下信息：

```java
public class UserPrincipal {
    private Long userId;        // 用户ID
    private String username;    // 用户名
    private String nickname;    // 昵称
    private String role;        // 角色（MERCHANT/USER/ADMIN）
    private List<String> roles; // 角色列表
    
    // 便捷方法
    public boolean isMerchant() {
        return "MERCHANT".equals(this.role);
    }
    
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }
}
```

## 安全性对比

### 场景1：伪造用户ID

**修改前（不安全）：**
```bash
# 恶意用户可以直接伪造请求头
curl -X POST http://localhost:8080/auth/logout \
  -H "X-User-Id: 12345"  # 伪造为用户12345
```
风险：可以登出其他用户的账户

**修改后（安全）：**
```bash
# 必须提供有效的 JWT Token
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer <valid_jwt_token>"
```
保护：只能操作自己的账户

### 场景2：越权访问

**修改前（不安全）：**
```bash
# 用户A可以访问用户B的信息
curl http://localhost:8080/auth/me \
  -H "X-User-Id: 999"  # 修改为用户B的ID
```
风险：可以查看其他用户信息

**修改后（安全）：**
```bash
# 只能查看自己的信息
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer <user_a_token>"
```
保护：Token 中的用户ID无法伪造

## 测试验证

### 1. 登录获取 Token
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "merchant1",
    "password": "Test1234"
  }'
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 2,
    "username": "merchant1"
  }
}
```

### 2. 使用 Token 访问接口
```bash
# 正确使用
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 3. 尝试伪造请求（会被拒绝）
```bash
# 尝试使用旧的 X-User-Id 方式（现在已不支持）
curl http://localhost:8080/auth/me \
  -H "X-User-Id: 999"
```

**预期响应（如果需要认证但未提供）：**
```json
{
  "code": 401,
  "message": "未授权"
}
```

## 迁移指南

### 前端代码迁移

**修改前：**
```javascript
// 需要手动传递 userId
axios.post('/auth/logout', null, {
  headers: {
    'X-User-Id': currentUser.id
  }
});
```

**修改后：**
```javascript
// 只需要传递 JWT Token
axios.post('/auth/logout', null, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

### Axios 拦截器配置（推荐）

```javascript
// 自动添加 JWT Token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});
```

## 总结

### 优势

| 特性 | 修改前 | 修改后 |
|------|--------|--------|
| **安全性** | ❌ 可伪造 | ✅ 无法伪造 |
| **便捷性** | 需要手动传递 | ✅ 自动注入 |
| **统一性** | 不统一 | ✅ 与其他Controller一致 |
| **验证** | ❌ 无身份验证 | ✅ JWT验证 |

### 影响范围

- ✅ 提升系统安全性
- ✅ 代码更简洁
- ✅ 与 ProductController 等保持一致
- ⚠️ 前端需要调整请求头（从 X-User-Id 改为 Authorization）

### 无需修改的部分

- ✅ `POST /auth/register` - 注册接口无需认证
- ✅ `POST /auth/login` - 登录接口无需认证
- ⚠️ `POST /auth/refresh` - 仍需要从 Authorization 头获取 Refresh Token

## 完整的修改后代码

```java
package com.auction.api.controller;

import com.auction.api.dto.request.LoginRequest;
import com.auction.api.dto.request.RegisterRequest;
import com.auction.api.dto.response.LoginResponse;
import com.auction.api.dto.response.UserResponse;
import com.auction.annotation.RateLimit;
import com.auction.common.Result;
import com.auction.domain.entity.User;
import com.auction.infrastructure.security.CurrentUser;
import com.auction.infrastructure.security.UserPrincipal;
import com.auction.service.auth.AuthService;
import com.auction.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @RateLimit(key = "register", time = 3600, count = 5, message = "注册过于频繁，请稍后再试")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        UserResponse response = userService.findById(user.getId());
        return Result.ok(response);
    }

    @PostMapping("/login")
    @RateLimit(key = "login", time = 60, count = 10, message = "登录过于频繁，请稍后再试")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, ipAddress, userAgent);
        return Result.ok(response);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@CurrentUser UserPrincipal currentUser) {
        if (currentUser != null) {
            authService.logout(currentUser.getUserId());
        }
        return Result.ok();
    }

    @PostMapping("/refresh")
    public Result<String> refreshToken(@RequestHeader("Authorization") String authorization) {
        String refreshToken = authorization.replace("Bearer ", "");
        String newAccessToken = authService.refreshToken(refreshToken);
        return Result.ok(newAccessToken);
    }

    @GetMapping("/me")
    public Result<UserResponse> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        UserResponse response = userService.findCurrentUser(currentUser.getUserId());
        return Result.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

修改已完成！现在所有认证接口都使用统一的 JWT Token 方式，系统更加安全可靠。
