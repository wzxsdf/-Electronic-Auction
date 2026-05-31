# 出价管理模块API文档

## 📋 模块说明

出价管理模块是拍卖系统的核心模块，负责处理用户的出价请求、出价历史查询和出价统计。

**🔗 基础路径**: `/api/bids`

**⚡ 重要**: 出价模块与WebSocket紧密集成，出价成功后会实时推送价格更新给所有参与者。

---

## 💰 1. 用户出价

### 接口信息

- **接口地址**: `POST /api/bids`
- **接口说明**: 用户在拍卖活动中出价
- **权限要求**: 需要登录
- **限流规则**: 30次/分钟

### 请求参数

```json
{
  "auctionId": 1,
  "amount": 1050.00,
  "isAutoBid": false
}
```

**参数说明**:

| 参数名 | 类型 | 必填 | 说明 | 验证规则 |
|--------|------|------|------|----------|
| auctionId | Long | ✅ | 拍卖ID | 拍卖必须存在且状态为ACTIVE |
| amount | BigDecimal | ✅ | 出价金额 | 必须高于当前价格，且符合加价幅度 |
| isAutoBid | Boolean | ❌ | 是否自动出价 | 默认false |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "bidId": 12345,
    "currentPrice": 1050.00,
    "yourRank": 1,
    "isLeading": true,
    "remainingMs": 3600000,
    "message": "出价成功"
  },
  "timestamp": 1717056000000
}
```

**错误响应**:
```json
{
  "code": 4101,
  "message": "出价必须高于当前价格 1000.00",
  "timestamp": 1717056000000
}
```

### 出价验证规则

```javascript
// 出价金额验证
function validateBidAmount(currentPrice, bidIncrement, maxPrice, amount) {
  // 1. 必须高于当前价格
  if (amount <= currentPrice) {
    return { valid: false, message: '出价必须高于当前价格' };
  }
  
  // 2. 必须按加价幅度递增
  const minPrice = currentPrice + bidIncrement;
  if (amount < minPrice) {
    return { 
      valid: false, 
      message: `出价必须按 ${bidIncrement} 的幅度递增，最低有效出价为 ${minPrice}` 
    };
  }
  
  // 3. 不能超过封顶价
  if (maxPrice && amount > maxPrice) {
    return { 
      valid: false, 
      message: `出价不能超过封顶价 ${maxPrice}` 
    };
  }
  
  return { valid: true };
}
```

### 错误码说明

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 4101 | 出价金额过低 | 提示用户提高出价金额 |
| 4102 | 拍卖未开始 | 提示用户等待拍卖开始 |
| 4103 | 拍卖已结束 | 提示用户拍卖已结束 |
| 4104 | 拍卖已取消 | 提示用户拍卖已取消 |
| 4105 | 出价频率过高 | 提示用户稍后再试 |
| 4106 | 当前是最高出价者 | 提示用户已经是领先者 |
| 4107 | 出价超过封顶价 | 提示用户出价不能超过封顶价 |

### 前端调用示例

```javascript
async function placeBid(auctionId, amount) {
  try {
    const token = localStorage.getItem('accessToken');
    const userId = localStorage.getItem('userId');
    
    const response = await fetch('http://localhost:8080/api/bids', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId
      },
      body: JSON.stringify({
        auctionId,
        amount,
        isAutoBid: false
      })
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      const { currentPrice, yourRank, isLeading, remainingMs } = result.data;
      
      console.log('出价成功!');
      console.log(`当前价格: ¥${currentPrice}`);
      console.log(`您的排名: ${yourRank}`);
      console.log(`是否领先: ${isLeading ? '是' : '否'}`);
      
      // 显示成功提示
      if (isLeading) {
        showSuccess(`🎉 恭喜！您当前领先，排名第 ${yourRank} 位`);
      } else {
        showInfo(`出价成功，当前排名 ${yourRank} 位`);
      }
      
      // 刷新拍卖信息
      await refreshAuctionInfo(auctionId);
      
      return result.data;
    } else {
      // 显示错误提示
      console.error('出价失败:', result.message);
      showError(result.message);
    }
  } catch (error) {
    console.error('出价请求失败:', error);
    showError('网络错误，请稍后重试');
  }
}

