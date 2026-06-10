# 直播间聊天功能接口说明

## 功能概述

本系统提供完整的直播间聊天功能，包括WebSocket实时通信和REST API接口两种方式。

### 核心功能
- ✅ WebSocket实时聊天
- ✅ 用户加入/离开直播间
- ✅ 在线用户管理
- ✅ 聊天历史记录查询
- ✅ 消息发送与管理
- ✅ 在线人数统计

---

## 一、WebSocket连接

### 1.1 连接格式

```
ws://host/ws/live/{auctionId}?userId={userId}&username={username}
```

### 1.2 连接参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| auctionId | Long | 是 | 拍卖活动ID |
| userId | Long | 是 | 用户ID |
| username | String | 是 | 用户名（URL编码） |

### 1.3 WebSocket消息类型

#### 客户端发送消息

**1. 聊天消息 (CHAT)**
```json
{
  "type": "CHAT",
  "content": "这件拍品真不错！"
}
```

**2. 心跳检测 (PING)**
```json
{
  "type": "PING"
}
```

**3. 获取用户列表 (GET_USER_LIST)**
```json
{
  "type": "GET_USER_LIST"
}
```

#### 服务端推送消息

**1. 聊天消息 (CHAT_MESSAGE)**
```json
{
  "type": "CHAT_MESSAGE",
  "data": {
    "auctionId": 123,
    "messageId": 456,
    "userId": 789,
    "username": "张三***",
    "content": "这件拍品真不错！",
    "timestamp": 1700000000000
  },
  "timestamp": 1700000000000
}
```

**2. 用户加入 (USER_JOIN)**
```json
{
  "type": "USER_JOIN",
  "data": {
    "auctionId": 123,
    "userId": 789,
    "username": "张三",
    "onlineCount": 25,
    "timestamp": 1700000000000
  },
  "timestamp": 1700000000000
}
```

**3. 用户离开 (USER_LEAVE)**
```json
{
  "type": "USER_LEAVE",
  "data": {
    "auctionId": 123,
    "userId": 789,
    "onlineCount": 24,
    "timestamp": 1700000000000
  },
  "timestamp": 1700000000000
}
```

**4. 系统消息 (SYSTEM_MESSAGE)**
```json
{
  "type": "SYSTEM_MESSAGE",
  "data": {
    "auctionId": 123,
    "message": "欢迎 张三 加入直播间！",
    "timestamp": 1700000000000
  },
  "timestamp": 1700000000000
}
```

**5. 聊天历史 (CHAT_HISTORY)**
```json
{
  "type": "CHAT_HISTORY",
  "data": {
    "auctionId": 123,
    "messages": [
      {
        "messageId": 456,
        "userId": 789,
        "username": "张三***",
        "content": "大家好！",
        "timestamp": "2024-01-01T12:00:00"
      }
    ],
    "total": 20
  },
  "timestamp": 1700000000000
}
```

---

## 二、REST API接口

### 2.1 发送聊天消息

**接口：** `POST /chat/send`

**限流：** 每分钟最多60次请求

**请求体：**
```json
{
  "auctionId": 123,
  "content": "这件拍品真不错！"
}
```

**请求参数说明：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| auctionId | Long | 是 | 拍卖活动ID |
| content | String | 是 | 消息内容（不超过500字） |

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "messageId": 456,
    "auctionId": 123,
    "userId": 789,
    "username": "张三***",
    "content": "这件拍品真不错！",
    "messageType": 1,
    "createdAt": "2024-01-01T12:00:00"
  },
  "timestamp": 1700000000000
}
```

### 2.2 获取聊天历史记录

**接口：** `GET /chat/history/{auctionId}`

**路径参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| auctionId | Long | 是 | 拍卖活动ID |

**查询参数：**
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码（从1开始） |
| size | Integer | 否 | 20 | 每页大小（最大100） |
| messageType | Integer | 否 | 全部 | 消息类型（1-用户消息，2-系统消息） |

**请求示例：**
```
GET /chat/history/123?page=1&size=20&messageType=1
```

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "auctionId": 123,
    "total": 150,
    "messages": [
      {
        "messageId": 456,
        "auctionId": 123,
        "userId": 789,
        "username": "张三***",
        "content": "这件拍品真不错！",
        "messageType": 1,
        "createdAt": "2024-01-01T12:00:00"
      },
      {
        "messageId": 455,
        "auctionId": 123,
        "userId": 788,
        "username": "李四***",
        "content": "同意楼上！",
        "messageType": 1,
        "createdAt": "2024-01-01T11:59:00"
      }
    ]
  },
  "timestamp": 1700000000000
}
```

### 2.3 获取在线用户列表

**接口：** `GET /chat/online-users/{auctionId}`

