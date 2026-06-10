# @CurrentUser 注解使用指南

## 📖 简介

`@CurrentUser` 是一个自定义注解，用于在Controller方法中自动注入当前登录用户信息，避免手动传递 `userId` 和 `userRole` 参数。

---

## 🎯 核心特性

### 1. 自动注入用户信息
从 JWT Token 中自动解析用户信息并注入到方法参数中。

### 2. 支持可选认证
通过 `required` 参数控制是否必须登录。

### 3. 类型安全
直接注入 `UserPrincipal` 对象，提供类型安全的访问。

---

## 💡 使用方式

### 方式1：必须认证（默认）

**适用场景**：需要登录才能访问的接口

```java
@PostMapping
public Result<ProductResponse> create(
    @Valid @RequestBody CreateProductRequest request,
    @CurrentUser UserPrincipal currentUser) {  // 默认 required=true
    
    // currentUser 不会为 null，因为用户必须登录
    Long userId = currentUser.getUserId();
    String role = currentUser.getRole();
    
    return Result.ok(productService.create(request, userId));
}
```

**行为**：
- ✅ 用户已登录 → 正常注入用户信息
- ❌ 用户未登录 → 抛出异常 "未登录或认证信息无效"

---

### 方式2：可选认证

**适用场景**：公开接口，但登录用户可以看到更多信息

```java
@GetMapping
public Result<List<ProductResponse>> queryProducts(
    ProductQueryRequest request,
    @CurrentUser(required = false) UserPrincipal currentUser) {  // required=false
    
    if (currentUser != null) {
        // 已登录用户：可以看到所有状态的商品（包括自己的未上架商品）
        Long userId = currentUser.getUserId();
        String role = currentUser.getRole();
        return Result.ok(productService.queryAllProducts(request, userId, role));
    } else {
        // 匿名用户：只能看到已上架的商品
        return Result.ok(productService.queryListedProducts(request));
    }
}
```

**行为**：
- ✅ 用户已登录 → 注入用户信息
- ✅ 用户未登录 → `currentUser` 为 `null`，不会抛出异常

---

## 🔧 UserPrincipal 对象

### 字段说明
```java
public class UserPrincipal {
    private Long userId;      // 用户ID
    private String username;  // 用户名
    private String role;      // 用户角色（USER, MERCHANT, ADMIN）
}
```

### 便捷方法
```java
// 判断角色
boolean isAdmin = currentUser.isAdmin();      // 是否为管理员
boolean isMerchant = currentUser.isMerchant(); // 是否为商家
boolean isUser = currentUser.isUser();         // 是否为普通用户

// 获取字段
Long userId = currentUser.getUserId();
String username = currentUser.getUsername();
String role = currentUser.getRole();
```

---

## 📋 使用示例

### 示例1：创建商品（商家专属）

```java
@PostMapping
public Result<ProductResponse> create(
    @Valid @RequestBody CreateProductRequest request,
    @CurrentUser UserPrincipal currentUser) {
    
    // 1. 权限验证
    if (!currentUser.isMerchant() && !currentUser.isAdmin()) {
        return Result.fail(403, "只有商家可以创建商品");
    }
    
    // 2. 业务逻辑
    ProductResponse response = productService.create(request, currentUser.getUserId());
    
    log.info("商家创建商品: merchantId={}, product={}", currentUser.getUserId(), request.getName());
    return Result.ok(response);
}
```

### 示例2：更新商品（只能修改自己的）

```java
@PutMapping("/{id}")
public Result<ProductResponse> update(
    @PathVariable Long id,
    @Valid @RequestBody UpdateProductRequest request,
    @CurrentUser UserPrincipal currentUser) {
    
    // 业务逻辑中会验证所有权
    ProductResponse response = productService.update(id, request, currentUser.getUserId());
    
    log.info("用户更新商品: userId={}, productId={}", currentUser.getUserId(), id);
    return Result.ok(response);
}
```

### 示例3：查询商品详情（公开接口）

```java
@GetMapping("/{id}")
public Result<ProductResponse> getById(@PathVariable Long id) {
    // 不需要 @CurrentUser，因为这是公开接口
    ProductResponse response = productService.getById(id);
    return Result.ok(response);
}
```

### 示例4：查询商品列表（根据角色过滤）