// 出价按钮组件
function BidButton({ auctionId, currentPrice, bidIncrement }) {
  const [bidAmount, setBidAmount] = useState('');
  const [bidding, setBidding] = useState(false);
  
  // 计算建议出价金额
  const suggestedBid = currentPrice + bidIncrement;
  
  const handleQuickBid = () => {
    setBidAmount(suggestedBid.toFixed(2));
    placeBid(auctionId, suggestedBid);
  };
  
  const handleCustomBid = () => {
    if (!bidAmount || parseFloat(bidAmount) <= currentPrice) {
      showError(`出价必须大于当前价格 ¥${currentPrice}`);
      return;
    }
    
    placeBid(auctionId, parseFloat(bidAmount));
    setBidAmount('');
  };
  
  return (
    <div className="bid-section">
      <div className="current-price">
        当前价格: <strong>¥{currentPrice}</strong>
      </div>
      
      <div className="quick-bid">
        <button 
          onClick={handleQuickBid}
          className="btn btn-primary"
          disabled={bidding}
        >
          快速出价 ¥{suggestedBid}
        </button>
      </div>
      
      <div className="custom-bid">
        <input
          type="number"
          value={bidAmount}
          onChange={(e) => setBidAmount(e.target.value)}
          placeholder="自定义出价金额"
          min={currentPrice + bidIncrement}
          step={bidIncrement}
        />
        <button 
          onClick={handleCustomBid}
          className="btn btn-secondary"
          disabled={bidding || !bidAmount}
        >
          {bidding ? '出价中...' : '出价'}
        </button>
      </div>
      
      <div className="bid-info">
        <p>💡 提示：</p>
        <ul>
          <li>出价必须按 ¥{bidIncrement} 的幅度递增</li>
          <li>建议出价: ¥{suggestedBid}</li>
          <li>每分钟最多出价30次</li>
        </ul>
      </div>
    </div>
  );
}
```

---

## 📜 2. 查询出价历史

### 接口信息

- **接口地址**: `GET /api/bids/auction/{auctionId}`
- **接口说明**: 查询指定拍卖的出价历史记录
- **权限要求**: 无需登录

### 请求参数

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| auctionId | Long | ✅ | 拍卖ID |

**查询参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|-------|------|
| limit | Integer | ❌ | 100 | 返回记录数量，最大1000条 |

### 响应示例

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "bidId": 12345,
      "auctionId": 1,
      "userId": 123,
      "username": "te***",
      "amount": 1050.00,
      "isAutoBid": false,
      "bidTime": "2026-05-31T10:30:25"
    },
    {
      "bidId": 12346,
      "auctionId": 1,
      "userId": 456,
      "username": "用户***",
      "amount": 1100.00,
      "isAutoBid": false,
      "bidTime": "2026-05-31T10:31:10"
    }
  ],
  "timestamp": 1717056000000
}
```

### 前端调用示例

```javascript
async function getBidHistory(auctionId, limit = 100) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/bids/auction/${auctionId}?limit=${limit}`
    );
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('出价历史:', result.data);
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('查询出价历史失败:', error);
    throw error;
  }
}

