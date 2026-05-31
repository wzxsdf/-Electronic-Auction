# 拍卖管理模块API文档

## 📋 模块说明

拍卖管理模块是拍卖系统的核心模块，负责拍卖活动的创建、管理和查询。

**🔗 基础路径**: `/api/auctions`

---

## ➕ 1. 创建拍卖

### 接口信息

- **接口地址**: `POST /api/auctions`
- **接口说明**: 创建新的拍卖活动
- **权限要求**: 需要登录，需要商家或管理员权限

### 请求参数

```json
{
  "productId": 1,
  "title": "iPhone 15 Pro Max 拍卖专场",
  "startPrice": 1000.00,
  "bidIncrement": 50.00,
  "maxPrice": 10000.00,
  "delaySeconds": 15,
  "startTime": "2026-05-31T10:00:00",
  "endTime": "2026-05-31T12:00:00"
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 | 验证规则 |
|--------|------|------|------|----------|
| productId | Long | ✅ | 关联商品ID | 商品必须存在 |
| title | String | ✅ | 拍卖标题 | 不能为空 |
| startPrice | BigDecimal | ✅ | 起拍价 | 必须> 0 |
| bidIncrement | BigDecimal | ✅ | 加价幅度 | 必须> 0 |
| maxPrice | BigDecimal | ❌ | 封顶价 | 必须> 起拍价 |
| delaySeconds | Integer | ❌ | 自动延时时长(秒) | 默认15秒 |
| startTime | String | ✅ | 开始时间 | ISO格式时间 |
| endTime | String | ✅ | 结束时间 | ISO格式时间，必须晚于开始时间 |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "productId": 1,
    "title": "iPhone 15 Pro Max 拍卖专场",
    "startPrice": 1000.00,
    "currentPrice": 1000.00,
    "bidIncrement": 50.00,
    "maxPrice": 10000.00,
    "delaySeconds": 15,
    "startTime": "2026-05-31T10:00:00",
    "endTime": "2026-05-31T12:00:00",
    "originalEndTime": "2026-05-31T12:00:00",
    "status": "PENDING",
    "highestBidder": null,
    "bidCount": 0
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "商品不存在",
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function createAuction(auctionData) {
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch('http://localhost:8080/api/auctions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      },
      body: JSON.stringify(auctionData)
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('拍卖创建成功:', result.data);
      showSuccess('拍卖创建成功');
      return result.data;
    } else {
      console.error('拍卖创建失败:', result.message);
      showError(result.message);
    }
  } catch (error) {
    console.error('创建拍卖请求失败:', error);
    showError('网络错误，请稍后重试');
  }
}
```

---

## 🔍 2. 查询拍卖详情

### 接口信息

- **接口地址**: `GET /api/auctions/{id}`
- **接口说明**: 根据拍卖ID查询拍卖详细信息
- **权限要求**: 无需登录

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | ✅ | 拍卖ID |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "productId": 1,
    "productName": "iPhone 15 Pro Max 256GB",
    "productImageUrl": "https://example.com/iphone.jpg",
    "title": "iPhone 15 Pro Max 拍卖专场",
    "startPrice": 1000.00,
    "currentPrice": 2500.00,
    "bidIncrement": 50.00,
    "maxPrice": 10000.00,
    "delaySeconds": 15,
    "startTime": "2026-05-31T10:00:00",
    "endTime": "2026-05-31T12:15:00",
    "originalEndTime": "2026-05-31T12:00:00",
    "status": "ACTIVE",
    "highestBidder": 123,
    "bidCount": 15,
    "description": "全新未拆封，正品行货"
  },
  "timestamp": 1717056000000
}
```

### 拍卖状态说明

| 状态 | 说明 |
|------|------|
| PENDING | 待开始 |
| ACTIVE | 进行中 |
| COMPLETED | 已完成 |
| CANCELLED | 已取消 |

### 前端调用示例

```javascript
async function getAuctionDetail(auctionId) {
  try {
    const response = await fetch(`http://localhost:8080/api/auctions/${auctionId}`);
    
    const result = await response.json();
    
    if (result.code === 200) {
      const auction = result.data;
      
      // 计算剩余时间
      const remainingMs = new Date(auction.endTime) - new Date();
      auction.remainingMs = remainingMs;
      auction.remainingMinutes = Math.floor(remainingMs / 60000);
      
      // 判断是否可以出价
      auction.canBid = auction.status === 'ACTIVE' && remainingMs > 0;
      
      console.log('拍卖详情:', auction);
      return auction;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('查询拍卖详情失败:', error);
    throw error;
  }
}

// React组件示例
function AuctionDetail({ auctionId }) {
  const [auction, setAuction] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    getAuctionDetail(auctionId).then(data => {
      setAuction(data);
      setLoading(false);
    });
  }, [auctionId]);
  
  if (loading) return <div>加载中...</div>;
  if (!auction) return <div>拍卖不存在</div>;
  
  return (
    <div className="auction-detail">
      <h1>{auction.title}</h1>
      <div className="product-info">
        <img src={auction.productImageUrl} alt={auction.productName} />
        <h3>{auction.productName}</h3>
        <p>{auction.description}</p>
      </div>
      
      <div className="auction-info">
        <div className="info-item">
          <span>当前价格:</span>
          <strong>¥{auction.currentPrice}</strong>
        </div>
        <div className="info-item">
          <span>起拍价:</span>
          <strong>¥{auction.startPrice}</strong>
        </div>
        <div className="info-item">
          <span>加价幅度:</span>
          <strong>¥{auction.bidIncrement}</strong>
        </div>
        <div className="info-item">
          <span>封顶价:</span>
          <strong>¥{auction.maxPrice || '无封顶'}</strong>
        </div>
        <div className="info-item">
          <span>出价次数:</span>
          <strong>{auction.bidCount}</strong>
        </div>
        <div className="info-item">
          <span>状态:</span>
          <strong>{getStatusText(auction.status)}</strong>
        </div>
        <div className="info-item">
          <span>剩余时间:</span>
          <strong>{auction.remainingMinutes}分钟</strong>
        </div>
      </div>
      
      {auction.canBid && (
        <BidButton auctionId={auction.id} currentPrice={auction.currentPrice} />
      )}
    </div>
  );
}

