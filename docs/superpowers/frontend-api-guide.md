# 拍卖系统前端API文档 - 时间一致性实现指南

## 📖 文档概述

本文档详细说明如何正确处理拍卖系统中的时间同步问题，确保前端展示的剩余时间与后端完全一致。

**版本**: v1.0  
**最后更新**: 2026-06-10  
**适用范围**: 所有拍卖相关的前端开发

---

## 🎯 核心原则

### ✅ 正确做法

```javascript
// ✅ 基于服务器返回的绝对时间计算剩余时间
const endTime = response.data.endTime;              // "2026-06-10T15:30:00"
const endTimeTimestamp = response.data.endTimeTimestamp;  // 1686427500000

// 计算剩余时间
const remaining = endTimeTimestamp - Date.now();
const remainingSeconds = Math.floor(remaining / 1000);
```

### ❌ 错误做法

```javascript
// ❌ 不要使用相对时间倒计时（会立即过期）
const remainingSeconds = response.data.remainingSeconds;  // 300
setInterval(() => {
  remainingSeconds--;  // 网络延迟导致不准确！
}, 1000);
```

---

## 📡 API接口说明

### 1. 获取拍品实时价格和时间

**接口**: `GET /auction-items/{id}/price`

**请求示例**:
```bash
GET /auction-items/123/price
Authorization: Bearer {token}
```

**响应结构**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "auctionItemId": 123,
    "auctionId": 456,
    "currentPrice": 1500.00,
    "highestBidder": 789,
    "bidCount": 5,
    "status": "ACTIVE",
    
    // ⭐ 时间相关字段（重点）
    "endTime": "2026-06-10T15:30:00",           // 推荐：ISO 8601格式
    "endTimeTimestamp": 1686427500000,          // 推荐：Unix时间戳（毫秒）
    "remainingSeconds": 300,                    // 已废弃：不要使用！
    
    "startPrice": 1000.00,
    "bidIncrement": 50.00,
    "maxPrice": 5000.00,
    "isBiddable": true
  }
}
```

**字段说明**:

| 字段 | 类型 | 推荐度 | 说明 |
|------|------|--------|------|
| `endTime` | String | ⭐⭐⭐⭐⭐ | ISO 8601格式的结束时间，**强烈推荐使用** |
| `endTimeTimestamp` | Long | ⭐⭐⭐⭐ | Unix时间戳（毫秒），便于计算 |
| `remainingSeconds` | Long | ⭐ 已废弃 | 相对时间，**不要使用**，仅用于老旧客户端兼容 |

---

### 2. 开始拍品

**接口**: `POST /auction-items/{id}/start`

**请求示例**:
```bash
POST /auction-items/123/start
Authorization: Bearer {token}
Content-Type: application/json

