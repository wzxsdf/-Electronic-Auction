# 订单管理模块API文档

## 📋 模块说明

订单管理模块负责拍卖成交后的订单管理，包括订单查询、状态管理和统计功能。

**🔗 基础路径**: `/api/orders`

---

## 🔍 1. 查询订单详情

### 接口信息

- **接口地址**: `GET /api/orders/{orderId}`
- **接口说明**: 根据订单ID查询订单详细信息
- **权限要求**: 需要登录，必须是订单所有者或管理员

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderId | Long | ✅ | 订单ID |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "orderNo": "AU20260531103025123456",
    "userId": 123,
    "productId": 1,
    "productName": "iPhone 15 Pro Max",
    "productImageUrl": "https://example.com/iphone.jpg",
    "itemId": 1,
    "auctionTitle": "iPhone 15 Pro Max 拍卖",
    "finalAmount": 5000.00,
    "status": "PENDING_PAYMENT",
    "statusDesc": "待支付",
    "createdAt": "2026-05-31T10:30:25",
    "paidTime": null,
    "completedTime": null,
    "cancelledTime": null
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 404,
  "message": "订单不存在",
  "timestamp": 1717056000000
}
```

### 订单状态说明

| 状态 | 说明 | 可执行操作 |
|------|------|------------|
| PENDING_PAYMENT | 待支付 | 支付、取消 |
| PAID | 已支付 | 确认收货 |
| COMPLETED | 已完成 | 无 |
| CANCELLED | 已取消 | 无 |

### 前端调用示例

```javascript
async function getOrderDetail(orderId) {
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch(`http://localhost:8080/api/orders/${orderId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      }
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('订单详情:', result.data);
      return result.data;
    } else if (result.code === 404) {
      throw new Error('订单不存在');
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('查询订单失败:', error);
    throw error;
  }
}

// 订单详情组件
function OrderDetail({ orderId }) {
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    getOrderDetail(orderId).then(data => {
      setOrder(data);
      setLoading(false);
    }).catch(error => {
      showError('加载订单信息失败');
      setLoading(false);
    });
  }, [orderId]);
  
  if (loading) return <div>加载中...</div>;
  if (!order) return <ErrorPage message="订单不存在" />;
  
  const canCancel = order.status === 'PENDING_PAYMENT';
  const canPay = order.status === 'PENDING_PAYMENT';
  
  return (
    <div className="order-detail">
      <div className="order-header">
        <h1>订单详情</h1>
        <span className={`order-status status-${order.status.toLowerCase()}`}>
          {order.statusDesc}
        </span>
      </div>
      
      {/* 商品信息 */}
      <div className="product-info">
        <img src={order.productImageUrl} alt={order.productName} />
        <div>
          <h3>{order.productName}</h3>
          <p>拍卖名称: {order.auctionTitle}</p>
        </div>
      </div>
      
      {/* 订单信息 */}
      <div className="order-info">
        <div className="info-row">
          <span>订单号:</span>
          <span>{order.orderNo}</span>
        </div>
        <div className="info-row">
          <span>成交金额:</span>
          <strong className="price">¥{order.finalAmount}</strong>
        </div>
        <div className="info-row">
          <span>创建时间:</span>
          <span>{formatDate(order.createdAt)}</span>
        </div>
        {order.paidTime && (
          <div className="info-row">
            <span>支付时间:</span>
            <span>{formatDate(order.paidTime)}</span>
          </div>
        )}
      </div>
      
      {/* 操作按钮 */}
      <div className="order-actions">
        {canPay && (
          <button 
            onClick={() => handlePay(order.id, order.finalAmount)}
            className="btn btn-primary"
          >
            立即支付
          </button>
        )}
        
        {canCancel && (
          <button 
            onClick={() => handleCancelOrder(order.id)}
            className="btn btn-secondary"
          >
            取消订单
          </button>
        )}
        
        <button 
          onClick={() => window.history.back()}
          className="btn btn-default"
        >
          返回
        </button>
      </div>
    </div>
  );
}
```

---

## 📋 2. 查询用户订单

### 接口信息

- **接口地址**: `GET /api/orders/user/{userId}`
- **接口说明**: 查询指定用户的所有订单
- **权限要求**: 需要登录，只能查询自己的订单

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | ✅ | 用户ID |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "orderNo": "AU20260531103025123456",
      "userId": 123,
      "productId": 1,
      "productName": "iPhone 15 Pro Max",
      "finalAmount": 5000.00,
      "status": "PENDING_PAYMENT",
      "statusDesc": "待支付",
      "createdAt": "2026-05-31T10:30:25"
    },
    {
      "id": 2,
      "orderNo": "AU20260531103025123457",
      "userId": 123,
      "productId": 2,
      "productName": "MacBook Pro",
      "finalAmount": 8000.00,
      "status": "PAID",
      "statusDesc": "已支付",
      "createdAt": "2026-05-30T15:30:25",
      "paidTime": "2026-05-30T16:00:00"
    }
  ],
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function getUserOrders() {
  try {
    const userId = localStorage.getItem('userId');
    const token = localStorage.getItem('accessToken');
    
    const response = await fetch(`http://localhost:8080/api/orders/user/${userId}`, {
      headers: {
        '        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      }
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('用户订单:', result.data);
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('查询订单失败:', error);
    throw error;
  }
}

// 用户订单列表组件
function UserOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  
  useEffect(() => {
    loadOrders();
  }, []);
  
  const loadOrders = async () => {
    setLoading(true);
    try {
      const data = await getUserOrders();
      setOrders(data);
    } catch (error) {
      showError('加载订单失败');
    } finally {
      setLoading(false);
    }
  };
  
  const filteredOrders = filter === 'ALL' 
    ? orders 
    : orders.filter(order => order.status === filter);
  
  return (
    <div className="user-orders">
      <div className="orders-header">
        <h1>我的订单</h1>
        <div className="filter-buttons">
          <button 
            className={filter === 'ALL' ? 'active' : ''}
            onClick={() => setFilter('ALL')}
          >
            全部
          </button>
          <button 
            className={filter === 'PENDING_PAYMENT' ? 'active' : ''}
            onClick={() => setFilter('PENDING_PAYMENT')}
          >
            待支付
          </button>
          <button 
            className={filter === 'PAID' ? 'active' : ''}
            onClick={() => setFilter('PAID')}
          >
            已支付
          </button>
          <button 
            className={filter === 'COMPLETED' ? 'active' : ''}
            onClick={() => setFilter('COMPLETED')}
          >
            已完成
          </button>
        </div>
      </div>
      
      {loading ? (
        <div className="loading">加载中...</div>
      ) : (
        <div className="orders-list">
          {filteredOrders.length === 0 ? (
            <div className="empty-state">
              <p>暂无订单</p>
            </div>
          ) : (
            filteredOrders.map(order => (
              <OrderCard 
                key={order.id}
                order={order}
                onViewDetail={() => window.location.href = `/orders/${order.id}`}
              />
            ))
          )}
        </div>
      )}
    </div>
  );
}

function OrderCard({ order, onViewDetail }) {
  return (
    <div className="order-card" onClick={onViewDetail}>
      <div className="order-header">
        <h3>{order.productName}</h3>
        <span className={`status-badge status-${order.status.toLowerCase()}`}>
          {order.statusDesc}
        </span>
      </div>
      
      <div className="order-body">
        <div className="order-info">
          <span>订单号: {order.orderNo}</span>
          <span>成交金额: <strong>¥{order.finalAmount}</strong></span>
        </div>
        <div className="order-info">
          <span>创建时间: {formatDate(order.createdAt)}</span>
        </div>
      </div>
    </div>
  );
}
```

---

## 🔍 3. 按状态查询订单

### 接口信息

- **接口地址**: `GET /api/orders/user/{userId}/status/{status}`
- **接口说明**: 查询指定状态的订单列表
- **权限要求**: 需要登录，只能查询自己的订单

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | ✅ | 用户ID |
| status | String | ✅ | 订单状态 (PENDING_PAYMENT, PAID, COMPLETED, CANCELLED) |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "orderNo": "AU20260531103025123456",
      "userId": 123,
      "productId": 1,
      "productName": "iPhone 15 Pro Max",
      "finalAmount": 5000.00,
      "status": "PENDING_PAYMENT",
      "createdAt": "2026-05-31T10:30:25"
    }
  ],
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function getOrdersByStatus(status) {
  try {
    const userId = localStorage.getItem('userId');
    const token = localStorage.getItem('accessToken');
    
    const response = await fetch(
      `http://localhost:8080/api/orders/user/${userId}/status/${status}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': userId
        }
      }
    );
    
    const result = await response.json();
    
    if (result.code === 200) {
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error(`查询${status}状态订单失败:`, error);
    throw error;
  }
}

