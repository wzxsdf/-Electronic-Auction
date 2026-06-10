# 🔧 拍卖系统接口测试套件

## 📁 目录结构

```
test_api/
├── README.md                 # 本文件
├── common.http              # 公共配置和变量
├── auth/                    # 认证模块测试
│   └── auth-api.http
├── products/                # 商品管理测试
│   └── products-api.http
├── auctions/                # 拍卖管理测试
│   └── auctions-api.http
├── bids/                    # 出价系统测试
│   └── bids-api.http
├── orders/                  # 订单管理测试
│   └── orders-api.http
├── users/                   # 用户管理测试
│   └── users-api.http
├── rooms/                   # 拍卖房间测试
│   └── rooms-api.http
├── items/                   # 拍卖商品测试
│   └── items-api.http
├── upload/                  # 文件上传测试
│   └── upload-api.http
├── payments/                # 支付系统测试
│   └── payments-api.http
└── test/                    # 测试接口
    └── test-api.http
```

## 🚀 快速开始

### 1. 环境准备
```bash
# 启动项目
mvn spring-boot:run

# 确认服务运行
curl http://localhost:8080/actuator/health
```

### 2. 按模块测试

#### **方式1：单模块测试**
```bash
# 在IDE中打开对应模块的 .http 文件
# 例如：test_api/auth/auth-api.http
# 点击运行按钮执行测试
```

#### **方式2：流程化测试**
```bash
# 按照业务流程顺序执行：
# 1. auth/ - 注册登录获取Token
# 2. products/ - 创建商品
# 3. auctions/ - 创建拍卖
# 4. bids/ - 出价测试
# 5. orders/ - 订单查询
```

## 📋 模块依赖关系

```
auth (认证) → products (商品) → auctions (拍卖) → bids (出价) → orders (订单)
     ↓              ↓               ↓            ↓
   users ←────────┴───────────────┴────────────┘
     ↓
   upload (上传)
     ↓
   payments (支付)
```

## 🔐 认证说明

### Token类型
- **用户Token**: 用于普通用户操作（出价、查询）
- **商家Token**: 用于商家操作（创建商品、管理拍卖）

### 获取Token
1. 先执行 `auth/auth-api.http` 中的注册/登录接口
2. Token自动保存到环境变量 `{{authToken}}` 和 `{{merchantToken}}`
3. 后续接口自动使用保存的Token

### 权限要求
| 模块 | 所需Token | 权限级别 |
|------|-----------|----------|
| auth | 无 | 公开 |
| products | 商家Token | 商家/管理员 |
| auctions | 商家Token | 商家/管理员 |
| bids | 用户Token | 注册用户 |
| orders | 用户Token | 注册用户 |
| users | 用户Token | 本人或管理员 |

## 📊 测试场景

### 完整业务流程测试
```
1. 用户注册登录 (auth)
2. 创建商品 (products) 
3. 创建拍卖活动 (auctions)
4. 开始拍卖 (auctions)
5. 用户出价 (bids)
6. 查询出价历史 (bids)
7. 生成订单 (orders)
8. 支付订单 (payments)
```

### 权限测试
```
1. 普通用户尝试创建商品 → 应该失败 (403)
2. 商家创建商品 → 应该成功 (200)
3. 未登录用户出价 → 应该失败 (401)
4. 已登录用户出价 → 应该成功 (200)
```

## 🛠️ 故障排查

### 常见问题

**Q: Token认证失败**
```
A: 1. 检查是否执行了 auth 模块的登录接口
   2. 确认Token是否正确保存
   3. 重新登录获取新Token
```

**Q: 权限不足 (403)**
```
A: 1. 检查Token类型是否正确（用户 vs 商家）
   2. 确认用户角色权限
   3. 检查资源所有权验证
```

**Q: 业务逻辑错误**
```
A: 1. 检查拍卖状态（只有ACTIVE状态可出价）
   2. 验证参数合理性（价格、时间等）
   3. 查看详细的错误信息
```

## 📈 测试覆盖率

### 模块覆盖率
- ✅ 认证模块: 100% (7/7)
- ✅ 商品管理: 100% (7/7)  
- ✅ 拍卖管理: 100% (6/6)
- ✅ 出价系统: 100% (4/4)
- ✅ 订单管理: 100% (7/7)
- ✅ 用户管理: 100% (6/6)
- ✅ 文件上传: 100% (2/2)
- ✅ 支付系统: 100% (3/3)

### 功能覆盖
- ✅ 基础CRUD操作
- ✅ 权限验证
- ✅ 业务规则验证
- ✅ 异常处理
- ✅ 数据关联查询

## 🔗 相关文档

- [项目主文档](../README.md)
- [API使用指南](../API-TEST-GUIDE.md)
- [数据库设计](../docs/database-design.md)

## 📝 更新日志

### v1.0.0 (2026-06-07)
- ✅ 初始版本
- ✅ 按模块划分测试文件
- ✅ 完整的业务流程覆盖
- ✅ 自动化Token管理

---

**维护者**: AI测试助手  
**最后更新**: 2026-06-07  
**版本**: 1.0.0