{
  "durationMinutes": 10,           // 必填：持续时间（分钟）
  "overrideDelaySeconds": 15       // 可选：覆盖默认延时秒数
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**WebSocket通知**:
拍品开始后，服务器会主动推送WebSocket消息（详见下文WebSocket章节）。

---

### 3. 出价

**接口**: `POST /auction-items/{id}/bid`

**请求示例**:
```bash
POST /auction-items/123/bid
Authorization: Bearer {token}
Content-Type: application/json

{
  "amount": 1550.00
}
```

**响应结构**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "bidId": 789,
    "currentPrice": 1550.00,
    "yourRank": 1,
    "isLeading": true,
    
    // ⭐ 时间字段
    "endTime": "2026-06-10T15:30:00",
    "endTimeTimestamp": 1686427500000,
    "remainingMs": 300000,
    
    "message": "出价成功"
  }
}
```

**重要**: 每次出价后，**必须更新前端的endTime**，因为可能触发了延时机制。

---

## 🔌 WebSocket消息格式

### 连接地址

```
ws://localhost:8080/auction/item/{auctionItemId}
```

### 消息类型总览

| 消息类型 | 说明 | 包含时间信息 |
|---------|------|------------|
| `AUCTION_STARTED` | 拍品开始 | ✅ endTime |
| `AUCTION_DELAYED` | 拍品延时 | ✅ newEndTime |
| `AUCTION_ENDED` | 拍品结束 | ❌ 无 |
| `NEW_BID` | 新出价 | ❌ 无 |
| `ITEM_PRICE_UPDATE` | 价格更新 | ❌ 无 |

---

### 消息1: 拍品开始 (AUCTION_STARTED)

**触发时机**: 管理员调用开始接口后

**数据结构**:
```json
{
  "type": "AUCTION_STARTED",
  "data": {
    "auctionItemId": 123,
    "endTime": "2026-06-10T15:30:00",           // ISO 8601格式
    "endTimeTimestamp": 1686427500000,            // Unix时间戳
    "message": "拍品竞拍已开始"
  }
}
```

**前端处理**:
```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'AUCTION_STARTED') {
    const { endTime, endTimeTimestamp } = message.data;
    
    // 创建倒计时
    const timer = new AuctionTimer(endTimeTimestamp);
    timer.start();
    
    // 显示拍品状态
    showAuctionStatus('ACTIVE');
  }
};
```

---

### 消息2: 拍品延时 (AUCTION_DELAYED)

**触发时机**: 在结束前N秒内出价时触发（自动延长）

**数据结构**:
```json
{
  "type": "AUCTION_DELAYED",
  "data": {
    "auctionItemId": 123,
    "newEndTime": "2026-06-10T15:30:15",        // 新的结束时间
    "newEndTimeTimestamp": 1686427515000,       // 新的时间戳
    "delayCount": 1,                             // 当前延时次数
    "message": "拍品竞拍时间延长！"
  }
}
```

**前端处理**:
```javascript
if (message.type === 'AUCTION_DELAYED') {
  const { newEndTimeTimestamp, delayCount } = message.data;
  
  // ⭐ 关键：更新倒计时的endTime
  timer.updateEndTime(newEndTimeTimestamp);
  
  // 显示延时通知
  showDelayNotification(delayCount);
}
```

---

### 消息3: 拍品结束 (AUCTION_ENDED)

**触发时机**: 定时任务检测到拍品到期

**数据结构**:
```json
{
  "type": "AUCTION_ENDED",
  "data": {
    "auctionItemId": 123,
    "winnerId": 456,                // 获胜用户ID（如有）
    "finalPrice": 2000.00,          // 成交价格
    "hasWinner": true               // 是否成交
  }
}
```

**前端处理**:
```javascript
if (message.type === 'AUCTION_ENDED') {
  const { winnerId, finalPrice, hasWinner } = message.data;
  
  // 停止倒计时
  timer.stop();
  
  // 显示结果
  if (hasWinner) {
    showWinner(winnerId, finalPrice);
  } else {
    showNoWinner();
  }
}
```

---

### 消息4: 新出价 (NEW_BID)

**触发时机**: 任何用户出价后

**数据结构**:
```json
{
  "type": "NEW_BID",
  "data": {
    "auctionItemId": 123,
    "bidId": 789,
    "userId": 456,
    "username": "用户***",              // 脱敏用户名
    "amount": 1600.00,
    "isAutoBid": false,
    "bidTime": "2026-06-10T15:25:30"
  }
}
```

**前端处理**:
```javascript
if (message.type === 'NEW_BID') {
  const { amount, username } = message.data;
  
  // 更新价格显示
  updatePriceDisplay(amount);
  
  // 显示出价通知
  showBidNotification(username, amount);
}
```

---

## 💻 前端实现示例

### 完整的倒计时类实现

```javascript
/**
 * 拍卖倒计时管理器
 * 基于服务器返回的绝对时间计算剩余时间
 */
class AuctionTimer {
  constructor(endTimeTimestamp, onTick, onEnd) {
    this.endTime = new Date(endTimeTimestamp);
    this.onTick = onTick;        // 每秒回调
    this.onEnd = onEnd;          // 结束回调
    this.timer = null;
    this.isRunning = false;
  }

