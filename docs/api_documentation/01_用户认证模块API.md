# 用户认证模块API文档

## 📋 模块说明

用户认证模块负责用户的注册、登录、Token管理和用户信息查询。

**🔗 基础路径**: `/api/auth`

---

## 🔓 1. 用户注册

### 接口信息

- **接口地址**: `POST /api/auth/register`
- **接口说明**: 用户注册新账号
- **权限要求**: 无需登录
- **限流规则**: 5次/小时

### 请求参数

```json
{
  "username": "testuser",
  "password": "Test123456",
  "nickname": "测试用户",
  "email": "test@example.com",
  "phone": "13800138000"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 | 验证规则 |
|--------|------|------|------|----------|
| username | String | ✅ | 用户名 | 4-20位字母、数字或下划线 |
| password | String | ✅ | 密码 | 8-20位，包含大小写字母和数字 |
| nickname | String | ✅ | 昵称 | 2-20位 |
| email | String | ❌ | 邮箱 | 有效的邮箱格式 |
| phone | String | ❌ | 手机号 | 11位手机号，1开头 |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "email": "test@example.com",
    "phone": "13800138000",
    "status": "ACTIVE",
    "statusDesc": "正常",
    "totalBids": 0,
    "totalWins": 0,
    "roles": ["USER"],
    "createdAt": "2026-05-31T10:00:00",
    "updatedAt": "2026-05-31T10:00:00"
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 3101,
  "message": "用户名已存在",
  "timestamp": 1717056000000
}
```

### 错误码说明

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 3101 | 用户名已存在 | 提示用户换一个用户名 |
| 3102 | 邮箱已存在 | 提示用户换一个邮箱 |
| 3103 | 手机号已存在 | 提示用户换一个手机号 |
| 3107 | 密码强度不足 | 提示用户使用更复杂的密码 |

### 前端调用示例

```javascript
async function register(formData) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(formData)
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('注册成功:', result.data);
      // 跳转到登录页面
      window.location.href = '/login';
    } else {
      console.error('注册失败:', result.message);
      // 显示错误提示
      showError(result.message);
    }
  } catch (error) {
    console.error('注册请求失败:', error);
    showError('网络错误，请稍后重试');
  }
}
```

---

## 🔐 2. 用户登录

### 接口信息

- **接口地址**: `POST /api/auth/login`
- **接口说明**: 用户登录获取Token
- **权限要求**: 无需登录
- **限流规则**: 10次/分钟

### 请求参数

```json
{
  "username": "testuser",
  "password": "Test123456"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | ✅ | 用户名 |
| password | String | ✅ | 密码 |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "avatarUrl": "https://example.com/avatar.jpg",
    "roles": ["USER"]
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 3104,
  "message": "用户名或密码错误",
  "timestamp": 1717056000000
}
```

### 错误码说明

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 3104 | 用户名或密码错误 | 提示用户检查账号密码 |
| 3105 | 账户已被禁用 | 提示用户联系管理员 |
| 3106 | 账户已被锁定 | 提示用户稍后重试或联系管理员 |

### Token使用说明

```javascript
// 登录成功后保存Token
localStorage.setItem('accessToken', result.data.accessToken);
localStorage.setItem('refreshToken', result.data.refreshToken);
localStorage.setItem('userId', result.data.userId);
localStorage.setItem('userInfo', JSON.stringify(result.data));

// 设置axios默认headers
axios.defaults.headers.common['Authorization'] = `Bearer ${result.data.accessToken}`;
axios.defaults.headers.common['X-User-Id'] = result.data.userId;
```

### 前端调用示例

