# 单个拍卖活动的活跃拍品API使用指南

## 📖 接口概述

### 接口信息

**接口路径**: `GET /auction-items/auction/{auctionId}/active-items`  
**功能描述**: 查询指定拍卖活动下正在进行中的拍品列表  
**适用场景**: 
- 拍卖活动详情页
- 用户查看某个活动的实时拍品
- 实时拍卖大厅按活动筛选

---

## 🎯 接口特性

### 核心功能

1. **精准查询**: 只查询指定活动下的拍品
2. **自动筛选**: 只返回正在进行中（ACTIVE状态）的拍品
3. **完整信息**: 包含价格、倒计时、最高出价者、商品图片等完整信息
4. **时间同步**: 返回绝对时间戳，确保前后端时间一致
5. **商品关联**: 自动加载商品名称、图片和描述信息
6. **活动验证**: 自动验证活动是否存在，返回友好错误提示

---

## 📡 API调用

### 请求示例

```bash
GET /auction-items/auction/1/active-items
Authorization: Bearer {token}
```

**路径参数**:
- `auctionId`: 拍卖活动ID（必填）

### 响应示例

#### 成功响应（有活跃拍品）

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "itemId": 101,
      "title": "清代青花瓷瓶",
      "productName": "青花瓷瓶-清乾隆年间",
      "productImageUrl": "https://example.com/images/vase.jpg",
      "description": "这是一件精美的清代青花瓷瓶，工艺精湛...",
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
      "description": "明代紫砂壶，工艺精湛，保存完好...",
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
}
```

#### 成功响应（无活跃拍品）

```json
{
  "code": 200,
  "message": "success",
  "data": []
}
```

#### 错误响应（活动不存在）

```json
{
  "code": 404,
  "message": "拍卖活动不存在"
}
```

---

## 📊 响应字段说明

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
import { useParams } from 'react-router-dom';
import { fetch } from '@/api';

const AuctionActiveItemsPage = () => {
  const { auctionId } = useParams();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [auction, setAuction] = useState(null);

  useEffect(() => {
    loadActiveItems();
    // 每30秒刷新一次
    const timer = setInterval(loadActiveItems, 30000);
    return () => clearInterval(timer);
  }, [auctionId]);

  const loadActiveItems = async () => {
    try {
      setLoading(true);

      // 先加载活动基本信息
      const auctionResponse = await fetch(`/auctions/${auctionId}`);
      const auctionResult = await auctionResponse.json();
      if (auctionResult.code === 200) {
        setAuction(auctionResult.data);
      }

      // 加载活跃拍品
      const itemsResponse = await fetch(`/auction-items/auction/${auctionId}/active-items`);
      const itemsResult = await itemsResponse.json();

      if (itemsResult.code === 200) {
        setItems(itemsResult.data);
      } else if (itemsResult.code === 404) {
        // 活动不存在
        showNotification('拍卖活动不存在');
      }
    } catch (error) {
      console.error('加载活跃拍品失败:', error);
      showNotification('加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>加载中...</div>;

  if (!auction) return <div>拍卖活动不存在</div>;

  if (items.length === 0) {
    return (
      <div className="empty-state">
        <h1>{auction.title}</h1>
        <p>当前没有正在进行的拍品</p>
        <button onClick={() => navigate(`/auctions/${auctionId}/pending`)}>
          查看待开始拍品
        </button>
      </div>
    );
  }

  return (
    <div className="auction-active-items">
      <div className="auction-header">
        <h1>{auction.title}</h1>
        <p>{auction.description}</p>
      </div>

      <div className="items-grid">
        {items.map(item => (
          <ActiveItemCard key={item.itemId} item={item} />
        ))}
      </div>
    </div>
  );
};

// 活跃拍品卡片组件
const ActiveItemCard = ({ item }) => {
  const [remainingTime, setRemainingTime] = useState(null);
  const timerRef = React.useRef(null);

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
    timerRef.current = timer;

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
        if (result.data.endTimeTimestamp && timerRef.current) {
          timerRef.current.updateEndTime(result.data.endTimeTimestamp);
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
        onError={(e) => {
          e.target.src = '/images/default-product.png';
        }}
      />

      {/* 商品信息 */}
      <div className="item-info">
        <h3>{item.title}</h3>
        <p className="product-name">{item.productName}</p>
        
        {/* 价格信息 */}
        <div className="price-section">
          <p className="current-price">
            ¥{item.currentPrice}
          </p>
          <p className="price-label">当前价格</p>
          
          <div className="other-prices">
            <span>起拍: ¥{item.startPrice}</span>
            <span>加价: ¥{item.bidIncrement}</span>
          </div>
        </div>

        {/* 倒计时 */}
        <div className="countdown">
          {remainingTime ? (
            <p className="time-display">
              {formatTime(remainingTime)}
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

        {/* 统计信息 */}
        <div className="stats">
          <span>出价 {item.bidCount} 次</span>
          {item.highestBidder && (
            <span>最高: 用户{item.highestBidder}</span>
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
      </div>
    </div>
  );
};

// 时间格式化
function formatTime(time) {
  const { minutes, seconds } = time;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

export default AuctionActiveItemsPage;
```

