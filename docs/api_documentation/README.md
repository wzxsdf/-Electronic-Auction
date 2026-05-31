# 拍卖系统前端API接口文档

## 📋 文档说明

本文档专门为前端开发者编写，包含所有API接口的详细说明、请求示例、响应格式和注意事项。

**🔗 API基础地址**: `http://localhost:8080/api`

**📋 版本信息**: v1.0.0

**📅 更新日期**: 2026-05-31

---

## 🔐 认证说明

### Token获取

所有需要认证的接口都需要在请求头中携带JWT Token：

```javascript
headers: {
  'Authorization': 'Bearer {access_token}',
  'X-User-Id': '{user_id}'
}
```

### Token类型

- **Access Token**: 短期有效令牌（2小时），用于API访问
- **Refresh Token**: 长期有效令牌（7天），用于刷新Access Token

---

## 📊 通用响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // 具体数据内容
  },
  "timestamp": 1717056000000
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "请求参数错误",
  "timestamp": 1717056000000
}
```

### 通用错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 200 | 成功 | 正常处理数据 |
| 400 | 请求参数错误 | 检查请求参数格式和内容 |
| 401 | 未登录 | 跳转到登录页面，重新获取Token |
| 403 | 无权限 | 提示用户权限不足 |
| 404 | 资源不存在 | 提示用户资源不存在 |
| 500 | 服务器内部错误 | 提示用户稍后重试 |

---

## 📚 模块文档目录

### 1. [用户认证模块](./01_用户认证模块API.md)
- 用户注册
- 用户登录
- Token刷新
- 用户登出
- 获取当前用户信息

### 2. [用户管理模块](./02_用户管理模块API.md)
- 查询用户信息
- 更新用户信息
- 修改密码
- 用户列表查询

### 3. [商品管理模块](./03_商品管理模块API.md)
- 创建商品
- 查询商品详情
- 查询商品列表
- 商品搜索

### 4. [拍卖管理模块](./04_拍卖管理模块API.md)
- 创建拍卖
- 查询拍卖详情
- 开始拍卖
- 取消拍卖
- 查询活跃拍卖

### 5. [出价管理模块](./05_出价管理模块API.md)
- 发起出价
- 查询出价历史
- 出价排行榜
- 出价统计

### 6. [订单管理模块](./06_订单管理模块API.md)
- 查询订单详情
- 查询用户订单
- 订单状态管理
- 订单统计

### 7. [支付管理模块](./07_支付管理模块API.md)
- 发起支付
- 查询支付状态
- 取消支付
- 支付回调处理

---

## 🔧 开发环境配置

### 本地开发环境

```bash
# 启动后端服务
mvn spring-boot:run

# 访问Swagger文档
http://localhost:8080/swagger-ui.html

# 访问健康检查
http://localhost:8080/actuator/health
```

### 测试账号

```javascript
// 测试用户账号
const testUsers = [
  {
    username: 'testuser',
    password: 'Test123456',
    role: 'USER'
  },
  {
    username: 'admin',
    password: 'Admin123456',
    role: 'ADMIN'
  }
];
```

---

## 📝 接口调用示例

### Fetch API示例

```javascript
// 1. 用户登录获取Token
async function login(username, password) {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  });
  
  const result = await response.json();
  
  if (result.code === 200) {
    // 保存Token
    localStorage.setItem('accessToken', result.data.accessToken);
    localStorage.setItem('refreshToken', result.data.refreshToken);
    localStorage.setItem('userId', result.data.userId);
    
    return result.data;
  }
  
  throw new Error(result.message);
}

// 2. 使用Token访问受保护接口
async function getProfile() {
  const token = localStorage.getItem('accessToken');
  
  const response = await fetch('http://localhost:8080/api/auth/me', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-User-Id': localStorage.getItem('userId')
    }
  });
  
  const result = await response.json();
  
  if (result.code === 200) {
    return result.data;
  }
  
  // Token过期，尝试刷新
  if (result.code === 401) {
    return await refreshToken();
  }
  
  throw new Error(result.message);
}

// 3. Token刷新
async function refreshToken() {
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
  
  // 刷新失败，跳转到登录页
  localStorage.clear();
  window.location.href = '/login';
}
```

### Axios示例

```javascript
import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
      config.headers['X-User-Id'] = localStorage.getItem('userId');
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  response => {
    const result = response.data;
    if (result.code === 200) {
      return result.data;
    }
    return Promise.reject(new Error(result.message));
  },
  async error => {
    if (error.response?.status === 401) {
      // Token过期，尝试刷新
      try {
        const newToken = await refreshToken();
        // 重新发送原请求
        error.config.headers['Authorization'] = `Bearer ${newToken}`;
        return api.request(error.config);
      } catch (refreshError) {
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

// 使用示例
async function getUserInfo() {
  try {
    const userInfo = await api.get('/auth/me');
    console.log('用户信息:', userInfo);
    return userInfo;
  } catch (error) {
    console.error('获取用户信息失败:', error.message);
  }
}
```

---

## ⚠️ 注意事项

### 1. 跨域问题

如果遇到跨域问题，请确保后端已配置CORS：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### 2. Token刷新策略

- Access Token有效期为2小时
- 建议在Token即将过期前30分钟自动刷新
- 刷新Token后需要更新本地存储的Token

### 3. 错误处理

- 所有接口都可能返回错误，请做好错误处理
- 网络错误请提供友好的用户提示
- 权限错误请引导用户重新登录

### 4. 数据验证

- 前端需要验证必填字段
- 前端需要验证数据格式（如手机号、邮箱等）
- 不要完全依赖后端验证，提升用户体验

### 5. 文件上传

- 文件上传使用`multipart/form-data`格式
- 图片文件限制为5MB以内
- 支持的格式：JPG、PNG、GIF

---

## 🔄 WebSocket连接

### WebSocket地址

```
ws://localhost:8080/ws/auction/{auctionId}?token={access_token}
```

### 消息格式

```javascript
// 连接WebSocket
const ws = new WebSocket('ws://localhost:8080/ws/auction/1?token=xxx');

// 监听消息
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  switch(message.type) {
    case 'NEW_BID':
      // 新出价通知
      console.log('新出价:', message.data);
      break;
    case 'YOU_WERE_OVERTAKEN':
      // 被超越通知
      console.log('您已被超越');
      showNotification('您已被超越！');
      break;
    case 'AUCTION_COMPLETED':
      // 拍卖结束通知
      console.log('拍卖已结束');
      break;
    default:
      console.log('未知消息类型:', message.type);
  }
};

// 发送心跳
setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'PING' }));
  }
}, 30000);
```

---

## 📞 技术支持

如有接口问题，请联系：

- **后端开发**: 通过项目Issue或内部沟通群
- **接口变更**: 会提前在沟通群通知
- **紧急问题**: 直接联系后端负责人

---

## 📅 更新记录

| 日期 | 版本 | 更新内容 | 更新人 |
|------|------|----------|--------|
| 2026-05-31 | v1.0.0 | 初始版本，包含所有核心接口 | 后端团队 |

---

**祝前端开发顺利！🎉**