```javascript
async function login(username, password) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ username, password })
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      // 保存Token和用户信息
      const { accessToken, refreshToken, userId, ...userData } = result.data;
      
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('userId', userId);
      localStorage.setItem('userInfo', JSON.stringify(userData));
      
      // 设置axios默认headers
      axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
      axios.defaults.headers.common['X-User-Id'] = userId;
      
      console.log('登录成功:', userData);
      // 跳转到首页
      window.location.href = '/dashboard';
    } else {
      console.error('登录失败:', result.message);
      showError(result.message);
    }
  } catch (error) {
    console.error('登录请求失败:', error);
    showError('网络错误，请稍后重试');
  }
}
```

---

## 🔄 3. 刷新Token

### 接口信息

- **接口地址**: `POST /api/auth/refresh`
- **接口说明**: 使用Refresh Token获取新的Access Token
- **权限要求**: 需要Refresh Token

### 请求头

```
Authorization: Bearer {refresh_token}
```

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 3203,
  "message": "Token刷新失败",
  "timestamp": 1717056000000
}
```

### Token刷新策略

```javascript
// 在axios响应拦截器中自动刷新Token
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // Token过期，尝试刷新
      const refreshToken = localStorage.getItem('refreshToken');
      
      try {
        const response = await axios.post('/api/auth/refresh', {}, {
          headers: {
            'Authorization': `Bearer ${refreshToken}`
          }
        });
        
        const newAccessToken = response.data.data;
        
        // 更新本地Token
        localStorage.setItem('accessToken', newAccessToken);
        axios.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`;
        
        // 重新发送原请求
        error.config.headers['Authorization'] = `Bearer ${newAccessToken}`;
        return axios.request(error.config);
        
      } catch (refreshError) {
        // 刷新失败，清除本地数据并跳转登录页
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);
```

---

## 🚪 4. 用户登出

### 接口信息

- **接口地址**: `POST /api/auth/logout`
- **接口说明**: 用户登出，使Token失效
- **权限要求**: 需要登录

### 请求头

```
Authorization: Bearer {access_token}
X-User-Id: {user_id}
```

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function logout() {
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch('http://localhost:8080/api/auth/logout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      }
    });
    
    const result = await response.json();
    
    // 无论登出是否成功，都清除本地数据
    localStorage.clear();
    
    console.log('登出成功');
    window.location.href = '/login';
    
  } catch (error) {
    console.error('登出请求失败:', error);
    // 即使请求失败，也清除本地数据
    localStorage.clear();
    window.location.href = '/login';
  }
}
```

---

## 👤 5. 获取当前用户信息

### 接口信息

- **接口地址**: `GET /api/auth/me`
- **接口说明**: 获取当前登录用户的详细信息
- **权限要求**: 需要登录

### 请求头

```
Authorization: Bearer {access_token}
X-User-Id: {user_id}
```

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "avatarUrl": "https://example.com/avatar.jpg",
    "email": "test@example.com",
    "phone": "13800138000",
    "status": "ACTIVE",
    "statusDesc": "正常",
    "totalBids": 10,
    "totalWins": 2,
    "roles": ["USER"],
    "createdAt": "2026-05-31T10:00:00",
    "updatedAt": "2026-05-31T10:00:00"
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 401,
  "message": "未登录",
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function getCurrentUser() {
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch('http://localhost:8080/api/auth/me', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      }
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('用户信息:', result.data);
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
    // Token可能过期，尝试刷新或跳转登录
    window.location.href = '/login';
  }
}

// 在页面加载时获取用户信息
useEffect(() => {
  getCurrentUser().then(userInfo => {
    setCurrentUser(userInfo);
  });
}, []);
```

---

## 📝 完整示例代码

### 认证工具类