function getStatusText(status) {
  const statusMap = {
    'PENDING': '待开始',
    'ACTIVE': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  };
  return statusMap[status] || status;
}
```

---

## 📋 3. 查询所有拍卖

### 接口信息

- **接口地址**: `GET /api/auctions`
- **接口说明**: 查询系统中所有拍卖活动
- **权限要求**: 无需登录

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "iPhone 15 Pro Max 拍卖",
      "productId": 1,
      "productName": "iPhone 15 Pro Max",
      "startPrice": 1000.00,
      "currentPrice": 2500.00,
      "status": "ACTIVE",
      "endTime": "2026-05-31T12:00:00"
    },
    {
      "id": 2,
      "title": "MacBook Pro 拍卖",
      "productId": 2,
      "productName": "MacBook Pro",
      "startPrice": 5000.00,
      "currentPrice": 5000.00,
      "status": "PENDING",
      "endTime": "2026-06-01T10:00:00"
    }
  ],
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function getAllAuctions() {
  try {
    const response = await fetch('http://localhost:8080/api/auctions');
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('拍卖列表:', result.data);
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('查询拍卖列表失败:', error);
    throw error;
  }
}

// 拍卖列表组件
function AuctionList() {
  const [auctions, setAuctions] = useState([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    getAllAuctions().then(data => {
      setAuctions(data);
      setLoading(false);
    });
  }, []);
  
  if (loading) return <div>加载中...</div>;
  
  return (
    <div className="auction-list">
      {auctions.map(auction => (
        <AuctionCard 
          key={auction.id}
          auction={auction}
        />
      ))}
    </div>
  );
}
```

---

## 🔥 4. 查询活跃拍卖

### 接口信息

- **接口地址**: `GET /api/auctions/active`
- **接口说明**: 查询所有正在进行的拍卖活动
- **权限要求**: 无需登录

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "iPhone 15 Pro Max 拍卖",
      "currentPrice": 3500.00,
      "highestBidder": 123,
      "bidCount": 25,
      "endTime": "2026-05-31T12:30:00",
      "status": "ACTIVE"
    }
  ],
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function getActiveAuctions() {
  try {
    const response = await fetch('http://localhost:8080/api/auctions/active');
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('活跃拍卖:', result.data);
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('查询活跃拍卖失败:', error);
    throw error;
  }
}