// 出价历史列表组件
function BidHistoryList({ auctionId }) {
  const [bids, setBids] = useState([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    getBidHistory(auctionId).then(data => {
      setBids(data);
      setLoading(false);
    });
  }, [auctionId]);
  
  if (loading) return <div>加载中...</div>;
  if (bids.length === 0) return <p>暂无出价记录</p>;
  
  return (
    <div className="bid-history">
      <h3>出价历史</h3>
      <table className="bid-table">
        <thead>
          <tr>
            <th>排名</th>
            <th>用户</th>
            <th>出价金额</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          {bids.map((bid, index) => (
            <tr key={bid.bidId}>
              <td>{index + 1}</td>
              <td>{bid.username}</td>
              <td>¥{bid.amount}</td>
              <td>{formatTime(bid.bidTime)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatTime(dateString) {
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN');
}
```

---

## 📊 3. 获取出价统计

### 接口信息

- **接口地址**: `GET /api/bids/auction/{auctionId}/statistics`
- **接口说明**: 获取拍卖的出价统计信息
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
    "totalBids": 150,
    "participantCount": 25,
    "autoBidCount": 30,
    "currentHighestPrice": 5000.00,
    "currentLowestPrice": 1000.00,
    "averagePrice": 2345.67
  },
  "timestamp": 1717056000000
}
```

### 统计指标说明

| 指标 | 说明 |
|------|------|
| totalBids | 总出价次数 |
| participantCount | 参与人数（去重） |
| autoBidCount | 自动出价次数 |
| currentHighestPrice | 当前最高出价 |
| currentLowestPrice | 当前最低出价 |
| averagePrice | 平均出价金额 |

### 前端调用示例

```javascript
async function getBidStatistics(auctionId) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/bids/auction/${auctionId}/statistics`
    );
    
    const result = await response.json();
    
    if (result.code === 200) {
      console.log('出价统计:', result.data);
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('获取出价统计失败:', error);
    throw error;
  }
}

// 出价统计组件
function BidStatistics({ auctionId }) {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    getBidStatistics(auctionId).then(data => {
      setStats(data);
      setLoading(false);
    });
  }, [auctionId]);
  
  if (loading) return <div>加载中...</div>;
  if (!stats) return <div>暂无统计数据</div>;
  
  return (
    <div className="bid-statistics">
      <h3>出价统计</h3>
      <div className="stats-grid">
        <div className="stat-item">
          <span>总出价次数</span>
          <strong>{stats.totalBids}</strong>
        </div>
        <div className="stat-item">
          <span>参与人数</span>
          <strong>{stats.participantCount}</strong>
        </div>
        <div className="stat-item">
          <span>自动出价</span>
          <strong>{stats.autoBidCount}</strong>
        </div>
        <div className="stat-item">
          <span>最高出价</span>
          <strong>¥{stats.currentHighestPrice}</strong>
        </div>
        <div className="stat-item">
          <span>平均出价</span>
          <strong>¥{stats.averagePrice}</strong>
        </div>
      </div>
    </div>
  );
}
```

---

## 🔄 WebSocket实时更新

### WebSocket连接

出价模块与WebSocket紧密集成，出价成功后会实时推送更新。

**WebSocket地址**: `ws://localhost:8080/ws/auction/{auctionId}?token={access_token}`

### 消息类型

#### 新出价通知

```javascript
// 监听新出价消息
websocket.on('NEW_BID', (data) => {
  console.log('新出价通知:', data);
  
  // 更新当前价格
  updateCurrentPrice(data.currentPrice);
  
  // 更新排行榜
  updateLeaderboard(data.leaderboard);
  
  // 如果用户被超越，显示提示
  if (data.youWereOvertaken) {
    showNotification('您已被超越！', 'warning');
  }
  
  // 如果用户领先，显示提示
  if (data.yreAreLeading) {
    showNotification('恭喜！您当前领先！', 'success');
  }
});
```

#### 价格更新通知

```javascript
// 监听价格更新
websocket.on('PRICE_UPDATE', (data) => {
  console.log('价格更新:', data);
  
  // 更新显示
  document.getElementById('current-price').textContent = `¥${data.currentPrice}`;
  document.getElementById('bid-count').textContent = data.bidCount;
});
```

### WebSocket集成示例

```javascript
// WebSocket管理器
class AuctionWebSocket {
  constructor(auctionId, token) {
    this.auctionId = auctionId;
    this.ws = null;
    this.token = token;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }
  
  connect() {
    const wsUrl = `ws://localhost:8080/ws/auction/${this.auctionId}?token=${this.token}`;
    this.ws = new WebSocket(wsUrl);
    
    this.ws.onopen = () => {
      console.log('WebSocket连接成功');
      this.reconnectAttempts = 0;
    };
    
    this.ws.onmessage = (event) => {
      this.handleMessage(event.data);
    };
    
    this.ws.onerror = (error) => {
      console.error('WebSocket错误:', error);
    };
    
    this.ws.onclose = () => {
      console.log('WebSocket连接关闭');
      this.reconnect();
    };
  }
  
  handleMessage(data) {
    try {
      const message = JSON.parse(data);
      this.processMessage(message);
    } catch (error) {
      console.error('解析消息失败:', error);
    }
  }
  
  processMessage(message) {
    switch (message.type) {
      case 'NEW_BID':
        this.onNewBid(message.data);
        break;
      case 'PRICE_UPDATE':
        this.onPriceUpdate(message.data);
        break;
      case 'YOU_WERE_OVERTAKEN':
        this.onOvertaken(message.data);
        break;
      case 'YOU_ARE_LEADING':
        this.onLeading(message.data);
        break;
      case 'AUCTION_COMPLETED':
        this.onAuctionCompleted(message.data);
        break;
      case 'AUCTION_DELAYED':
        this.onAuctionDelayed(message.data);
        break;
      default:
        console.log('未知消息类型:', message.type);
    }
  }
  
  onNewBid(data) {
    // 触发出价更新事件
    window.dispatchEvent(new CustomEvent('bidUpdate', { detail: data }));
  }
  
  onPriceUpdate(data) {
    // 触发价格更新事件
    window.dispatchEvent(new CustomEvent('priceUpdate', { detail: data }));
  }
  
  onOvertaken(data) {
    // 显示被超越提示
    showNotification(`您已被超越！当前排名: ${data.yourNewRank}`, 'warning');
  }
  
  onLeading(data) {
    // 显示领先提示
    showNotification('恭喜！您当前领先！', 'success');
  }
  
  onAuctionCompleted(data) {
    // 拍卖结束
    showNotification('拍卖已结束！', 'info');
    // 跳转到结果页面
    window.location.href = `/auctions/${this.auctionId}/result`;
  }
  
  onAuctionDelayed(data) {
    // 拍卖延时
    showNotification(`拍卖已延长 ${data.delaySeconds} 秒`, 'info');
  }
  
  reconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      
      setTimeout(() => {
        this.connect();
      }, 3000);
    } else {
      console.error('达到最大重连次数，停止重连');
    }
  }
  
  disconnect() {
    if (this.ws) {
      this.ws.close();
    }
  }
}