---

### Vue组件

```vue
<template>
  <div class="auction-active-items">
    <!-- 活动头部信息 -->
    <div v-if="auction" class="auction-header">
      <h1>{{ auction.title }}</h1>
      <p>{{ auction.description }}</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      加载中...
    </div>

    <!-- 空状态 -->
    <div v-else-if="items.length === 0" class="empty-state">
      <h1 v-if="auction">{{ auction.title }}</h1>
      <p>当前没有正在进行的拍品</p>
      <button @click="goToPendingItems">
        查看待开始拍品
      </button>
    </div>

    <!-- 拍品列表 -->
    <div v-else class="items-grid">
      <ActiveItemCard 
        v-for="item in items"
        :key="item.itemId"
        :item="item"
      />
    </div>
  </div>
</template>

<script>
import ActiveItemCard from './ActiveItemCard.vue';

export default {
  name: 'AuctionActiveItemsPage',
  components: { ActiveItemCard },
  data() {
    return {
      auctionId: null,
      items: [],
      auction: null,
      loading: true,
      refreshTimer: null
    };
  },
  mounted() {
    this.auctionId = this.$route.params.auctionId;
    this.loadActiveItems();
    this.refreshTimer = setInterval(this.loadActiveItems, 30000);
  },
  beforeUnmount() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  },
  methods: {
    async loadActiveItems() {
      try {
        this.loading = true;

        // 先加载活动基本信息
        const auctionResponse = await this.$api.get(`/auctions/${this.auctionId}`);
        if (auctionResponse.code === 200) {
          this.auction = auctionResponse.data;
        } else if (auctionResponse.code === 404) {
          this.$message.error('拍卖活动不存在');
          return;
        }

        // 加载活跃拍品
        const itemsResponse = await this.$api.get(
          `/auction-items/auction/${this.auctionId}/active-items`
        );

        if (itemsResponse.code === 200) {
          this.items = itemsResponse.data;
        }
      } catch (error) {
        console.error('加载活跃拍品失败:', error);
        this.$message.error('加载失败，请稍后重试');
      } finally {
        this.loading = false;
      }
    },

    goToPendingItems() {
      this.$router.push(`/auctions/${this.auctionId}/pending`);
    }
  }
};
</script>

<style scoped>
.auction-header {
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px;
  margin-bottom: 20px;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  padding: 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-state button {
  margin-top: 20px;
  padding: 10px 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>
```

---

### ActiveItemCard.vue