```javascript
// auth.js
class AuthManager {
  constructor() {
    this.tokenRefreshPromise = null;
  }
  
  // 登录
  async login(username, password) {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      this.saveAuthData(result.data);
      return result.data;
    }
    
    throw new Error(result.message);
  }
  
  // 注册
  async register(formData) {
    const response = await fetch('http://localhost:8080/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      return result.data;
    }
    
    throw new Error(result.message);
  }
  
  // 登出
  async logout() {
    try {
      const token = localStorage.getItem('accessToken');
      const userId = localStorage.getItem('userId');
      
      await fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': userId
        }
      });
    } catch (error) {
      console.error('登出请求失败:', error);
    } finally {
      this.clearAuthData();
      window.location.href = '/login';
    }
  }
  
  // 获取当前用户信息
  async getCurrentUser() {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch('http://localhost:8080/api/auth/me', {
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      }
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      return result.data;
    }
    
    throw new Error(result.message);
  }
  
  // 刷新Token
  async refreshToken() {
    // 防止并发刷新
    if (this.tokenRefreshPromise) {
      return this.tokenRefreshPromise;
    }
    
    this.tokenRefreshPromise = this._doRefreshToken();
    
    try {
      const newToken = await this.tokenRefreshPromise;
      return newToken;
    } finally {
      this.tokenRefreshPromise = null;
    }
  }
  
  async _doRefreshToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    
    const response = await fetch('http://localhost:8080/api/auth/refresh', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${refreshToken}`
      }
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      localStorage.setItem('accessToken', result.data);
      return result.data;
    }
    
    // 刷新失败，清除数据并跳转登录
    this.clearAuthData();
    window.location.href = '/login';
    throw new Error('Token刷新失败');
  }
  
  // 保存认证数据
  saveAuthData(authData) {
    const { accessToken, refreshToken, userId, ...userData } = authData;
    
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('userId', userId);
    localStorage.setItem('userInfo', JSON.stringify(userData));
  }
  
  // 清除认证数据
  clearAuthData() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('userInfo');
  }
  
  // 检查是否已登录
  isAuthenticated() {
    return !!localStorage.getItem('accessToken');
  }
  
  // 获取Token
  getToken() {
    return localStorage.getItem('accessToken');
  }
  
  // 获取用户ID
  getUserId() {
    return localStorage.getItem('userId');
  }
  
  // 获取用户信息
  getUserInfo() {
    const userInfo = localStorage.getItem('userInfo');
    return userInfo ? JSON.parse(userInfo) : null;
  }
}

// 导出单例
export const authManager = new AuthManager();
```

### React Hook示例

```javascript
// useAuth.js
import { useState, useEffect } from 'react';
import { authManager } from './auth';

export function useAuth() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    if (authManager.isAuthenticated()) {
      authManager.getCurrentUser()
        .then(userData => {
          setUser(userData);
          setLoading(false);
        })
        .catch(error => {
          console.error('获取用户信息失败:', error);
          setLoading(false);
          window.location.href = '/login';
        });
    } else {
      setLoading(false);
    }
  }, []);
  
  const login = async (username, password) => {
    try {
      const userData = await authManager.login(username, password);
      setUser(userData);
      return true;
    } catch (error) {
      console.error('登录失败:', error);
      throw error;
    }
  };
  
  const logout = async () => {
    await authManager.logout();
    setUser(null);
  };
  
  return {
    user,
    loading,
    isAuthenticated: !!user,
    login,
    logout
  };
}
```

---

## ⚠️ 注意事项

### 1. Token管理

- Access Token有效期为2小时，建议在即将过期前30分钟自动刷新
- Refresh Token有效期为7天，过期后需要重新登录
- 建议在前端实现Token自动刷新机制

### 2. 安全性

- 密码必须包含大小写字母和数字
- 前端需要对密码进行基本验证
- Token应该保存在localStorage中，但要注意安全性

### 3. 错误处理

- 401错误应该跳转到登录页面
- 403错误应该提示用户权限不足
- 网络错误应该提供友好的用户提示

### 4. 用户体验

- 登录失败5次后账户会被锁定30分钟
- 注册接口有频率限制，5次/小时
- 建议在登录页面提供"忘记密码"功能

---

## 📞 技术支持

如有接口问题，请联系后端开发团队。

**最后更新**: 2026-05-31
**文档版本**: v1.0.0