// 使用示例
const loadPendingOrders = () => {
  getOrdersByStatus('PENDING_PAYMENT')
    .then(orders => {
      setPendingOrders(orders);
    })
    .catch(error => {
      console.error('加载待支付订单失败:', error);
    });
};
```

---

## 🎯 4. 根据拍卖ID查询订单

### 接口信息

- **接口地址**: `GET /api/orders/auction/{auctionId}`
- **接口说明**: 查询指定拍卖成交后生成的订单
- **权限要求**: 无需登录

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| auctionId | Long | ✅ | 拍卖ID |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "orderNo": "AU20260531103025123456",
    "userId": 123,
    "productId": 1,
    "productName": "iPhone 15 Pro Max",
    "finalAmount": 5000.00,
    "status": "PENDING_PAYMENT",
    "createdAt": "2026-05-31T10:30:25"
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 404,
  "message": "订单不存在",
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function getOrderByAuctionId(auctionId) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/orders/auction/${auctionId}`
    );
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('订单信息:', result.data);
      return result.data;
    } else if (result.code === 404) {
      // 拍卖可能还未结束，没有生成订单
      return null;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('查询订单失败:', error);
    throw error;
  }
}

// 在拍卖页面使用
function AuctionResult({ auctionId }) {
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    // 延迟查询，给系统一些时间生成订单
    const timer = setTimeout(() => {
      getOrderByAuctionId(auctionId).then(data => {
        if (data) {
          setOrder(data);
        }
        setLoading(false);
      }).catch(() => {
        setLoading(false);
      });
    }, 3000);
    
    return () => clearTimeout(timer);
  }, [auctionId]);
  
  return (
    <div className="auction-result">
      {loading ? (
        <div>正在查询订单信息...</div>
      ) : order ? (
        <div className="order-info">
          <h2>🎉 恭喜！您已成功拍得此商品</h2>
          <OrderDetail orderId={order.id} />
        </div>
      ) : (
        <div>
          <p>拍卖已结束，但未查询到订单信息</p>
        </div>
      )}
    </div>
  );
}
```

---

## ❌ 5. 取消订单

### 接口信息

- **接口地址**: `POST /api/orders/{orderId}/cancel`
- **接口说明**: 取消待支付状态的订单
- **权限要求**: 需要登录，必须是订单所有者

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderId | Long | ✅ | 订单ID |

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reason | String | ❌ | 取消原因 |

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

**错误响应**:
```json
{
  "code": 400,
  "message": "当前订单状态不允许取消",
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function cancelOrder(orderId, reason = '') {
  if (!confirm('确定要取消此订单吗？')) {
    return;
  }
  
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch(
      `http://localhost:8080/api/orders/${orderId}/cancel?reason=${encodeURIComponent(reason)}`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': userId
        }
      }
    );
    
    const result = await response.json();
    
    if (result.code === 200) {
      showSuccess('订单已取消');
      // 刷新订单列表
      window.location.reload();
    } else {
      showError(result.message);
    }
  } catch (error) {
    console.error('取消订单失败:', error);
    showError('取消订单失败，请稍后重试');
  }
}