// 首页展示活跃拍卖
function ActiveAuctions() {
  const [auctions, setAuctions] = useState([]);
  
  useEffect(() => {
    // 定时刷新活跃拍卖列表
    getActiveAuctions().then(data => {
      setAuctions(data);
    });
    
    const interval = setInterval(() => {
      getActiveAuctions().then(data => {
        setAuctions(data);
      });
    }, 30000); // 每30秒刷新一次
    
    return () => clearInterval(interval);
  }, []);
  
  return (
    <div className="active-auctions">
      <h2>正在进行的拍卖</h2>
      {auctions.length === 0 ? (
        <p>暂无活跃拍卖</p>
      ) : (
        <div className="auction-grid">
          {auctions.map(auction => (
            <AuctionCard 
              key={auction.id}
              auction={auction}
              showStatus={false}
              showEndTime={true}
            />
          ))}
        </div>
      )}
    </div>
  );
}
```

---

## ▶️ 5. 开始拍卖

### 接口信息

- **接口地址**: `POST /api/auctions/{id}/start`
- **接口说明**: 开始指定的拍卖活动
- **权限要求**: 需要登录，需要商家或管理员权限

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | ✅ | 拍卖ID |

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
async function startAuction(auctionId) {
  if (!confirm('确定要开始这个拍卖吗？')) {
    return;
  }
  
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch(`http://localhost:8080/api/auctions/${auctionId}/start`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      }
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('拍卖已开始');
      showSuccess('拍卖已开始');
      // 刷新拍卖列表
      window.location.reload();
    } else {
      showError(result.message);
    }
  } catch (error) {
    console.error('开始拍卖失败:', error);
    showError('操作失败，请稍后重试');
  }
}
```

---

## ❌ 6. 取消拍卖

### 接口信息

- **接口地址**: `POST /api/auctions/{id}/cancel`
- **接口说明**: 取消指定的拍卖活动
- **权限要求**: 需要登录，需要商家或管理员权限

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | ✅ | 拍卖ID |

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

### 前端调用示例

```javascript
async function cancelAuction(auctionId, reason) {
  if (!confirm('确定要取消这个拍卖吗？此操作不可撤销！')) {
    return;
  }
  
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch(`http://localhost:8080/api/auctions/${auctionId}/cancel?reason=${encodeURIComponent(reason || '')}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      }
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('拍卖已取消');
      showSuccess('拍卖已取消');
      // 跳转到拍卖列表
      window.location.href = '/auctions';
    } else {
      showError(result.message);
    }
  } catch (error) {
    console.error('取消拍卖失败:', error);
    showError('操作失败，请稍后重试');
  }
}
```

---

## 🎨 拍卖卡片组件

```javascript
// AuctionCard.js
import { useState, useEffect } from 'react';

function AuctionCard({ auction, showStatus = true, showEndTime = false, onClick }) {
  const [timeLeft, setTimeLeft] = useState('');
  
  useEffect(() => {
    if (auction.status === 'ACTIVE' && showEndTime) {
      // 计算剩余时间
      const calculateTimeLeft = () => {
        const now = new Date();
        const endTime = new Date(auction.endTime);
        const diff = endTime - now;
        
        if (diff <= 0) {
          setTimeLeft('已结束');
          return;
        }
        
        const hours = Math.floor(diff / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);
        
        setTimeLeft(`${hours}时${minutes}分${seconds}秒`);
      };
      
      calculateTimeLeft();
      const interval = setInterval(calculateTimeLeft, 1000);
      
      return () => clearInterval(interval);
    }
  }, [auction, showEndTime]);
  
  const getStatusColor = (status) => {
    const colors = {
      'PENDING': '#6c757d',
      'ACTIVE': '#28a745',
      'COMPLETED': '#007bff',
      'CANCELLED': '#dc3545'
    };
    return colors[status] || '#6c757d';
  };
  
  return (
    <div className="auction-card" onClick={onClick ? () => onClick(auction.id) : undefined}>
      <div className="auction-header">
        <h3>{auction.title}</h3>
        {showStatus && (
          <span 
            className="status-badge" 
            style={{ backgroundColor: getStatusColor(auction.status)}}
          >
            {auction.status === 'ACTIVE' ? '进行中' : 
             auction.status === 'PENDING' ? '待开始' :
             auction.status === 'COMPLETED' ? '已完成' : '已取消'}
          </span>
        )}
      </div>
      
      <div className="auction-body">
        <div className="price-info">
          <span>当前价格</span>
          <strong>¥{auction.currentPrice}</strong>
        </div>
        
        <div className="info-row">
          <span>出价次数</span>
          <span>{auction.bidCount}</span>
        </div>
        
        {auction.highestBidder && (
          <div className="info-row">
            <span>最高出价者</span>
            <span>用户{auction.highestBidder}</span>
          </div>
        )}
        
        {showEndTime && timeLeft && (
          <div className="time-left">
            <span>剩余时间</span>
            <strong>{timeLeft}</strong>
          </div>
        )}
      </div>
    </div>
  );
}
```

---

## 📝 拍卖创建表单

```javascript
// AuctionForm.js
import { useState } from 'react';

function AuctionForm({ products }) {
  const [formData, setFormData] = useState({
    productId: '',
    title: '',
    startPrice: '',
    bidIncrement: '',
    maxPrice: '',
    delaySeconds: 15,
    startTime: '',
    endTime: ''
  });
  
  const [submitting, setSubmitting] = useState(false);
  
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // 基本验证
    if (!formData.productId || !formData.title || !formData.startPrice || 
        !formData.bidIncrement || !formData.startTime || !formData.endTime) {
      showError('请填写所有必填字段');
      return;
    }
    
    // 验证价格
    const startPrice = parseFloat(formData.startPrice);
    const bidIncrement = parseFloat(formData.bidIncrement);
    const maxPrice = formData.maxPrice ? parseFloat(formData.maxPrice) : null;
    
    if (startPrice <= 0 || bidIncrement <= 0) {
      showError('价格必须大于0');
      return;
    }
    
    if (maxPrice && maxPrice <= startPrice) {
      showError('封顶价必须大于起拍价');
      return;
    }
    
    // 验证时间
    const startTime = new Date(formData.startTime);
    const endTime = new Date(formData.endTime);
    
    if (endTime <= startTime) {
      showError('结束时间必须晚于开始时间');
      return;
    }
    
    setSubmitting(true);
    
    try {
      const auctionData = {
        ...formData,
        startPrice,
        bidIncrement,
        maxPrice,
        startTime: formData.startTime,
        endTime: formData.endTime
      };
      
      const result = await createAuction(auctionData);
      showSuccess('拍卖创建成功！');
      
      // 重置表单或跳转到拍卖详情
      window.location.href = `/auctions/${result.id}`;
      
    } catch (error) {
      showError('创建拍卖失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };
  
  return (
    <form onSubmit={handleSubmit} className="auction-form">
      <div className="form-group">
        <label>选择商品 *</label>
        <select
          name="productId"
          value={formData.productId}
          onChange={handleChange}
          required
        >
          <option value="">请选择商品</option>
          {products.map(product => (
            <option key={product.id} value={product.id}>
              {product.name}
            </option>
          ))}
        </select>
      </div>
      
      <div className="form-group">
        <label>拍卖标题 *</label>
        <input
          type="text"
          name="title"
          value={formData.title}
          onChange={handleChange}
          placeholder="请输入拍卖标题"
          required
        />
      </div>
      
      <div className="form-row">
        <div className="form-group">
          <label>起拍价(¥) *</label>
          <input
            type="number"
            name="startPrice"
            value={formData.startPrice}
            onChange={handleChange}
            min="0"
            step="0.01"
            placeholder="0.00"
            required
          />
        </div>
        
        <div className="form-group">
          <label>加价幅度(¥) *</label>
          <input
            type="number"
            name="bidIncrement"
            value={formData.bidIncrement}
            onChange={handleChange}
            min="0"
            step="0.01"
            placeholder="0.00"
            required
          />
        </div>
      </div>
      
      <div className="form-row">
        <div className="form-group">
          <label>封顶价(¥)</label>
          <input
            type="number"
            name="maxPrice"
            value={formData.maxPrice}
            onChange={handleChange}
            min="0"
            step="0.01"
            placeholder="可选，留空表示无封顶"
          />
        </div>
        
        <div className="form-group">
          <label>延时时间(秒)</label>
          <input
            type="number"
            name="delaySeconds"
            value={formData.delaySeconds}
            onChange={handleChange}
            min="0"
            placeholder="默认15秒"
          />
        </div>
      </div>
      
      <div className="form-row">
        <div className="form-group">
          <label>开始时间 *</label>
          <input
            type="datetime-local"
            name="startTime"
            value={formData.startTime}
            onChange={handleChange}
            required
          />
        </div>
        
        <div className="form-group">
          <label>结束时间 *</label>
          <input
            type="datetime-local"
            name="endTime"
            value={formData.endTime}
            onChange={handleChange}
            required
          />
        </div>
      </div>
      
      <button 
        type="submit" 
        disabled={submitting}
        className="btn btn-primary"
      >
        {submitting ? '创建中...' : '创建拍卖'}
      </button>
    </form>
  );
}
```

---

## ⚠️ 注意事项

### 1. 拍卖规则

- 起拍价必须大于0
- 加价幅度必须大于0
- 封顶价必须大于起拍价（如果设置）
- 结束时间必须晚于开始时间
- 延时时间默认为15秒，建议值：10-30秒

### 2. 状态管理

- 拍卖创建后状态为PENDING（待开始）
- 需要手动调用开始接口将状态变为ACTIVE（进行中）
- 只有ACTIVE状态的拍卖才能接受出价
- 拍卖结束后状态变为COMPLETED（已完成）

### 3. 自动延时机制

- 当剩余时间<延时阈值时，如果有新出价，会自动延时
- 默认延时15秒，可在创建拍卖时自定义
- 达到封顶价时拍卖立即结束，不再延时

### 4. 权限控制

- 创建拍卖需要商家或管理员权限
- 开始和取消拍卖需要创建者权限
- 查询拍卖无需权限

---

## 📞 技术支持

如有接口问题，请联系后端开发团队。

**最后更新**: 2026-05-31
**文档版本**: v1.0.0