```vue
<template>
  <div class="item-card">
    <!-- 商品图片 -->
    <img 
      :src="item.productImageUrl || '/images/default-product.png'" 
      :alt="item.productName"
      class="item-image"
      @error="handleImageError"
    />

    <!-- 商品信息 -->
    <div class="item-info">
      <h3>{{ item.title }}</h3>
      <p class="product-name">{{ item.productName }}</p>
      
      <!-- 价格信息 -->
      <div class="price-section">
        <p class="current-price">
          ¥{{ item.currentPrice }}
        </p>
        <p class="price-label">当前价格</p>
        
        <div class="other-prices">
          <span>起拍: ¥{{ item.startPrice }}</span>
          <span>加价: ¥{{ item.bidIncrement }}</span>
        </div>
      </div>

      <!-- 倒计时 -->
      <div class="countdown">
        <template v-if="remainingTime">
          <p class="time-display">
            {{ formatTime(remainingTime) }}
          </p>
        </template>
        <template v-else>
          <p>计算中...</p>
        </template>
        
        <span 
          v-if="item.delayCount > 0" 
          class="delay-badge"
        >
          已延时 {{ item.delayCount }} 次
        </span>
      </div>

      <!-- 统计信息 -->
      <div class="stats">
        <span>出价 {{ item.bidCount }} 次</span>
        <span v-if="item.highestBidder">
          最高: 用户{{ item.highestBidder }}
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
    </div>
  </div>
</template>

<script>
export default {
  name: 'ActiveItemCard',
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
          if (response.data.endTimeTimestamp && this.timer) {
            this.timer.updateEndTime(response.data.endTimeTimestamp);
          }
          
          // 触发父组件刷新
          this.$emit('bid-placed');
        }
      } catch (error) {
        this.$message.error('出价失败: ' + error.message);
      }
    },

    formatTime(time) {
      const { minutes, seconds } = time;
      return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    },

    handleImageError(event) {
      event.target.src = '/images/default-product.png';
    }
  }
};
</script>

<style scoped>
.item-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.item-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.item-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.item-info {
  padding: 15px;
}

.current-price {
  font-size: 24px;
  font-weight: bold;
  color: #e53935;
  margin: 10px 0;
}

.countdown {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 10px;
  border-radius: 4px;
  text-align: center;
  margin: 10px 0;
}

.time-display {
  font-size: 18px;
  font-weight: bold;
}

.delay-badge {
  background: #ff9800;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  margin-left: 8px;
}

.bid-button {
  width: 100%;
  padding: 12px;
  background: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.bid-button:hover {
  background: #45a049;
}
</style>
```

---

## 🔧 实用工具函数

### 倒计时管理器（已在前文文档中定义）

```javascript
// 复用前文的 AuctionTimer 类
class AuctionTimer {
  constructor(endTimeTimestamp, onTick, onEnd) {
    this.endTime = new Date(endTimeTimestamp);
    this.onTick = onTick;
    this.onEnd = onEnd;
    this.timer = null;
  }

  start() {
    this.update();
    this.timer = setInterval(() => this.update(), 1000);
  }

  stop() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  update() {
    const now = Date.now();
    const remaining = this.endTime.getTime() - now;

    if (remaining <= 0) {
      this.stop();
      if (this.onEnd) this.onEnd();
      return;
    }

    const totalSeconds = Math.floor(remaining / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    if (this.onTick) {
      this.onTick({ totalSeconds, minutes, seconds });
    }
  }

  updateEndTime(newEndTimeTimestamp) {
    this.endTime = new Date(newEndTimeTimestamp);
    this.update();
  }
}
```

---

## 🎨 UI样式建议

### 拍品网格布局

```css
.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  padding: 20px;
  margin: 0 auto;
  max-width: 1400px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .items-grid {
    grid-template-columns: 1fr;
    gap: 15px;
    padding: 10px;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .items-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
```

### 拍品卡片样式

```css
.item-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
  transition: all 0.3s ease;
}

.item-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
}

.item-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.item-card:hover .item-image {
  transform: scale(1.05);
}
```

### 倒计时动画

```css
.countdown {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  color: white;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
}

.countdown.urgent {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  animation: urgent-pulse 1s infinite;
}

@keyframes urgent-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.02);
  }
}
```

---

## ⚡ 性能优化

### 1. 数据缓存

```javascript
// 30秒内不重复请求
let lastFetch = 0;
const CACHE_DURATION = 30000;

async function getActiveItems(auctionId) {
  const now = Date.now();
  
  if (now - lastFetch < CACHE_DURATION) {
    return cachedData;
  }

  const response = await fetch(`/auction-items/auction/${auctionId}/active-items`);
  const result = await response.json();
  
  lastFetch = now;
  cachedData = result.data;
  
  return result.data;
}
```

### 2. 虚拟滚动

```javascript
// 当拍品数量很多时使用虚拟滚动
import { FixedSizeList } from 'react-window';

const ItemList = ({ items }) => (
  <FixedSizeList
    height={600}
    itemCount={items.length}
    itemSize={350}
    width="100%"
  >
    {({ index, style }) => (
      <div style={style}>
        <ActiveItemCard item={items[index]} />
      </div>
    )}
  </FixedSizeList>
);
```

### 3. 图片懒加载

```javascript
// 使用Intersection Observer实现图片懒加载
const ImageLazyLoad = ({ src, alt }) => {
  const imgRef = useRef(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setIsVisible(true);
        observer.disconnect();
      }
    });

    if (imgRef.current) {
      observer.observe(imgRef.current);
    }

    return () => observer.disconnect();
  }, []);

  return (
    <img
      ref={imgRef}
      src={isVisible ? src : '/images/placeholder.png'}
      alt={alt}
      loading="lazy"
    />
  );
};
```