// 取消订单确认对话框
function CancelOrderDialog({ orderId, onCancel }) {
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  
  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      await cancelOrder(orderId, reason);
      onCancel();
    } catch (error) {
      showError('取消订单失败');
    } finally {
      setSubmitting(false);
    }
  };
  
  return (
    <div className="cancel-order-dialog">
      <h3>取消订单</h3>
      <p>确定要取消此订单吗？此操作不可撤销。</p>
      
      <div className="form-group">
        <label>取消原因（可选）</label>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="请输入取消原因"
          rows="3"
        />
      </div>
      
      <div className="dialog-actions">
        <button 
          onClick={() => onCancel()}
          className="btn btn-default"
        >
          取消
        </button>
        <button 
          onClick={handleSubmit}
          disabled={submitting}
          className="btn btn-danger"
        >
          {submitting ? '取消中...' : '确认取消'}
        </button>
      </div>
    </div>
  );
}
```

---

## 📊 6. 获取订单统计

### 接口信息

- **接口地址**: `GET /api/orders/user/{userId}/statistics`
- **接口说明**: 获取用户的订单统计信息
- **权限要求**: 需要登录，只能查询自己的统计

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | ✅ | 用户ID |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalOrders": 10,
    "pendingPaymentCount": 2,
    "paidCount": 5,
    "completedCount": 3,
    "cancelledCount": 0
  },
  "timestamp": 1717056000000
}
```

### 统计指标说明

| 指标 | 说明 |
|------|------|
| totalOrders | 总订单数 |
| pendingPaymentCount | 待支付订单数 |
| paidCount | 已支付订单数 |
| completedCount | 已完成订单数 |
| cancelledCount | 已取消订单数 |

### 前端调用示例