  /**
   * 启动倒计时
   */
  start() {
    if (this.isRunning) return;
    
    this.isRunning = true;
    this.update();  // 立即执行一次
    
    this.timer = setInterval(() => {
      this.update();
    }, 1000);
  }

  /**
   * 停止倒计时
   */
  stop() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.isRunning = false;
  }

  /**
   * 更新倒计时（每秒执行）
   */
  update() {
    const now = Date.now();
    const remaining = this.endTime.getTime() - now;

    if (remaining <= 0) {
      // 倒计时结束
      this.stop();
      if (this.onEnd) {
        this.onEnd();
      }
      return;
    }

    // 计算剩余时间
    const totalSeconds = Math.floor(remaining / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    // 回调通知
    if (this.onTick) {
      this.onTick({
        totalSeconds,
        minutes,
        seconds,
        remainingMs: remaining
      });
    }
  }

  /**
   * 更新结束时间（用于延时场景）
   */
  updateEndTime(newEndTimeTimestamp) {
    this.endTime = new Date(newEndTimeTimestamp);
    this.update();  // 立即刷新显示
  }
}
```

---

### React组件示例

```jsx
import React, { useState, useEffect, useCallback } from 'react';
import { fetch } from '@/api';

const AuctionItem = ({ itemId }) => {
  const [itemData, setItemData] = useState(null);
  const [remainingTime, setRemainingTime] = useState(null);
  const [timer, setTimer] = useState(null);

  // 初始化：获取拍品信息
  useEffect(() => {
    loadAuctionItem();
  }, [itemId]);

  // 加载拍品数据
  const loadAuctionItem = async () => {
    try {
      const response = await fetch(`/auction-items/${itemId}/price`);
      const result = await response.json();
      
      if (result.code === 200) {
        setItemData(result.data);
        
        // ✅ 创建倒计时
        if (result.data.endTimeTimestamp) {
          const auctionTimer = new AuctionTimer(
            result.data.endTimeTimestamp,
            (time) => setRemainingTime(time),
            () => handleAuctionEnd()
          );
          auctionTimer.start();
          setTimer(auctionTimer);
        }
      }
    } catch (error) {
      console.error('加载拍品信息失败:', error);
    }
  };

  // WebSocket监听
  useEffect(() => {
    const ws = new WebSocket(`ws://localhost:8080/auction/item/${itemId}`);

    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      switch (message.type) {
        case 'AUCTION_STARTED':
          handleAuctionStarted(message.data);
          break;
        case 'AUCTION_DELAYED':
          handleAuctionDelayed(message.data);
          break;
        case 'AUCTION_ENDED':
          handleAuctionEnd(message.data);
          break;
        case 'NEW_BID':
          handleNewBid(message.data);
          break;
      }
    };

    return () => ws.close();
  }, [itemId]);

  // 处理拍品开始
  const handleAuctionStarted = useCallback((data) => {
    const newTimer = new AuctionTimer(
      data.endTimeTimestamp,
      (time) => setRemainingTime(time),
      () => handleAuctionEnd()
    );
    newTimer.start();
    setTimer(newTimer);
  }, []);

  // 处理拍品延时
  const handleAuctionDelayed = useCallback((data) => {
    // ⭐ 关键：更新endTime
    if (timer) {
      timer.updateEndTime(data.newEndTimeTimestamp);
    }
    
    // 显示延时通知
    showNotification(`延时第 ${data.delayCount} 次！`);
  }, [timer]);

  // 处理拍品结束
  const handleAuctionEnd = useCallback((data) => {
    if (timer) {
      timer.stop();
    }
    showNotification('拍卖已结束');
  }, [timer]);

  // 处理新出价
  const handleNewBid = useCallback((data) => {
    setItemData(prev => ({
      ...prev,
      currentPrice: data.amount,
      bidCount: prev.bidCount + 1
    }));
  }, []);

  // 渲染倒计时
  const renderCountdown = () => {
    if (!remainingTime) return '加载中...';
    
    const { minutes, seconds } = remainingTime;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  if (!itemData) return <div>加载中...</div>;

  return (
    <div className="auction-item">
      <h2>{itemData.title}</h2>
      
      {/* 价格信息 */}
      <div className="price-info">
        <p>当前价格: ¥{itemData.currentPrice}</p>
        <p>出价次数: {itemData.bidCount}</p>
        <p>起拍价: ¥{itemData.startPrice}</p>
      </div>

      {/* 倒计时 */}
      <div className="countdown">
        <p>剩余时间: {renderCountdown()}</p>
      </div>

      {/* 出价按钮 */}
      {itemData.isBiddable && (
        <button onClick={() => handleBid()}>
          出价
        </button>
      )}
    </div>
  );
};
```

---

### Vue组件示例

```vue
<template>
  <div class="auction-item">
    <h2>{{ itemData.title }}</h2>
    
    <!-- 价格信息 -->
    <div class="price-info">
      <p>当前价格: ¥{{ itemData.currentPrice }}</p>
      <p>出价次数: {{ itemData.bidCount }}</p>
      <p>起拍价: ¥{{ itemData.startPrice }}</p>
    </div>

    <!-- 倒计时 -->
    <div class="countdown">
      <p>剩余时间: {{ formattedCountdown }}</p>
    </div>

    <!-- 出价按钮 -->
    <button v-if="itemData.isBiddable" @click="handleBid">
      出价
    </button>
  </div>