```java
@GetMapping
public Result<List<ProductResponse>> queryProducts(
    ProductQueryRequest request,
    @CurrentUser(required = false) UserPrincipal currentUser) {
    
    Long userId = currentUser != null ? currentUser.getUserId() : null;
    String role = currentUser != null ? currentUser.getRole() : "USER";
    
    List<ProductResponse> responses = productService.queryProducts(request, userId, role);
    return Result.ok(responses);
}
```

---

## ⚠️ 注意事项

### 1. 参数类型必须为 UserPrincipal
```java
// ❌ 错误：类型不匹配
public Result<Void> method(@CurrentUser String userId) { }

// ✅ 正确：使用 UserPrincipal
public Result<Void> method(@CurrentUser UserPrincipal currentUser) {
    Long userId = currentUser.getUserId();
}
```

### 2. 不要与 @RequestHeader 同时使用
```java
// ❌ 错误：不要混合使用
public Result<Void> method(
    @CurrentUser UserPrincipal currentUser,
    @RequestHeader("X-User-Id") Long userId) { }

// ✅ 正确：只使用 @CurrentUser
public Result<Void> method(@CurrentUser UserPrincipal currentUser) {
    Long userId = currentUser.getUserId();
}
```

### 3. required=false 时要检查 null
```java
@GetMapping
public Result<List<ProductResponse>> query(
    @CurrentUser(required = false) UserPrincipal currentUser) {
    
    // ✅ 正确：需要检查 null
    if (currentUser != null) {
        // 已登录用户的逻辑
    }
    
    // ❌ 错误：直接使用可能抛出 NullPointerException
    Long userId = currentUser.getUserId(); // 可能报错
}
```

### 4. 权限验证建议
```java
@PostMapping
public Result<ProductResponse> create(
    @Valid @RequestBody CreateProductRequest request,
    @CurrentUser UserPrincipal currentUser) {
    
    // ✅ 推荐：使用便捷方法
    if (!currentUser.isMerchant() && !currentUser.isAdmin()) {
        return Result.fail(403, "权限不足");
    }
    
    // ❌ 不推荐：直接比较字符串
    if (!"MERCHANT".equals(currentUser.getRole())) {
        return Result.fail(403, "权限不足");
    }
}
```

---

## 🔒 安全特性

### 1. 防伪造
`UserPrincipal` 从 JWT Token 解析而来，无法通过 HTTP 请求伪造。

### 2. 自动验证
- Token 过期 → 自动返回 401
- Token 无效 → 自动返回 401
- 未登录 → 抛出异常（required=true 时）

### 3. 线程安全
使用 `ThreadLocal` 存储用户信息，确保多线程环境下的安全性。

---

## 🆚 与传统方式对比

### 传统方式（不安全）
```java
@PostMapping
public Result<ProductResponse> create(
    @RequestBody CreateProductRequest request,
    @RequestHeader(value = "X-User-Id") Long userId,        // ❌ 可伪造
    @RequestHeader(value = "X-User-Role") String userRole) { // ❌ 可伪造
    
    return Result.ok(productService.create(request, userId));
}
```

**问题**：
- 任何人都可以伪造 HTTP 头部
- 不安全，不符合规范

### 新方式（安全）
```java
@PostMapping
public Result<ProductResponse> create(
    @RequestBody CreateProductRequest request,
    @CurrentUser UserPrincipal currentUser) {  // ✅ 从 JWT 解析，无法伪造
    
    return Result.ok(productService.create(request, currentUser.getUserId()));
}
```

**优势**：
- 安全可靠，无法伪造
- 代码简洁，易于维护
- 符合 RESTful 规范

---

## 📊 总结

| 特性 | 说明 |
|------|------|
| **安全性** | ✅ 从 JWT Token 解析，无法伪造 |
| **简洁性** | ✅ 自动注入，无需手动传递参数 |
| **灵活性** | ✅ 支持 required 控制是否必须登录 |
| **类型安全** | ✅ 强类型 UserPrincipal，编译时检查 |
| **可维护性** | ✅ 集中管理，易于扩展 |

---

## 🎓 最佳实践

1. **需要认证的接口**：使用 `@CurrentUser`（默认 required=true）
2. **公开接口**：不使用 `@CurrentUser`
3. **半公开接口**：使用 `@CurrentUser(required = false)` 并检查 null
4. **权限判断**：使用 `currentUser.isXxx()` 方法而不是字符串比较
5. **日志记录**：使用 `currentUser.getUserId()` 而不是从请求参数获取

使用 `@CurrentUser` 注解可以让您的代码更安全、更简洁、更易维护！