```javascript
async function getUserOrderStatistics() {
  try {
    const userId = localStorage.getItem('userId');
    const token = localStorage.getItem('accessToken');
    
    const response = await fetch(
      `http://localhost:8080/api/orders/user/${userId}/statistics`,
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': userId
        }
      }
    );
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('订单统计:', result.data);
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('获取订单统计失败:', error);
    throw error;
  }
}

// 订单统计组件
function OrderStatistics() {
  const [stats, setStats] = useState(null);
  
  useEffect(() => {
    getUserOrderStatistics().then(data => {
      setStats(data);
    });
  }, []);
  
  if (!stats) return null;
  
  return (
    <div className="order-statistics">
      <h3>订单统计</h3>
      <div className="stats-grid">
        <div className="stat-card total">
          <span>总订单数</span>
          <strong>{stats.totalOrders}</strong>
        </div>
        <div className="stat-card pending">
          <span>待支付</span>
          <strong>{stats.pendingPaymentCount}</strong>
        </div>
        <div className="stat-card paid">
          <span>已支付</span>
          <strong>{stats.paidCount}</strong>
        </div>
        <div className="stat-card completed">
          <span>已完成</span>
          <strong>{stats.completedCount}</strong>
        </div>
        <div className="stat-card cancelled">
          <span>已取消</span>
          <strong>{stats.cancelledCount}</strong>
        </div>
      </div>
    </div>
  );
}
```

---

## 💳 7. 更新订单状态

### 接口信息

- **接口地址**: `PUT /api/orders/{orderId}/status`
- **接口说明**: 更新订单状态（管理员功能）
- **权限要求**: 需要管理员权限

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderId | Long | ✅ | 订单ID |

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| newStatus | String | ✅ | 新状态 (PAID, COMPLETED) |

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
async function updateOrderStatus(orderId, newStatus) {
  if (!confirm(`确定要将订单状态更改为 ${newStatus} 吗？`)) {
    return;
  }
  
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch(
      `http://localhost:8080/api/orders/${orderId}/status?newStatus=${newStatus}`,
      {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': userId
        }
      }
    );
    
    const result = await response.json();
    
    if (result.code === 200) {
      showSuccess('订单状态更新成功');
      // 刷新订单详情
      window.location.reload();
    } else {
      showError(result.message);
    }
  } catch (error) {
    console.error('更新订单状态失败:', error);
    showError('操作失败，请稍后重试');
  }
}
```

---

## 📝 完整订单管理页面

```javascript
// OrderManagement.js
import { useState, useEffect } from 'react';
import { format } from 'date-fns';

function OrderManagement() {
  const [orders, setOrders] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentTab, setCurrentTab] = useState('all');
  
  useEffect(() => {
    loadData();
    
    // 定时刷新订单数据（每30秒）
    const interval = setInterval(loadData, 30000);
    return () => clearInterval(interval);
  }, []);
  
  const loadData = async () => {
    setLoading(true);
    try {
      // 并行加载订单和统计
      const [ordersData, statsData] = await Promise.all([
        getUserOrders(),
        getUserOrderStatistics()
      ]);
      
      setOrders(ordersData);
      setStats(statsData);
    } catch (error) {
      showError('加载订单数据失败');
    } finally {
      setLoading(false);
    }
  };
  
  const handleTabChange = (tab) => {
    setCurrentTab(tab);
  };
  
  const filteredOrders = () => {
    switch (currentTab) {
      case 'pending':
        return orders.filter(order => order.status === 'PENDING_PAYMENT');
      case 'paid':
        return orders.filter(order => order.status === 'PAID');
      case 'completed':
        return orders.filter(order => order.status === 'COMPLETED');
      default:
        return orders;
    }
  };
  
  const handlePay = (orderId, amount) => {
    // 跳转到支付页面
    window.location.href = `/orders/${orderId}/pay`;
  };
  
  const handleCancel = (orderId) => {
    if (confirm('确定要取消此订单吗？')) {
      cancelOrder(orderId).then(() => {
        loadData();
      });
    }
  };
  
  if (loading) {
    return <div className="loading-spinner">加载中...</div>;
  }
  
  return (
    <div className="order-management">
      <div className="page-header">
        <h1>订单管理</h1>
      </div>
      
      {/* 统计卡片 */}
      {stats && (
        <div className="stats-section">
          <div className="stat-item">
            <span>总订单</span>
            <strong>{stats.totalOrders}</strong>
          </div>
          <div className="stat-item">
            <span>待支付</span>
            <strong>{stats.pendingPaymentCount}</strong>
          </div>
          <div className="stat-item">
            <span>已支付</span>
            <strong>{stats.paidCount}</strong>
          </div>
          <div className="stat-item">
            <span>已完成</span>
            <strong>{stats.completedCount}</strong>
          </div>
          <div className="stat-item">
            <span>已取消</span>
            <strong>{stats.cancelledCount}</strong>
          </div>
        </div>
      )}
      
      {/* 标签页 */}
      <div className="tabs">
        <button 
          className={currentTab === 'all' ? 'active' : ''}
          onClick={() => handleTabChange('all')}
        >
          全部订单 ({orders.length})
        </button>
        <button 
          className={currentTab === 'pending' ? 'active' : ''}
          onClick={() => handleTabChange('pending')}
        >
          待支付 ({stats?.pendingPaymentCount || 0})
        </button>
        <button 
          className={currentTab === 'paid' ? 'active' : ''}
          onClick={() => handleTabChange('paid')}
        >
          已支付 ({stats?.paidCount || 0})
        </button>
        <button 
          className={currentTab === 'completed' ? 'active' : ''}
          onClick={() => handleTabChange('completed')}
        >
          已完成 ({stats?.completedCount || 0})
        </button>
      </div>
      
      {/* 订单列表 */}
      <div className="orders-list">
        {filteredOrders().length === 0 ? (
          <div className="empty-state">
            <p>暂无订单</p>
          </div>
        ) : (
          filteredOrders().map(order => (
            <OrderCard 
              key={order.id}
              order={order}
              onPay={handlePay}
              onCancel={handleCancel}
            />
          ))
        )}
      </div>
    </div  );
}