</template>

<script>
export default {
  name: 'AuctionItem',
  props: {
    itemId: {
      type: Number,
      required: true
    }
  },
  data() {
    return {
      itemData: null,
      remainingTime: null,
      timer: null,
      ws: null
    };
  },
  computed: {
    formattedCountdown() {
      if (!this.remainingTime) return '加载中...';
      
      const { minutes, seconds } = this.remainingTime;
      return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }
  },
  mounted() {
    this.loadAuctionItem();
    this.connectWebSocket();
  },
  beforeUnmount() {
    if (this.timer) {
      this.timer.stop();
    }
    if (this.ws) {
      this.ws.close();
    }
  },
  methods: {
    async loadAuctionItem() {
      try {
        const response = await this.$api.get(`/auction-items/${this.itemId}/price`);
        
        if (response.code === 200) {
          this.itemData = response.data;
          
          // ✅ 创建倒计时
          if (response.data.endTimeTimestamp) {
            this.timer = new AuctionTimer(
              response.data.endTimeTimestamp,
              (time) => {
                this.remainingTime = time;
              },
              () => {
                this.handleAuctionEnd();
              }
            );
            this.timer.start();
          }
        }
      } catch (error) {
        console.error('加载拍品信息失败:', error);
      }
    },

    connectWebSocket() {
      this.ws = new WebSocket(`ws://localhost:8080/auction/item/${this.itemId}`);
      
      this.ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        
        switch (message.type) {
          case 'AUCTION_STARTED':
            this.handleAuctionStarted(message.data);
            break;
          case 'AUCTION_DELAYED':
            this.handleAuctionDelayed(message.data);
            break;
          case 'AUCTION_ENDED':
            this.handleAuctionEnd(message.data);
            break;
          case 'NEW_BID':
            this.handleNewBid(message.data);
            break;
        }
      };
    },

    handleAuctionStarted(data) {
      this.timer = new AuctionTimer(
        data.endTimeTimestamp,
        (time) => {
          this.remainingTime = time;
        },
        () => {
          this.handleAuctionEnd();
        }
      );
      this.timer.start();
    },

    handleAuctionDelayed(data) {
      // ⭐ 关键：更新endTime
      if (this.timer) {
        this.timer.updateEndTime(data.newEndTimeTimestamp);
      }
      
      // 显示延时通知
      this.$message.success(`延时第 ${data.delayCount} 次！`);
    },

    handleAuctionEnd() {
      if (this.timer) {
        this.timer.stop();
      }
      this.$message.info('拍卖已结束');
    },

    handleNewBid(data) {
      this.itemData.currentPrice = data.amount;
      this.itemData.bidCount += 1;
    },

    async handleBid() {
      // 出价逻辑
      try {
        const response = await this.$api.post(`/auction-items/${this.itemId}/bid`, {
          amount: this.itemData.currentPrice + this.itemData.bidIncrement
        });
        
        if (response.code === 200) {
          // ⭐ 出价后可能触发延时，更新endTime
          if (response.data.endTimeTimestamp) {
            this.timer.updateEndTime(response.data.endTimeTimestamp);
          }
          
          this.$message.success('出价成功');
        }
      } catch (error) {
        this.$message.error('出价失败: ' + error.message);
      }
    }
  }
};
</script>
```

---

## 🔧 工具函数库

### 时间格式化工具

```javascript
/**
 * 时间工具类
 */
