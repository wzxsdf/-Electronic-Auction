# 当前活跃拍卖活动API使用指南

## 📖 接口概述

### 接口信息

**接口路径**: `GET /auction-items/active-auctions`  
**功能描述**: 查询当前正在进行中的所有拍卖活动及拍品信息  
**适用场景**: 首页展示、实时拍卖大厅、用户进入应用时查看正在进行的拍卖

---

## 🎯 接口特性

### 核心功能

1. **自动筛选**: 只返回正在进行中（ACTIVE状态）的拍品
2. **按活动分组**: 拍品按所属拍卖活动自动分组展示
3. **完整信息**: 包含价格、倒计时、最高出价者、商品图片等完整信息
4. **时间同步**: 返回绝对时间戳，确保前后端时间一致
5. **商品关联**: 自动加载商品名称、图片和描述信息

---

## 📡 API调用

### 请求示例

```bash
GET /auction-items/active-auctions
Authorization: Bearer {token}
```

**请求参数**: 无需参数

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "auctions": [
      {
        "auctionId": 1,
        "auctionTitle": "周六古董拍卖专场",
        "auctionDescription": "精选古董珍品拍卖",
        "items": [
          {
            "itemId": 101,
            "title": "清代青花瓷瓶",
            "productName": "青花瓷瓶-清乾隆年间",
            "productImageUrl": "https://example.com/images/vase.jpg",
            "description": "这是一件精美的清代青花瓷瓶...",
            "currentPrice": 8500.00,
            "startPrice": 5000.00,
            "bidIncrement": 100.00,
            "maxPrice": 20000.00,
            "highestBidder": 12345,
            "bidCount": 8,
            "endTime": "2026-06-10T15:30:00",
            "endTimeTimestamp": 1686427500000,
            "remainingSeconds": 1800,
            "isBiddable": true,
            "delayCount": 0
          },
          {
            "itemId": 102,
            "title": "明代紫砂壶",
            "productName": "紫砂壶-明万历",
            "productImageUrl": "https://example.com/images/teapot.jpg",
            "description": "明代紫砂壶，工艺精湛...",
            "currentPrice": 3200.00,
            "startPrice": 2000.00,
            "bidIncrement": 50.00,
            "maxPrice": 10000.00,
            "highestBidder": 67890,
            "bidCount": 5,
            "endTime": "2026-06-10T16:00:00",
            "endTimeTimestamp": 1686429600000,
            "remainingSeconds": 3600,
            "isBiddable": true,
            "delayCount": 1
          }
        ]
      },
      {
        "auctionId": 2,
        "auctionTitle": "现代艺术品拍卖",
        "auctionDescription": "当代艺术家作品专场",
        "items": [
          {
            "itemId": 201,
            "title": "抽象油画作品",
            "productName": "《无题》抽象油画",
            "productImageUrl": "https://example.com/images/painting.jpg",
            "description": "当代知名艺术家抽象派作品...",
            "currentPrice": 12000.00,
            "startPrice": 8000.00,
            "bidIncrement": 200.00,
            "maxPrice": 50000.00,
            "highestBidder": 11111,
            "bidCount": 3,
            "endTime": "2026-06-10T17:00:00",
            "endTimeTimestamp": 1686433200000,
            "remainingSeconds": 7200,
            "isBiddable": true,
            "delayCount": 0
          }
        ]
      }
    ]
  }
}
```

---

## 📊 响应字段说明

### AuctionInfo（拍卖活动信息）

| 字段 | 类型 | 说明 |
|------|------|------|
| `auctionId` | Long | 拍卖活动ID |
| `auctionTitle` | String | 活动标题 |
| `auctionDescription` | String | 活动描述 |
| `items` | List | 该活动下正在进行的拍品列表 |

### ActiveItemInfo（正在进行中的拍品信息）

| 字段 | 类型 | 推荐度 | 说明 |
|------|------|--------|------|
| `itemId` | Long | ⭐⭐⭐⭐⭐ | 拍品ID |
| `title` | String | ⭐⭐⭐⭐⭐ | 拍品标题 |
| `productName` | String | ⭐⭐⭐⭐ | 商品名称 |
| `productImageUrl` | String | ⭐⭐⭐⭐⭐ | 商品图片URL |
| `description` | String | ⭐⭐⭐ | 商品描述 |
| `currentPrice` | BigDecimal | ⭐⭐⭐⭐⭐ | 当前价格 |
| `startPrice` | BigDecimal | ⭐⭐⭐⭐ | 起拍价 |
| `bidIncrement` | BigDecimal | ⭐⭐⭐ | 加价幅度 |
| `maxPrice` | BigDecimal | ⭐⭐⭐ | 封顶价 |
| `highestBidder` | Long | ⭐⭐⭐⭐ | 当前最高出价者ID |
| `bidCount` | Integer | ⭐⭐⭐⭐ | 出价次数 |
| `endTime` | String | ⭐⭐⭐⭐⭐ | 结束时间（ISO 8601格式） |
| `endTimeTimestamp` | Long | ⭐⭐⭐⭐⭐ | 结束时间戳（毫秒） |
| `remainingSeconds` | Long | ⭐ 已废弃 | 剩余秒数（不要使用） |
| `isBiddable` | Boolean | ⭐⭐⭐⭐ | 是否可出价 |
| `delayCount` | Integer | ⭐⭐⭐ | 延时次数 |

---

## 💻 前端实现示例

### React组件

```jsx
import React, { useState, useEffect } from 'react';
import { fetch } from '@/api';