function OrderCard({ order, onPay, onCancel }) {
  const getStatusColor = (status) => {
    const colors = {
      'PENDING_PAYMENT': '#ffc107',
      'PAID': '#28a745',
      'COMPLETED': '#007bff',
      'CANCELLED': '#6c757d'
    };
    return colors[status] || '#6c757d';
  };
  
  const canPay = order.status === 'PENDING_PAYMENT';
  const canCancel = order.status === 'PENDING_PAYMENT';
  
  return (
    <div className="order-card">
      <div className="order-header">
        <h4>{order.productName}</h4>
        <span 
          className="status-badge"
          style={{ backgroundColor: getStatusColor(order.status)}}
        >
          {order.statusDesc}
        </span>
      </div>
      
      <div className="order-body">
        <div className="order-info">
          <div className="info-item">
            <span>订单号:</span>
            <span className="order-no">{order.orderNo}</span>
          </div>
          <div className="info-item">
            <span>成交金额:</span>
            <span className="price">¥{order.finalAmount}</span>
          </div>
          <div className="info-item">
            <span>创建时间:</span>
            <span>{format(new Date(order.createdAt), 'yyyy-MM-dd HH:mm:ss')}</span>
          </div>
          {order.paidTime && (
            <div className="info-item">
              <span>支付时间:</span>
              <span>{format(new Date(order.paidTime), 'yyyy-MM-dd HH:mm:ss')}</span>
            </div>
          )}
        </div>
        
        <div className="order-actions">
          {canPay && (
            <button 
              onClick={() => onPay(order.id, order.finalAmount)}
              className="btn btn-primary btn-sm"
            >
              立即支付
            </button>
          )}
          
          {canCancel && (
            <button 
              onClick={() => onCancel(order.id)}
              className="btn btn-secondary btn-sm"
            >
              取消订单
            </button>
          )}
          
          <button 
            onClick={() => window.location.href = `/orders/${order.id}`}
            className="btn btn-default btn-sm"
          >
            查看详情
          </button>
        </div>
      </div>
    </div>
  );
}
```

---

## ⚠️ 注意事项

### 1. 订单创建机制

- 订单由拍卖结束自动创建，前端无需手动创建
- 拍卖完成后会自动为最高出价者生成订单
- 订单创建后默认状态为PENDING_PAYMENT（待支付）

### 2. 状态转换规则

- PENDING_PAYMENT → PAID（用户支付）
- PENDING_PAYMENT → CANCELLED（用户或系统取消）
- PAID → COMPLETED（管理员确认或自动完成）
- COMPLETED/CANCELLED → 不可变更

### 3. 权限控制

- 用户只能查询和操作自己的订单
- 管理员可以查询所有订单并更新状态
- 系统会在关键时刻自动更新订单状态

### 4. 用户体验

- 待支付订单建议提供倒计时
- 即将到期的订单建议突出显示
- 已完成订单建议提供评价功能
- 已取消订单建议说明取消原因

---

## 📞 技术支持

如有接口问题，请联系后端开发团队。

**最后更新**: 2026-05-31
**文档版本**: v1.0.0