class TimeUtils {
  /**
   * 格式化倒计时显示
   */
  static formatCountdown(totalSeconds) {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    if (hours > 0) {
      return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  }

  /**
   * ISO时间字符串转时间戳
   */
  static isoToTimestamp(isoString) {
    return new Date(isoString).getTime();
  }

  /**
   * 计算剩余时间（秒）
   */
  static getRemainingSeconds(endTimeTimestamp) {
    const now = Date.now();
    const remaining = endTimeTimestamp - now;
    return Math.max(0, Math.floor(remaining / 1000));
  }

  /**
   * 判断拍品是否已结束
   */
  static isEnded(endTimeTimestamp) {
    return endTimeTimestamp <= Date.now();
  }
}
```

---

## ⚠️ 常见问题解答

### Q1: 为什么不能用remainingSeconds？

**A**: `remainingSeconds` 是后端计算的**相对时间**，从后端响应到前端开始倒计时的这段时间（网络延迟+处理时间），剩余时间已经减少了。这会导致前端显示的时间不准确。

**正确做法**:
```javascript
// ✅ 使用endTime
const endTime = new Date(response.data.endTimeTimestamp);
const remaining = endTime - Date.now();

// ❌ 不要使用remainingSeconds
const remaining = response.data.remainingSeconds;  // 已经过期了！
```

---

### Q2: WebSocket断线了怎么办？

**A**: 实现断线重连机制，重连后重新查询最新的endTime。

```javascript
class ReconnectingWebSocket {
  constructor(url) {
    this.url = url;
    this.ws = null;
    this.reconnectDelay = 1000;
    this.maxReconnectDelay = 30000;
  }

  connect() {
    this.ws = new WebSocket(this.url);

    this.ws.onopen = () => {
      console.log('WebSocket已连接');
      this.reconnectDelay = 1000;  // 重置重连延迟
    };

    this.ws.onclose = () => {
      console.log('WebSocket断线，尝试重连...');
      setTimeout(() => {
        this.reconnect();
      }, this.reconnectDelay);
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket错误:', error);
    };
  }

  reconnect() {
    this.reconnectDelay = Math.min(
      this.reconnectDelay * 2,
      this.maxReconnectDelay
    );
    this.connect();
    
    // 重连后重新查询数据
    this.fetchLatestData();
  }