// 使用示例
const auctionWs = new AuctionWebSocket(auctionId, accessToken);
auctionWs.connect();

// 监听自定义事件
window.addEventListener('bidUpdate', (event) => {
  console.log('出价更新:', event.detail);
  updateUI(event.detail);
});
```

---

## 📱 出价界面完整示例

```javascript
// AuctionRoom.js - 完整的拍卖房间页面
import { useState, useEffect } from 'react';
import { AuctionWebSocket } from '../utils/websocket';

function AuctionRoom({ auctionId }) {
  const [auction, setAuction] = useState(null);
  const [ws, setWs] = useState(null);
  const [bidAmount, setBidAmount] = useState('');
  const [bidding, setBidding] = useState(false);
  const [notification, setNotification] = useState(null);
  
  useEffect(() => {
    // 加载拍卖信息
    loadAuctionInfo();
    
    // 建立WebSocket连接
    const token = localStorage.getItem('accessToken');
    const auctionWs = new AuctionWebSocket(auctionId, token);
    auctionWs.connect();
    setWs(auctionWs);
    
    return () => {
      auctionWs.disconnect();
    };
  }, [auctionId]);
  
  const loadAuctionInfo = async () => {
    try {
      const data = await getAuctionDetail(auctionId);
      setAuction(data);
      
      // 设置默认出价金额
      const suggestedBid = data.currentPrice + data.bidIncrement;
      setBidAmount(suggestedBid.toFixed(2));
    } catch (error) {
      console.error('加载拍卖信息失败:', error);
      showError('加载拍卖信息失败');
    }
  };
  
  const handleBid = async () => {
    if (!bidAmount || parseFloat(bidAmount) <= auction.currentPrice) {
      showError(`出价必须大于当前价格 ¥${auction.currentPrice}`);
      return;
    }
    
    setBidding(true);
    
    try {
      await placeBid(auctionId, parseFloat(bidAmount));
      showSuccess('出价成功！');
      
      // 清空输入
      setBidAmount('');
      
      // 重新加载拍卖信息
      await loadAuctionInfo();
      
    } catch (error) {
      showError(error.message);
    } finally {
      setBidding(false);
    }
  };
  
  const getMinBid = () => {
    if (!auction) return 0;
    return (auction.currentPrice + auction.bidIncrement).toFixed(2);
  };
  
  if (!auction) return <div>加载中...</div>;
  
  // 检查拍卖是否已结束
  const isEnded = auction.status !== 'ACTIVE' && auction.status !== 'PENDING';
  
  return (
    <div className="auction-room">
      {/* 拍卖信息 */}
      <div className="auction-header">
        <h1>{auction.title}</h1>
        <div className="status-badge">
          {getStatusText(auction.status)}
        </div>
      </div>
      
      {/* 当前价格区域 */}
      <div className="price-section">
        <div className="current-price">
          <span>当前价格</span>
          <strong className="price">¥{auction.currentPrice}</strong>
        </div>
        
        <div className="bid-info">
          <span>出价次数: {auction.bidCount}</span>
          {auction.highestBidder && (
            <span>最高出价者: 用户{auction.highestBidder}</span>
          )}
        </div>
      </div>
      
      {/* 出价区域 */}
      {!isEnded && (
        <div className="bid-section">
          <div className="bid-form">
            <label>出价金额 (¥)</label>
            <input
              type="number"
              value={bidAmount}
              onChange={(e) => setBidAmount(e.target.value)}
              min={getMinBid()}
              step={auction.bidIncrement}
              placeholder="请输入出价金额"
              disabled={bidding}
            />
            
            <button
              onClick={handleBid}
              disabled={bidding || !bidAmount}
              className="btn btn-primary btn-large"
            >
              {bidding ? '出价中...' : '立即出价'}
            </button>
            
            <button
              onClick={() => setBidAmount(getMinBid())}
              className="btn btn-secondary"
              disabled={bidding}
            >
              建议出价
            </button>
          </div>
          
          <div className="bid-tips">
            <p>💡 出价规则：</p>
            <ul>
              <li>出价必须按 <strong>¥{auction.bidIncrement}</strong> 的幅度递增</li>
              <li>建议出价: <strong>¥{getMinBid()}</strong></li>
              <li>每分钟最多出价30次</li>
              {auction.maxPrice && (
                <li>封顶价: <strong>¥{auction.maxPrice}</strong></li>
              )}
            </ul>
          </div>
        </div>
      )}
      
      {/* 已结束提示 */}
      {isEnded && (
        <div className="auction-ended">
          <h2>拍卖已结束</h2>
          <p>最终价格: ¥{auction.currentPrice}</p>
          {auction.status === 'COMPLETED' && (
            <p>恭喜最高出价者！</p>
          )}
        </div>
      )}
      
      {/* 通知区域 */}
      {notification && (
        <div className={`notification ${notification.type}`}>
          {notification.message}
          <button onClick={() => setNotification(null)}>×</button>
        </div>
      )}
    </div>
  );
}

function showNotification(message, type = 'info') {
  setNotification({ message, type });
  setTimeout(() => setNotification(null), 5000);
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

## ⚠️ 注意事项

### 1. 出价限制

- 单用户每分钟最多出价30次
- 当前最高出价者不能连续出价
- 出价必须按加价幅度递增
- 不能超过封顶价（如果设置）

### 2. 实时更新

- 出价成功后会通过WebSocket实时推送价格更新
- 建议建立WebSocket连接监听实时消息
- WebSocket断开时会自动重连（最多5次）

### 3. 错误处理

- 401错误表示Token过期，需要刷新Token
- 403错误表示无权限，需要登录
- 400错误表示参数验证失败，提示用户检查输入

### 4. 用户体验

- 出价成功后显示成功提示
- 被超越时显示警告通知
- 领先时显示成功通知
- 建议提供快速出价按钮

---

## 📞 技术支持

如有接口问题，请联系后端开发团队。

**最后更新**: 2026-05-31
**文档版本**: v1.0.0