**路径参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| auctionId | Long | 是 | 拍卖活动ID |

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "auctionId": 123,
    "total": 25,
    "users": [
      {
        "userId": 789,
        "username": "张三",
        "avatar": "https://example.com/avatar1.jpg",
        "joinTime": "2024-01-01T10:00:00"
      },
      {
        "userId": 788,
        "username": "李四",
        "avatar": "https://example.com/avatar2.jpg",
        "joinTime": "2024-01-01T10:05:00"
      }
    ]
  },
  "timestamp": 1700000000000
}
```

### 2.4 获取在线人数

**接口：** `GET /chat/online-count/{auctionId}`

**路径参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| auctionId | Long | 是 | 拍卖活动ID |

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 25,
  "timestamp": 1700000000000
}
```

### 2.5 删除聊天消息

**接口：** `DELETE /chat/messages/{messageId}`

**路径参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| messageId | Long | 是 | 消息ID |

**权限说明：** 只有消息发送者可以删除自己的消息

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": true,
  "timestamp": 1700000000000
}
```

---

## 三、前端集成示例

### 3.1 WebSocket连接示例（JavaScript）

```javascript
// 建立WebSocket连接
const auctionId = 123;
const userId = 789;
const username = encodeURIComponent('张三');
const ws = new WebSocket(`ws://localhost:8080/ws/live/${auctionId}?userId=${userId}&username=${username}`);

// 连接成功
ws.onopen = () => {
  console.log('WebSocket连接成功');
  
  // 发送心跳
  setInterval(() => {
    ws.send(JSON.stringify({ type: 'PING' }));
  }, 30000);
};

// 接收消息
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('收到消息:', message);
  
  switch (message.type) {
    case 'CHAT_MESSAGE':
      // 处理聊天消息
      displayChatMessage(message.data);
      break;
    case 'USER_JOIN':
      // 处理用户加入
      displaySystemMessage(`${message.data.username} 加入了直播间`);
      updateOnlineCount(message.data.onlineCount);
      break;
    case 'USER_LEAVE':
      // 处理用户离开
      displaySystemMessage(`有用户离开了直播间`);
      updateOnlineCount(message.data.onlineCount);
      break;
    case 'CHAT_HISTORY':
      // 处理聊天历史
      displayChatHistory(message.data.messages);
      break;
    case 'SYSTEM_MESSAGE':
      // 处理系统消息
      displaySystemMessage(message.data.message);
      break;
  }
};

// 发送聊天消息
function sendChatMessage(content) {
  const message = {
    type: 'CHAT',
    content: content
  };
  ws.send(JSON.stringify(message));
}

// 连接关闭
ws.onclose = () => {
  console.log('WebSocket连接关闭');
};

// 连接错误
ws.onerror = (error) => {
  console.error('WebSocket错误:', error);
};
```

### 3.2 REST API调用示例（JavaScript）

```javascript
// 发送聊天消息
async function sendMessage(auctionId, content) {
  const response = await fetch('/chat/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      auctionId: auctionId,
      content: content
    })
  });
  return await response.json();
}

// 获取聊天历史
async function getChatHistory(auctionId, page = 1, size = 20) {
  const response = await fetch(
    `/chat/history/${auctionId}?page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return await response.json();
}

// 获取在线用户列表
async function getOnlineUsers(auctionId) {
  const response = await fetch(`/chat/online-users/${auctionId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
}

// 获取在线人数
async function getOnlineCount(auctionId) {
  const response = await fetch(`/chat/online-count/${auctionId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
}

// 删除聊天消息
async function deleteMessage(messageId) {
  const response = await fetch(`/chat/messages/${messageId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
}
```

---

## 四、技术架构

### 4.1 技术栈
- **WebSocket协议：** Spring WebSocket
- **消息存储：** MySQL + Redis缓存
- **用户管理：** 在线状态实时同步
- **消息格式：** JSON

### 4.2 核心组件
- **LiveRoomWebSocketHandler：** 直播间WebSocket处理器
- **WsLiveRoomService：** 直播间业务逻辑
- **WsRoomManager：** 房间连接管理
- **ChatService：** 聊天服务层
- **ChatController：** REST API控制器

### 4.3 数据表
- **chat_messages：** 聊天消息表
- **live_room_users：** 直播间用户表

---

## 五、注意事项

### 5.1 安全性
- ✅ 用户名脱敏处理（显示前两位+星号）
- ✅ 消息内容敏感词过滤（待实现）
- ✅ 发送频率限流（每分钟60次）
- ✅ 权限校验（只能删除自己的消息）

### 5.2 性能优化
- ✅ Redis缓存在线人数
- ✅ 异步消息发送
- ✅ 分页查询历史消息
- ✅ 心跳保活机制

### 5.3 待优化功能
- ⏳ 敏感词过滤算法
- ⏳ 用户头像上传
- ⏳ 消息点赞功能
- ⏳ @用户功能
- ⏳ 表情包支持

---

## 六、错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

---

**如有问题，请联系技术支持。**