---

## 🔍 错误处理

### 完善的错误处理逻辑

```javascript
const loadActiveItems = async (auctionId) => {
  try {
    // 验证参数
    if (!auctionId || isNaN(auctionId)) {
      throw new Error('无效的活动ID');
    }

    const response = await fetch(`/auction-items/auction/${auctionId}/active-items`);

    // 处理不同的HTTP状态码
    switch (response.status) {
      case 200:
        const result = await response.json();
        if (result.code === 200) {
          setItems(result.data);
          setAuctionLoaded(true);
        } else {
          throw new Error(result.message || '未知错误');
        }
        break;

      case 404:
        setAuctionNotFound(true);
        showNotification('拍卖活动不存在', 'error');
        break;

      case 500:
        throw new Error('服务器内部错误');

      default:
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

  } catch (error) {
    console.error('加载活跃拍品失败:', error);
    
    // 显示用户友好的错误信息
    if (error.message.includes('网络')) {
      showNotification('网络连接失败，请检查网络设置', 'error');
    } else {
      showNotification('加载失败，请稍后重试', 'error');
    }

    // 设置错误状态
    setError(error.message);
  } finally {
    setLoading(false);
  }
};
```

---

## 📱 移动端优化

### 触摸优化

```css
/* 增大点击区域 */
.bid-button {
  padding: 16px 24px;
  font-size: 18px;
  min-height: 48px;
  /* 适合手指点击 */
}

/* 响应式图片 */
.item-image {
  width: 100%;
  height: auto;
  max-height: 250px;
  object-fit: cover;
}

@media (max-width: 768px) {
  .item-card {
    margin-bottom: 15px;
  }

  .countdown {
    font-size: 20px;
    padding: 15px;
  }
}
```

---

## 🚀 最佳实践总结

### ✅ 推荐做法

1. **活动验证**: 先验证活动存在，再查询拍品
2. **定时刷新**: 30秒间隔自动刷新列表
3. **WebSocket结合**: 结合WebSocket实时更新价格和倒计时
4. **倒计时处理**: 使用endTimeTimestamp计算，避免时间偏差
5. **错误处理**: 完善的错误处理，友好的用户提示
6. **空状态**: 当没有活跃拍品时，提供引导操作

### ❌ 避免做法

1. **忽略活动验证**: 不检查活动是否存在直接查询
2. **过于频繁刷新**: 每秒刷新，浪费资源
3. **硬编码时间**: 使用remainingSeconds而非endTimeTimestamp
4. **忽略空数据**: 不处理没有活跃拍品的情况
5. **阻塞UI**: 同步等待API响应，影响用户体验

---

## 📞 相关接口

### 配套接口

1. **获取活动详情**:
   - `GET /auctions/{auctionId}`
   - 返回活动完整信息

2. **查询待开始拍品**:
   - `GET /auction-items/auction/{auctionId}/pending-items`
   - 返回活动下待开始的拍品

3. **查询所有拍品**:
   - `GET /auction-items/auction/{auctionId}`
   - 返回活动下所有拍品（所有状态）

---

## 🔗 完整使用流程

```javascript
// 完整的使用流程示例
const AuctionPage = () => {
  // 1. 进入页面，加载活动信息
  useEffect(() => {
    loadAuctionInfo();
    loadActiveItems();
  }, []);

  // 2. 定时刷新活跃拍品（30秒）
  useEffect(() => {
    const timer = setInterval(loadActiveItems, 30000);
    return () => clearInterval(timer);
  }, []);

  // 3. 建立WebSocket连接，实时更新
  useEffect(() => {
    const ws = new WebSocket(`ws://localhost:8080/auction/auction/${auctionId}`);
    
    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      switch (message.type) {
        case 'NEW_BID':
          // 实时更新价格
          updateItemPrice(message.data);
          break;
        case 'AUCTION_DELAYED':
          // 实时更新倒计时
          updateItemCountdown(message.data);
          break;
        case 'AUCTION_ENDED':
          // 从列表中移除
          removeItem(message.data.itemId);
          break;
      }
    };

    return () => ws.close();
  }, [auctionId]);

  // 4. 用户操作
  const handleBid = async (itemId, amount) => {
    // 出价后更新数据
    await placeBid(itemId, amount);
    // 刷新列表
    loadActiveItems();
  };
};
```

---

**文档维护**: 前端团队  
**最后更新**: 2026-06-10  
**版本**: v2.0 (单活动查询版本)