  async fetchLatestData() {
    // 重新获取最新的endTime
    const response = await fetch(`/auction-items/${itemId}/price`);
    const data = await response.json();
    
    // 更新倒计时
    if (timer) {
      timer.updateEndTime(data.data.endTimeTimestamp);
    }
  }
}
```

---

### Q3: 如何处理时区问题？

**A**: 服务器返回的ISO 8601格式字符串包含时区信息，JavaScript的`Date`对象会自动处理。

```javascript
// 服务器返回： "2026-06-10T15:30:00"
const endTime = new Date("2026-06-10T15:30:00");

// Date对象会自动转换为本地时区
console.log(endTime.toLocaleString());  // 自动显示为本地时间

// 获取时间戳（UTC毫秒数）
const timestamp = endTime.getTime();
```

**推荐**: 直接使用 `endTimeTimestamp`（Unix时间戳），避免时区转换问题。

---

### Q4: 出价后需要立即刷新endTime吗？

**A**: 是的！每次出价后都可能触发延时机制，需要立即更新endTime。

```javascript
async function placeBid(itemId, amount) {
  const response = await fetch(`/auction-items/${itemId}/bid`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount })
  });
  
  const result = await response.json();
  
  if (result.code === 200) {
    // ⭐ 关键：出价后立即更新endTime
    const { endTimeTimestamp } = result.data;
    if (timer && endTimeTimestamp) {
      timer.updateEndTime(endTimeTimestamp);
    }
    
    // 更新价格显示
    updatePrice(result.data.currentPrice);
  }
}
```

---

### Q5: 如何测试倒计时准确性？

**A**: 模拟网络延迟和时钟偏差。

```javascript
// 模拟客户端时钟快10秒
const clockOffset = 10000;

function getRemainingTime(endTimeTimestamp) {
  const serverTime = Date.now();
  const clientTime = serverTime + clockOffset;
  
  // 基于服务器时间计算，不受客户端时钟影响
  const remaining = endTimeTimestamp - serverTime;
  return Math.max(0, Math.floor(remaining / 1000));
}

// 测试用例
test('倒计时计算准确性', () => {
  const endTime = Date.now() + 60000;  // 1分钟后
  const remaining = getRemainingTime(endTime);
  
  expect(remaining).toBe(60);  // 应该准确为60秒
});
```

---

## 📊 性能优化建议

### 1. 避免频繁创建定时器

```javascript
// ❌ 错误：每次更新都创建新的定时器
function updateCountdown() {
  setInterval(() => {
    // ...
  }, 1000);
}

// ✅ 正确：复用同一个定时器
class AuctionTimer {
  updateEndTime(newEndTime) {
    this.endTime = new Date(newEndTime);
    this.update();  // 立即刷新，无需创建新定时器
  }
}
```

### 2. 使用requestAnimationFrame优化渲染

```javascript
class OptimizedTimer {
  start() {
    const tick = () => {
      this.update();
      this.animationFrameId = requestAnimationFrame(tick);
    };
    this.animationFrameId = requestAnimationFrame(tick);
  }

  stop() {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
  }
}
```

### 3. 节流WebSocket消息处理

```javascript
import { throttle } from 'lodash';

const handleNewBid = throttle((data) => {
  updatePriceDisplay(data.amount);
}, 1000);  // 最多1秒更新一次
```

---

## 🎓 最佳实践总结

### ✅ 推荐做法

1. **使用绝对时间**: 始终基于 `endTimeTimestamp` 计算剩余时间
2. **监听WebSocket**: 实时处理延时和结束事件
3. **封装倒计时类**: 统一管理倒计时逻辑
4. **断线重连**: 实现自动重连和数据恢复
5. **出价后刷新**: 每次出价后更新endTime

### ❌ 避免做法

1. **使用相对时间**: 不要依赖 `remainingSeconds` 字段
2. **忽略延时**: 必须处理 `AUCTION_DELAYED` 消息
3. **频繁请求**: 不要轮询API，使用WebSocket
4. **硬编码时间**: 不要假设时区，使用服务器时间
5. **忽略错误**: 必须处理网络异常和断线情况

---

## 📞 技术支持

如有任何问题，请联系后端团队或查看相关文档：

- **后端API文档**: [后端接口文档](./backend-api-reference.md)
- **WebSocket协议**: [WebSocket消息规范](./websocket-protocol.md)
- **常见问题**: [FAQ汇总](./faq.md)

---

**文档维护**: 前端团队  
**反馈渠道**: [GitHub Issues](https://github.com/your-repo/issues)  
**更新频率**: 每次API变更后更新