const ActiveAuctionsPage = () => {
  const [auctions, setAuctions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadActiveAuctions();
    // 每30秒刷新一次
    const timer = setInterval(loadActiveAuctions, 30000);
    return () => clearInterval(timer);
  }, []);

  const loadActiveAuctions = async () => {
    try {
      setLoading(true);
      const response = await fetch('/auction-items/active-auctions');
      const result = await response.json();

      if (result.code === 200) {
        setAuctions(result.data.auctions);
      }
    } catch (error) {
      console.error('加载活跃拍卖失败:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>加载中...</div>;

  if (auctions.length === 0) return <div>当前没有正在进行的拍卖</div>;

  return (
    <div className="active-auctions">
      <h1>正在进行的拍卖</h1>

      {auctions.map(auction => (
        <div key={auction.auctionId} className="auction-section">
          <h2>{auction.auctionTitle}</h2>
          <p>{auction.auctionDescription}</p>

          <div className="items-grid">
            {auction.items.map(item => (
              <AuctionItemCard key={item.itemId} item={item} />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

// 拍品卡片组件
const AuctionItemCard = ({ item }) => {
  const [remainingTime, setRemainingTime] = useState(null);

  useEffect(() => {
    // 创建倒计时
    const timer = new AuctionTimer(
      item.endTimeTimestamp,
      (time) => setRemainingTime(time),
      () => {
        showNotification('拍卖已结束');
        // 重新加载列表
        window.location.reload();
      }
    );
    timer.start();

    return () => timer.stop();
  }, [item.endTimeTimestamp]);

  const handleBid = async () => {
    try {
      const response = await fetch(`/auction-items/${item.itemId}/bid`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          amount: item.currentPrice + item.bidIncrement
        })
      });

      const result = await response.json();
      if (result.code === 200) {
        showNotification('出价成功！');
        // 更新endTime（可能触发延时）
        if (result.data.endTimeTimestamp) {
          // 倒计时会自动更新
        }
      }
    } catch (error) {
      showNotification('出价失败: ' + error.message);
    }
  };

  return (
    <div className="item-card">
      {/* 商品图片 */}
      <img 
        src={item.productImageUrl} 
        alt={item.productName}
        className="item-image"
      />

      {/* 商品信息 */}
      <h3>{item.title}</h3>
      <p className="product-name">{item.productName}</p>
      <p className="description">{item.description}</p>

      {/* 价格信息 */}
      <div className="price-info">
        <p className="current-price">
          当前价格: ¥{item.currentPrice}
        </p>
        <p className="start-price">
          起拍价: ¥{item.startPrice}
        </p>
        <p className="bid-count">
          出价次数: {item.bidCount}
        </p>
      </div>

      {/* 倒计时 */}
      <div className="countdown">
        {remainingTime ? (
          <p>
            剩余时间: {String(remainingTime.minutes).padStart(2, '0')}:
                      {String(remainingTime.seconds).padStart(2, '0')}
          </p>
        ) : (
          <p>计算中...</p>
        )}
        {item.delayCount > 0 && (
          <span className="delay-badge">
            已延时 {item.delayCount} 次
          </span>
        )}
      </div>

      {/* 出价按钮 */}
      {item.isBiddable && (
        <button 
          onClick={handleBid}
          className="bid-button"
        >
          立即出价
        </button>
      )}

      {/* 最高出价者 */}
      {item.highestBidder && (
        <p className="highest-bidder">
          当前最高: 用户{item.highestBidder}
        </p>
      )}
    </div>
  );
};

export default ActiveAuctionsPage;
```

---

### Vue组件

```vue
<template>
  <div class="active-auctions">
    <h1>正在进行的拍卖</h1>

    <div v-if="loading">加载中...</div>
    <div v-else-if="auctions.length === 0">当前没有正在进行的拍卖</div>
    
    <div 
      v-for="auction in auctions" 
      :key="auction.auctionId"
      class="auction-section"
    >
      <h2>{{ auction.auctionTitle }}</h2>
      <p>{{ auction.auctionDescription }}</p>

      <div class="items-grid">
        <AuctionItemCard 
          v-for="item in auction.items"
          :key="item.itemId"
          :item="item"
        />
      </div>
    </div>
  </div>
</template>

<script>
import AuctionItemCard from './AuctionItemCard.vue';

export default {
  name: 'ActiveAuctionsPage',
  components: { AuctionItemCard },
  data() {
    return {
      auctions: [],
      loading: true,
      refreshTimer: null
    };
  },
  mounted() {
    this.loadActiveAuctions();
    this.refreshTimer = setInterval(this.loadActiveAuctions, 30000);
  },
  beforeUnmount() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  },
  methods: {
    async loadActiveAuctions() {
      try {
        this.loading = true;
        const response = await this.$api.get('/auction-items/active-auctions');
        
        if (response.code === 200) {
          this.auctions = response.data.auctions;
        }
      } catch (error) {
        console.error('加载活跃拍卖失败:', error);
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
```

```vue
<!-- AuctionItemCard.vue -->
<template>
  <div class="item-card">
    <!-- 商品图片 -->
    <img 
      :src="item.productImageUrl" 
      :alt="item.productName"
      class="item-image"
    />

    <!-- 商品信息 -->
    <h3>{{ item.title }}</h3>
    <p class="product-name">{{ item.productName }}</p>
    <p class="description">{{ item.description }}</p>

    <!-- 价格信息 -->
    <div class="price-info">
      <p class="current-price">当前价格: ¥{{ item.currentPrice }}</p>
      <p class="start-price">起拍价: ¥{{ item.startPrice }}</p>
      <p class="bid-count">出价次数: {{ item.bidCount }}</p>
    </div>

    <!-- 倒计时 -->
    <div class="countdown">
      <template v-if="remainingTime">
        <p>
          剩余时间: {{ formatTime(remainingTime) }}
        </p>
      </template>
      <template v-else>
        <p>计算中...</p>
      </template>
      
      <span v-if="item.delayCount > 0" class="delay-badge">
        已延时 {{ item.delayCount }} 次
      </span>
    </div>

    <!-- 出价按钮 -->
    <button 
      v-if="item.isBiddable"
      @click="handleBid"
      class="bid-button"
    >
      立即出价
    </button>

    <!-- 最高出价者 -->
    <p v-if="item.highestBidder" class="highest-bidder">
      当前最高: 用户{{ item.highestBidder }}
    </p>
  </div>
</template>

<script>
export default {
  name: 'AuctionItemCard',
  props: {
    item: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      remainingTime: null,
      timer: null
    };
  },
  mounted() {
    this.startCountdown();
  },
  beforeUnmount() {
    if (this.timer) {
      this.timer.stop();
    }
  },
  methods: {
    startCountdown() {
      this.timer = new AuctionTimer(
        this.item.endTimeTimestamp,
        (time) => {
          this.remainingTime = time;
        },
        () => {
          this.$message.info('拍卖已结束');
          this.$emit('ended');
        }
      );
      this.timer.start();
    },

    async handleBid() {
      try {
        const response = await this.$api.post(`/auction-items/${this.item.itemId}/bid`, {
          amount: this.item.currentPrice + this.item.bidIncrement
        });

        if (response.code === 200) {
          this.$message.success('出价成功！');
          
          // 更新endTime（可能触发延时）
          if (response.data.endTimeTimestamp) {
            if (this.timer) {
              this.timer.updateEndTime(response.data.endTimeTimestamp);
            }
          }
        }
      } catch (error) {
        this.$message.error('出价失败: ' + error.message);
      }
    },

    formatTime(time) {
      const { minutes, seconds } = time;
      return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }
  }
};
</script>
```

---

## ⚡ 性能优化建议

### 1. 合理的刷新频率

```javascript
// ❌ 不推荐：过于频繁的刷新（浪费资源）
setInterval(loadActiveAuctions, 1000);  // 每秒刷新

// ✅ 推荐：30秒刷新一次
setInterval(loadActiveAuctions, 30000);  // 30秒刷新
```

### 2. 结合WebSocket实时更新

```javascript
// 使用WebSocket实时更新，而不是轮询
const ws = new WebSocket('ws://localhost:8080/auction/item/' + itemId);

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  switch (message.type) {
    case 'NEW_BID':
      // 新出价：更新价格
      updateItemPrice(message.data);
      break;
    case 'AUCTION_DELAYED':
      // 延时：更新倒计时
      updateItemCountdown(message.data);
      break;
    case 'AUCTION_ENDED':
      // 结束：从列表中移除
      removeItem(message.data.auctionItemId);
      break;
  }
};
```

### 3. 数据缓存

```javascript
// 使用缓存减少API调用
let cachedData = null;
let cacheTime = null;
const CACHE_DURATION = 30000; // 30秒

async function getActiveAuctions() {
  const now = Date.now();
  
  if (cachedData && cacheTime && (now - cacheTime < CACHE_DURATION)) {
    return cachedData;
  }

  const response = await fetch('/auction-items/active-auctions');
  const result = await response.json();
  
  cachedData = result.data;
  cacheTime = now;
  
  return cachedData;
}
```

---

## 🎨 UI展示建议

### 1. 网格布局

```css
.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding: 20px;
}

.item-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s;
}

.item-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
```

### 2. 倒计时样式

```css
.countdown {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 10px;
  border-radius: 4px;
  text-align: center;
  font-weight: bold;
  font-size: 18px;
}

.delay-badge {
  background: #ff9800;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  margin-left: 8px;
}
```

### 3. 价格突出显示

```css
.current-price {
  color: #e53935;
  font-size: 24px;
  font-weight: bold;
}

.price-info p {
  margin: 8px 0;
  font-size: 14px;
  color: #666;
}
```

---

## 🔍 错误处理

### 网络错误处理

```javascript
try {
  const response = await fetch('/auction-items/active-auctions');
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  
  const result = await response.json();
  
  if (result.code !== 200) {
    throw new Error(result.message || '未知错误');
  }
  
  return result.data;
  
} catch (error) {
  console.error('加载活跃拍卖失败:', error);
  
  // 显示错误提示
  showNotification('加载失败，请稍后重试');
  
  // 返回空数据，避免页面崩溃
  return { auctions: [] };
}
```

### 空数据处理

```javascript
if (!auctions || auctions.length === 0) {
  return (
    <div className="empty-state">
      <p>当前没有正在进行的拍卖</p>
      <button onClick={() => navigate('/auctions')}>
        查看即将开始的拍卖
      </button>
    </div>
  );
}
```

---

## 📱 移动端适配

### 响应式布局

```css
/* 移动端：单列显示 */
@media (max-width: 768px) {
  .items-grid {
    grid-template-columns: 1fr;
    gap: 15px;
    padding: 10px;
  }

  .item-card {
    font-size: 14px;
  }

  .countdown {
    font-size: 16px;
  }
}

/* 平板：2列显示 */
@media (min-width: 769px) and (max-width: 1024px) {
  .items-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 桌面：3-4列显示 */
@media (min-width: 1025px) {
  .items-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}
```

---

## 🚀 最佳实践总结

### ✅ 推荐做法

1. **定时刷新**: 30秒间隔轮询，结合WebSocket实时更新
2. **倒计时处理**: 使用 `endTimeTimestamp` 计算，避免使用 `remainingSeconds`
3. **错误处理**: 网络异常时显示友好提示，返回空数据避免崩溃
4. **缓存策略**: 30秒内复用数据，减少API调用
5. **加载状态**: 显示loading动画，提升用户体验

### ❌ 避免做法

1. **频繁轮询**: 不要每秒刷新，浪费服务器资源
2. **忽略WebSocket**: 不要仅依赖轮询，应该使用WebSocket实时更新
3. **硬编码时间**: 不要假设时区，使用服务器时间戳
4. **忽略空数据**: 必须处理没有活跃拍卖的情况
5. **阻塞UI**: 不要同步等待API响应，影响用户操作

---

## 📞 技术支持

如有任何问题，请联系后端团队或查看相关文档：

- **API文档**: [接口完整文档](./api-reference.md)
- **WebSocket协议**: [WebSocket消息规范](./websocket-protocol.md)
- **时间同步**: [时间一致性实现指南](./time-sync-guide.md)

---

**文档维护**: 前端团队  
**最后更新**: 2026-06-10  
**版本**: v1.0
