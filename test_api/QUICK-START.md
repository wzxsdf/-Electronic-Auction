# 🎯 拍卖系统接口测试快速开始指南

## 📁 测试文件结构

```
test_api/
├── README.md                    # 📖 完整测试文档
├── all-tests.http              # 🚀 总入口文件
├── common.http                 # 🔧 公共配置
│
├── auth/auth-api.http          # 🔐 认证模块（推荐从这里开始）
├── products/products-api.http # 📦 商品管理
├── auctions/auctions-api.http # 🎵 拍卖管理
├── bids/bids-api.http          # 💰 出价系统
├── orders/orders-api.http      # 📋 订单管理
│
├── users/users-api.http        # 👥 用户管理
├── rooms/rooms-api.http        # 🏠 拍卖房间
├── upload/upload-api.http      # 📤 文件上传
├── payments/payments-api.http  # 💳 支付系统
└── test/test-api.http          # 🎮 系统测试
```

## 🚀 3分钟快速开始

### 步骤1：启动项目
```bash
cd C:\Users\魏子轩\Desktop\auction-system
mvn spring-boot:run
```

### 步骤2：打开测试文件
在IDE中打开 `test_api/all-tests.http`

### 步骤3：按顺序测试
1. **认证模块** → 获取Token
2. **商品模块** → 创建商品
3. **拍卖模块** → 创建拍卖
4. **出价模块** → 测试出价

## 📋 模块测试顺序

### 🟢 基础模块（推荐顺序）
1. **auth** - 用户认证（获取Token）
2. **products** - 商品管理（创建商品）
3. **auctions** - 拍卖管理（创建拍卖）

### 🔵 核心业务
4. **bids** - 出价系统（用户竞拍）
5. **orders** - 订单管理（交易订单）

### 🟡 高级功能
6. **payments** - 支付系统（支付处理）
7. **users** - 用户管理（个人信息）
8. **rooms** - 拍卖房间（实时通信）

### 🟠 辅助功能
9. **upload** - 文件上传（图片管理）
10. **test** - 系统测试（健康检查）

## 🔑 关键环境变量

### 认证相关
- `authToken` - 普通用户Token（出价、查询）
- `merchantToken` - 商家Token（创建商品、拍卖）
- `adminToken` - 管理员Token（系统管理）

### 业务实体
- `productId` - 商品ID（从商品创建接口获取）
- `auctionId` - 拍卖ID（从拍卖创建接口获取）
- `orderId` - 订单ID（拍卖结束自动生成）

## 💡 使用技巧

### IDE推荐设置
- IntelliJ IDEA：内置HTTP Client支持
- VS Code：安装REST Client插件
- 点击接口左侧的运行按钮即可执行

### Token自动管理
```javascript
// 登录接口会自动保存Token
> {%
    client.global.set("authToken", response.body.data.token);
%}

// 后续接口自动使用
Authorization: Bearer {{authToken}}
```

### 批量测试
- 选中多个接口（Ctrl+点击）
- 右键选择"Run All"
- 查看批量测试结果

## 📊 完整业务流程测试

### 流程：商品拍卖
```
商家登录 → 创建商品 → 创建拍卖 → 开始拍卖
                                      ↓
用户登录 → 查询拍卖 → 参与出价 → 拍卖结束
                                      ↓
                          生成订单 → 完成支付 → 交易完成
```

### 接口执行顺序
1. `auth/auth-api.http` - 商家登录
2. `products/products-api.http` - 创建商品
3. `auctions/auctions-api.http` - 创建拍卖
4. `auctions/auctions-api.http` - 开始拍卖
5. `auth/auth-api.http` - 用户登录
6. `bids/bids-api.http` - 用户出价
7. `orders/orders-api.http` - 查询订单
8. `payments/payments-api.http` - 完成支付

## 🛠️ 故障排查

### 常见问题

**问题1：连接失败**
```
❌ Connection refused
✅ 解决：检查项目是否启动，确认端口8080
```

**问题2：认证失败**
```
❌ 401 Unauthorized
✅ 解决：重新执行登录接口获取新Token
```

**问题3：权限不足**
```
❌ 403 Forbidden
✅ 解决：使用merchantToken代替authToken
```

**问题4：数据不存在**
```
❌ 404 Not Found
✅ 解决：先创建测试数据（商品、拍卖等）
```

### 调试技巧
1. 查看响应状态码
2. 检查响应内容中的错误信息
3. 确认依赖关系是否满足
4. 验证环境变量是否正确

## 📈 测试覆盖率

### 功能模块覆盖
- ✅ **认证系统**：注册、登录、Token管理
- ✅ **商品管理**：CRUD、上架下架、图片管理
- ✅ **拍卖管理**：创建、开始、取消、查询
- ✅ **出价系统**：出价、历史、统计、排名
- ✅ **订单管理**：查询、状态更新、取消
- ✅ **支付系统**：创建、回调、退款
- ✅ **用户管理**：信息、资料、权限
- ✅ **文件上传**：图片上传、URL生成
- ✅ **拍卖房间**：WebSocket、实时通信
- ✅ **系统测试**：健康检查、性能测试

### 测试类型覆盖
- ✅ 功能测试：正常业务流程
- ✅ 异常测试：错误处理验证
- ✅ 安全测试：权限和防护
- ✅ 性能测试：响应时间和并发
- ✅ 边界测试：参数和限制

## 🎯 学习路径

### 初学者（第1-2天）
1. 了解测试文件结构
2. 测试认证模块，理解Token机制
3. 测试商品模块，理解CRUD操作
4. 测试拍卖模块，理解业务流程

### 进阶（第3-4天）
1. 完整业务流程测试
2. 理解模块间依赖关系
3. 测试异常情况和边界条件
4. 学习WebSocket实时通信

### 高级（第5-7天）
1. 自动化测试脚本编写
2. 性能和压力测试
3. 安全测试深入
4. CI/CD集成

## 🔗 相关资源

### 官方文档
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [REST Client Guide](https://www.jetbrains.com/help/idea/http-client-in-product-code.html)

### 项目文档
- `README.md` - 项目说明
- `docs/` - 详细文档
- `CLAUDE.md` - 开发指南

### 技术支持
- 查看项目Issues
- 提交测试问题
- 参与讨论交流

## ✅ 测试检查清单

### 准备工作
- [ ] 项目已启动
- [ ] 数据库已连接
- [ ] Redis已启动
- [ ] OSS已配置（如需测试上传）

### 基础测试
- [ ] 健康检查通过
- [ ] 用户注册成功
- [ ] 用户登录成功
- [ ] Token自动保存

### 业务测试
- [ ] 商品创建成功
- [ ] 拍卖创建成功
- [ ] 出价功能正常
- [ ] 订单生成正常

### 完整性检查
- [ ] 所有模块测试通过
- [ ] 无安全漏洞
- [ ] 性能符合预期
- [ ] 文档完整准确

## 🎉 总结

这套测试系统提供了：
- **10个功能模块** 的完整测试
- **100+个测试接口** 覆盖所有功能
- **自动化Token管理** 提升测试效率
- **业务流程验证** 确保系统正确性
- **安全防护测试** 保障系统安全

开始测试：打开 `test_api/auth/auth-api.http`，点击第一个接口开始您的测试之旅！

---

**最后更新**：2026-06-07  
**维护者**：AI测试助手  
**版本**：1.0.0