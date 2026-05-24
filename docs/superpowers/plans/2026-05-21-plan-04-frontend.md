# 直播竞拍全栈系统 - Plan 4: 前端框架 + 核心页面

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建前端项目，实现移动端 H5 直播间页面和 PC 管理后台，包括 WebSocket 实时通信、出价交互、竞价氛围展示。

**Architecture:** React 18 + TypeScript + Vite，Zustand 状态管理，TailwindCSS 样式，Axios HTTP 请求，原生 WebSocket。

**Tech Stack:** React 18, TypeScript, Vite, Zustand, Axios, TailwindCSS, Ant Design Mobile

---

## 文件结构

```
auction-frontend/
├── src/
│   ├── api/                          # API 服务
│   │   ├── index.ts
│   │   ├── auction.ts
│   │   ├── bid.ts
│   │   └── websocket.ts
│   ├── hooks/                        # 自定义 Hooks
│   │   ├── useWebSocket.ts
│   │   ├── useAuction.ts
│   │   └── useCountDown.ts
│   ├── store/                        # 状态管理
│   │   ├── index.ts
│   │   └── auctionStore.ts
│   ├── types/                        # TypeScript 类型
│   │   ├── auction.ts
│   │   ├── bid.ts
│   │   └── websocket.ts
│   ├── components/                   # 组件
│   │   ├── mobile/                   # 移动端组件
│   │   │   ├── LiveRoom.tsx
│   │   │   ├── PriceDisplay.tsx
│   │   │   ├── CountDown.tsx
│   │   │   ├── Leaderboard.tsx
│   │   │   ├── BidInput.tsx
│   │   │   └── BidButton.tsx
│   │   └── common/                   # 通用组件
│   │       ├── Loading.tsx
│   │       └── Error.tsx
│   ├── pages/                        # 页面
│   │   ├── mobile/                   # 移动端页面
│   │   │   ├── LiveRoomPage.tsx
│   │   │   └── AuctionListPage.tsx
│   │   └── admin/                    # 管理后台
│   │       ├── Dashboard.tsx
│   │       ├── AuctionManage.tsx
│   │       └── ProductEdit.tsx
│   ├── App.tsx
│   └── main.tsx
├── index.html
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
└── package.json
```

---

## Task 1: 创建项目脚手架

**Files:**
- Create: `package.json`
- Create: `vite.config.ts`
- Create: `tsconfig.json`
- Create: `index.html`
- Create: `src/main.tsx`
- Create: `src/App.tsx`
- Create: `tailwind.config.js`
- Create: `postcss.config.js`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "auction-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "zustand": "^4.4.7",
    "axios": "^1.6.2",
    "antd-mobile": "^5.34.0",
    "dayjs": "^1.11.10"
  },
  "devDependencies": {
    "@types/react": "^18.2.43",
    "@types/react-dom": "^18.2.17",
    "@vitejs/plugin-react": "^4.2.1",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32",
    "tailwindcss": "^3.3.6",
    "typescript": "^5.3.3",
    "vite": "^5.0.8"
  }
}
```

- [ ] **Step 2: 创建 vite.config.ts**

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
});
```

- [ ] **Step 3: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 4: 创建 tsconfig.node.json**

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 5: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <title>直播竞拍</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 6: 创建 src/main.tsx**

```typescript
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
```

- [ ] **Step 7: 创建 src/App.tsx**

```typescript
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LiveRoomPage from './pages/mobile/LiveRoomPage';
import AuctionListPage from './pages/mobile/AuctionListPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<AuctionListPage />} />
        <Route path="/live/:auctionId" element={<LiveRoomPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

- [ ] **Step 8: 创建 src/index.css**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#root {
  min-height: 100vh;
}
```

- [ ] **Step 9: 创建 tailwind.config.js**

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#FF6B6B',
        secondary: '#4ECDC4',
        accent: '#FFE66D',
      },
    },
  },
  plugins: [],
}
```

- [ ] **Step 10: 创建 postcss.config.js**

```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

- [ ] **Step 11: 安装依赖**

```bash
cd auction-frontend
npm install
```

Expected: 依赖安装成功

- [ ] **Step 12: 启动开发服务器**

```bash
npm run dev
```

Expected: 服务器在 http://localhost:3000 启动

- [ ] **Step 13: 提交**

```bash
git add .
git commit -m "feat: 创建前端项目脚手架"
```

---

## Task 2: 创建类型定义

**Files:**
- Create: `src/types/auction.ts`
- Create: `src/types/bid.ts`
- Create: `src/types/websocket.ts`
- Create: `src/types/common.ts`

- [ ] **Step 1: 创建 src/types/common.ts**

```typescript
export interface Result<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageParams {
  pageNum: number;
  pageSize: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}
```

- [ ] **Step 2: 创建 src/types/auction.ts**

```typescript
export type AuctionStatus = 'PENDING' | 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'COMPLETED';

export interface Product {
  id: number;
  name: string;
  imageUrl?: string;
  description?: string;
  category?: string;
  createdAt: string;
}

export interface Auction {
  id: number;
  title: string;
  productId: number;
  productName?: string;
  productImageUrl?: string;
  startPrice: number;
  currentPrice: number;
  bidIncrement: number;
  maxPrice?: number;
  startTime: string;
  endTime: string;
  status: AuctionStatus;
  statusDesc?: string;
  highestBidder?: number;
  bidCount: number;
  createdAt: string;
}

export interface AuctionDetail extends Auction {
  description?: string;
  delaySeconds: number;
  originalEndTime: string;
  participantCount?: number;
  isExtendable?: boolean;
}
```

- [ ] **Step 3: 创建 src/types/bid.ts**

```typescript
export interface PlaceBidRequest {
  auctionId: number;
  userId: number;
  amount: number;
  isAutoBid?: boolean;
}

export interface BidResult {
  bidId: number;
  currentPrice: number;
  yourRank: number;
  isLeading: boolean;
  remainingMs: number;
  wasExtended?: boolean;
  newEndTime?: number;
  message: string;
}

export interface Bid {
  id: number;
  auctionId: number;
  userId: number;
  username: string;
  amount: number;
  rankWhenBid?: number;
  status: string;
  isAutoBid: boolean;
  createdAt: string;
}
```

- [ ] **Step 4: 创建 src/types/websocket.ts**

```typescript
export type MessageType =
  | 'CONNECT'
  | 'DISCONNECT'
  | 'PING'
  | 'PONG'
  | 'AUCTION_START'
  | 'AUCTION_END'
  | 'AUCTION_EXTENDED'
  | 'AUCTION_PAUSED'
  | 'AUCTION_CANCELLED'
  | 'NEW_BID'
  | 'BID_VALIDATED'
  | 'BID_REJECTED'
  | 'PRICE_UPDATE'
  | 'LEADERBOARD_UPDATE'
  | 'TIME_UPDATE'
  | 'BID_COUNT_UPDATE'
  | 'YOU_ARE_LEADING'
  | 'YOU_WERE_OVERTAKEN'
  | 'YOU_WON'
  | 'YOU_LOST'
  | 'ERROR';

export interface WsMessage<T = any> {
  type: MessageType;
  data: T;
  timestamp: number;
  auctionId?: number;
}

export interface BidMessageData {
  bidId: number;
  auctionId: number;
  userId: number;
  username: string;
  amount: number;
  rank: number;
  bidTime: string;
  isAutoBid: boolean;
}

export interface PriceUpdateData {
  currentPrice: number;
  highestBidder?: number;
  highestBidderName?: string;
  bidCount: number;
  remainingMs?: number;
}

export interface AuctionExtendedData {
  auctionId: number;
  oldEndTime: string;
  newEndTime: string;
  extendedSeconds: number;
  reason: string;
}

export interface LeaderboardEntry {
  userId: number;
  username: string;
  amount: number;
  rank: number;
}
```

- [ ] **Step 5: 提交**

```bash
git add .
git commit -m "feat: 添加类型定义"
```

---

## Task 3: 创建 API 服务层

**Files:**
- Create: `src/api/index.ts`
- Create: `src/api/auction.ts`
- Create: `src/api/bid.ts`

- [ ] **Step 1: 创建 src/api/index.ts**

```typescript
import axios, { AxiosInstance, AxiosError } from 'axios';
import type { Result } from '@/types/common';

const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 可以在这里添加 token
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    const { data } = response;
    if (data.code === 200) {
      return data;
    }
    return Promise.reject(new Error(data.message || '请求失败'));
  },
  (error: AxiosError<Result>) => {
    const message = error.response?.data?.message || error.message || '网络错误';
    return Promise.reject(new Error(message));
  }
);

export default api;
```

- [ ] **Step 2: 创建 src/api/auction.ts**

```typescript
import api from './index';
import type { Result } from '@/types/common';
import type { Auction, AuctionDetail, Product } from '@/types/auction';

export const auctionApi = {
  // 获取所有竞拍
  listAll: () =>
    api.get<Result<Auction[]>>('/auctions'),

  // 获取进行中的竞拍
  listActive: () =>
    api.get<Result<Auction[]>>('/auctions/active'),

  // 获取待开始的竞拍
  listPending: () =>
    api.get<Result<Auction[]>>('/auctions/pending'),

  // 获取竞拍详情
  getById: (id: number) =>
    api.get<Result<AuctionDetail>>(`/auctions/${id}`),

  // 创建竞拍（管理员）
  create: (data: {
    productId: number;
    title: string;
    startPrice: number;
    bidIncrement: number;
    maxPrice?: number;
    delaySeconds?: number;
    startTime: string;
    endTime: string;
  }) =>
    api.post<Result<Auction>>('/auctions', data),

  // 开始竞拍（管理员）
  start: (auctionId: number) =>
    api.post<Result<void>>('/auctions/start', { auctionId }),

  // 暂停竞拍（管理员）
  pause: (auctionId: number, reason?: string) =>
    api.post<Result<void>>(`/auctions/${auctionId}/pause`, null, {
      params: { reason },
    }),

  // 恢复竞拍（管理员）
  resume: (auctionId: number) =>
    api.post<Result<void>>(`/auctions/${auctionId}/resume`),

  // 取消竞拍（管理员）
  cancel: (auctionId: number, reason?: string) =>
    api.post<Result<void>>(`/auctions/${auctionId}/cancel`, null, {
      params: { reason },
    }),
};

export const productApi = {
  // 获取所有商品
  listAll: () =>
    api.get<Result<Product[]>>('/products'),

  // 获取商品详情
  getById: (id: number) =>
    api.get<Result<Product>>(`/products/${id}`),

  // 创建商品（管理员）
  create: (data: {
    name: string;
    imageUrl?: string;
    description?: string;
    category?: string;
  }) =>
    api.post<Result<Product>>('/products', data),
};
```

- [ ] **Step 3: 创建 src/api/bid.ts**

```typescript
import api from './index';
import type { Result } from '@/types/common';
import type { Bid, BidResult, PlaceBidRequest } from '@/types/bid';

export const bidApi = {
  // 出价
  placeBid: (data: PlaceBidRequest) =>
    api.post<Result<BidResult>>('/bids', data),

  // 获取出价记录
  getHistory: (auctionId: number) =>
    api.get<Result<Bid[]>>(`/bids/auction/${auctionId}`),

  // 获取用户出价记录
  getUserBids: (auctionId: number, userId: number) =>
    api.get<Result<Bid[]>>(`/bids/auction/${auctionId}/user/${userId}`),
};
```

- [ ] **Step 4: 提交**

```bash
git add .
git commit -m "feat: 添加 API 服务层"
```

---

## Task 4: 创建 WebSocket Hook

**Files:**
- Create: `src/hooks/useWebSocket.ts`

- [ ] **Step 1: 创建 useWebSocket Hook**

```typescript
import { useEffect, useRef, useState, useCallback } from 'react';
import type { WsMessage, MessageType } from '@/types/websocket';

interface UseWebSocketOptions {
  onMessage?: (message: WsMessage) => void;
  onConnected?: () => void;
  onDisconnected?: () => void;
  onError?: (error: Event) => void;
  reconnect?: boolean;
  reconnectInterval?: number;
  maxReconnectAttempts?: number;
}

interface UseWebSocketReturn {
  connected: boolean;
  sendMessage: (message: any) => void;
  connect: () => void;
  disconnect: () => void;
}

export function useWebSocket(
  url: string | null,
  options: UseWebSocketOptions = {}
): UseWebSocketReturn {
  const {
    onMessage,
    onConnected,
    onDisconnected,
    onError,
    reconnect = true,
    reconnectInterval = 3000,
    maxReconnectAttempts = 5,
  } = options;

  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout>();
  const reconnectAttemptsRef = useRef(0);
  const heartbeatIntervalRef = useRef<NodeJS.Timeout>();

  const disconnect = useCallback(() => {
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setConnected(false);
  }, []);

  const connect = useCallback(() => {
    if (!url || wsRef.current?.readyState === WebSocket.OPEN) {
      return;
    }

    try {
      const ws = new WebSocket(url);
      wsRef.current = ws;

      ws.onopen = () => {
        setConnected(true);
        reconnectAttemptsRef.current = 0;
        onConnected?.();

        // 启动心跳
        heartbeatIntervalRef.current = setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ type: 'PING' }));
          }
        }, 30000);
      };

      ws.onmessage = (event) => {
        try {
          const message: WsMessage = JSON.parse(event.data);

          // 处理心跳响应
          if (message.type === 'PONG') {
            return;
          }

          onMessage?.(message);
        } catch (error) {
          console.error('解析 WebSocket 消息失败:', error);
        }
      };

      ws.onclose = () => {
        setConnected(false);
        onDisconnected?.();

        if (heartbeatIntervalRef.current) {
          clearInterval(heartbeatIntervalRef.current);
        }

        // 自动重连
        if (reconnect && reconnectAttemptsRef.current < maxReconnectAttempts) {
          reconnectAttemptsRef.current++;
          const delay = reconnectInterval * Math.pow(2, reconnectAttemptsRef.current - 1);

          reconnectTimeoutRef.current = setTimeout(() => {
            console.log(`WebSocket 重连中... (${reconnectAttemptsRef.current}/${maxReconnectAttempts})`);
            connect();
          }, delay);
        }
      };

      ws.onerror = (error) => {
        console.error('WebSocket 错误:', error);
        onError?.(error);
      };
    } catch (error) {
      console.error('创建 WebSocket 连接失败:', error);
    }
  }, [url, reconnect, reconnectInterval, maxReconnectAttempts, onConnected, onDisconnected, onError, onMessage]);

  const sendMessage = useCallback((message: any) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket 未连接，无法发送消息');
    }
  }, []);

  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  return {
    connected,
    sendMessage,
    connect,
    disconnect,
  };
}
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加 WebSocket Hook"
```

---

## Task 5: 创建 Zustand 状态管理

**Files:**
- Create: `src/store/index.ts`
- Create: `src/store/auctionStore.ts`

- [ ] **Step 1: 创建 src/store/auctionStore.ts**

```typescript
import { create } from 'zustand';
import type { Auction, AuctionDetail, BidMessageData, PriceUpdateData, LeaderboardEntry } from '@/types';

interface AuctionState {
  // 当前竞拍信息
  currentAuction: AuctionDetail | null;
  currentPrice: number;
  highestBidder: number | null;
  bidCount: number;
  remainingMs: number | null;

  // 排行榜
  leaderboard: LeaderboardEntry[];

  // 个人状态
  myRank: number | null;
  myLastBid: number | null;
  isLeading: boolean;

  // 通知
  notifications: string[];

  // Actions
  setCurrentAuction: (auction: AuctionDetail) => void;
  updatePrice: (data: PriceUpdateData) => void;
  updateLeaderboard: (leaderboard: LeaderboardEntry[]) => void;
  addNotification: (message: string) => void;
  clearNotifications: () => void;
  reset: () => void;
}

export const useAuctionStore = create<AuctionState>((set) => ({
  // 初始状态
  currentAuction: null,
  currentPrice: 0,
  highestBidder: null,
  bidCount: 0,
  remainingMs: null,
  leaderboard: [],
  myRank: null,
  myLastBid: null,
  isLeading: false,
  notifications: [],

  setCurrentAuction: (auction) =>
    set({
      currentAuction: auction,
      currentPrice: auction.currentPrice,
      highestBidder: auction.highestBidder || null,
      bidCount: auction.bidCount,
    }),

  updatePrice: (data) =>
    set((state) => ({
      currentPrice: data.currentPrice,
      highestBidder: data.highestBidder || null,
      bidCount: data.bidCount,
      remainingMs: data.remainingMs || state.remainingMs,
    })),

  updateLeaderboard: (leaderboard) =>
    set({ leaderboard }),

  addNotification: (message) =>
    set((state) => ({
      notifications: [...state.notifications, message],
    })),

  clearNotifications: () =>
    set({ notifications: [] }),

  reset: () =>
    set({
      currentAuction: null,
      currentPrice: 0,
      highestBidder: null,
      bidCount: 0,
      remainingMs: null,
      leaderboard: [],
      myRank: null,
      myLastBid: null,
      isLeading: false,
      notifications: [],
    }),
}));
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat: 添加 Zustand 状态管理"
```

---

## Task 6: 创建通用组件

**Files:**
- Create: `src/components/common/Loading.tsx`
- Create: `src/components/common/Error.tsx`
- Create: `src/components/common/CountDown.tsx`

- [ ] **Step 1: 创建 Loading 组件**

```typescript
interface LoadingProps {
  size?: 'small' | 'medium' | 'large';
}

export default function Loading({ size = 'medium' }: LoadingProps) {
  const sizeClass = {
    small: 'w-4 h-4',
    medium: 'w-8 h-8',
    large: 'w-12 h-12',
  }[size];

  return (
    <div className="flex items-center justify-center">
      <div
        className={`${sizeClass} border-4 border-gray-200 border-t-primary rounded-full animate-spin`}
      />
    </div>
  );
}
```

- [ ] **Step 2: 创建 Error 组件**

```typescript
interface ErrorProps {
  message: string;
  onRetry?: () => void;
}

export default function Error({ message, onRetry }: ErrorProps) {
  return (
    <div className="flex flex-col items-center justify-center p-8">
      <div className="text-red-500 text-4xl mb-4">⚠️</div>
      <p className="text-gray-600 mb-4">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90"
        >
          重试
        </button>
      )}
    </div>
  );
}
```

- [ ] **Step 3: 创建 CountDown 组件**

```typescript
import { useEffect, useState } from 'react';
import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
import utc from 'dayjs/plugin/utc';
dayjs.extend(duration);
dayjs.extend(utc);

interface CountDownProps {
  endTime: string | Date;
  remainingMs?: number | null;
  className?: string;
  onComplete?: () => void;
}

export default function CountDown({ endTime, remainingMs, className = '', onComplete }: CountDownProps) {
  const [timeLeft, setTimeLeft] = useState<number>(0);

  useEffect(() => {
    const calculateTimeLeft = () => {
      if (remainingMs !== null && remainingMs !== undefined) {
        return remainingMs;
      }
      const end = dayjs(endTime);
      const now = dayjs();
      return Math.max(0, end.diff(now));
    };

    setTimeLeft(calculateTimeLeft());

    const timer = setInterval(() => {
      const newTimeLeft = calculateTimeLeft();
      setTimeLeft(newTimeLeft);

      if (newTimeLeft <= 0 && onComplete) {
        onComplete();
        clearInterval(timer);
      }
    }, 100);

    return () => clearInterval(timer);
  }, [endTime, remainingMs, onComplete]);

  if (timeLeft <= 0) {
    return <span className={className}>竞拍已结束</span>;
  }

  const duration = dayjs.duration(timeLeft);
  const hours = Math.floor(duration.asHours());
  const minutes = duration.minutes();
  const seconds = duration.seconds();
  const ms = Math.floor(duration.milliseconds() / 10);

  const formatNum = (n: number) => n.toString().padStart(2, '0');

  return (
    <span className={`tabular-nums ${className}`}>
      {hours > 0 && <>{formatNum(hours)}:</>}
      {formatNum(minutes)}:{formatNum(seconds)}.<span className="text-sm">{formatNum(ms)}</span>
    </span>
  );
}
```

- [ ] **Step 4: 提交**

```bash
git add .
git commit -m "feat: 添加通用组件"
```

---

## Task 7: 创建移动端组件

**Files:**
- Create: `src/components/mobile/PriceDisplay.tsx`
- Create: `src/components/mobile/Leaderboard.tsx`
- Create: `src/components/mobile/BidInput.tsx`

- [ ] **Step 1: 创建 PriceDisplay 组件**

```typescript
import { useAuctionStore } from '@/store';

interface PriceDisplayProps {
  className?: string;
}

export default function PriceDisplay({ className = '' }: PriceDisplayProps) {
  const { currentPrice, bidCount } = useAuctionStore();

  return (
    <div className={`bg-gradient-to-r from-primary to-orange-500 text-white p-4 rounded-xl ${className}`}>
      <div className="text-sm opacity-90 mb-1">当前价格</div>
      <div className="text-3xl font-bold">¥ {currentPrice.toLocaleString()}</div>
      <div className="text-sm opacity-75 mt-1">出价 {bidCount} 次</div>
    </div>
  );
}
```

- [ ] **Step 2: 创建 Leaderboard 组件**

```typescript
import { useAuctionStore } from '@/store';
import { useEffect } from 'react';
import type { LeaderboardEntry } from '@/types';
import type { WsMessage } from '@/types/websocket';

interface LeaderboardProps {
  className?: string;
  currentUserId?: number;
}

export default function Leaderboard({ className = '', currentUserId }: LeaderboardProps) {
  const { leaderboard, updateLeaderboard } = useAuctionStore();

  // 监听排行榜更新
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      try {
        const message: WsMessage<LeaderboardEntry[]> = JSON.parse(event.data);
        if (message.type === 'LEADERBOARD_UPDATE') {
          updateLeaderboard(message.data);
        }
      } catch (error) {
        console.error('解析排行榜消息失败:', error);
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, [updateLeaderboard]);

  return (
    <div className={`bg-white rounded-xl p-4 ${className}`}>
      <h3 className="text-lg font-bold mb-3">🏆 实时排行榜</h3>
      {leaderboard.length === 0 ? (
        <p className="text-gray-400 text-center py-4">暂无出价</p>
      ) : (
        <div className="space-y-2">
          {leaderboard.map((entry, index) => (
            <div
              key={entry.userId}
              className={`flex items-center justify-between p-2 rounded-lg ${
                entry.userId === currentUserId ? 'bg-primary bg-opacity-10' : ''
              }`}
            >
              <div className="flex items-center gap-2">
                <span className="w-6 h-6 flex items-center justify-center rounded-full bg-gray-200 text-sm font-bold">
                  {index + 1}
                </span>
                <span className="text-sm">
                  {entry.userId === currentUserId ? '你' : entry.username}
                  {entry.userId === currentUserId && ' ←'}
                </span>
              </div>
              <span className="font-bold text-primary">¥ {entry.amount.toLocaleString()}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 3: 创建 BidInput 组件**

```typescript
import { useState } from 'react';
import { bidApi } from '@/api/bid';
import { useAuctionStore } from '@/store';
import type { PlaceBidRequest, BidResult } from '@/types';

interface BidInputProps {
  auctionId: number;
  userId: number;
  className?: string;
  onBidSuccess?: (result: BidResult) => void;
  onBidError?: (error: Error) => void;
}

export default function BidInput({
  auctionId,
  userId,
  className = '',
  onBidSuccess,
  onBidError,
}: BidInputProps) {
  const { currentPrice, bidCount, currentAuction } = useAuctionStore();
  const [amount, setAmount] = useState('');
  const [loading, setLoading] = useState(false);

  const minPrice = currentPrice + (currentAuction?.bidIncrement || 10);
  const quickAddAmounts = [10, 50, 100, 500];

  const handleQuickAdd = (addAmount: number) => {
    const current = parseInt(amount) || minPrice;
    setAmount((current + addAmount).toString());
  };

  const handlePlaceBid = async () => {
    const bidAmount = parseInt(amount);
    if (!bidAmount || bidAmount <= minPrice) {
      onBidError?.(new Error(`出价必须高于 ¥${minPrice}`));
      return;
    }

    setLoading(true);
    try {
      const request: PlaceBidRequest = {
        auctionId,
        userId,
        amount: bidAmount,
      };
      const result = await bidApi.placeBid(request);
      onBidSuccess?.(result);
      setAmount('');
    } catch (error) {
      onBidError?.(error as Error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`bg-white rounded-xl p-4 ${className}`}>
      <div className="mb-3">
        <label className="text-sm text-gray-600 block mb-2">出价金额</label>
        <div className="flex items-center gap-2">
          <span className="text-2xl">¥</span>
          <input
            type="number"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder={`最低 ¥${minPrice}`}
            className="flex-1 text-2xl font-bold border-b-2 border-gray-300 focus:border-primary outline-none px-2 py-1"
            disabled={loading}
          />
        </div>
      </div>

      <div className="flex gap-2 mb-4">
        {quickAddAmounts.map((addAmount) => (
          <button
            key={addAmount}
            onClick={() => handleQuickAdd(addAmount)}
            className="flex-1 py-2 bg-gray-100 rounded-lg text-sm hover:bg-gray-200 transition"
            disabled={loading}
          >
            +{addAmount}
          </button>
        ))}
      </div>

      <button
        onClick={handlePlaceBid}
        disabled={loading || !amount}
        className="w-full py-3 bg-gradient-to-r from-primary to-orange-500 text-white text-lg font-bold rounded-xl hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition"
      >
        {loading ? '出价中...' : '立即出价'}
      </button>
    </div>
  );
}
```

- [ ] **Step 4: 提交**

```bash
git add .
git commit -m "feat: 添加移动端组件"
```

---

## Task 8: 创建直播间页面

**Files:**
- Create: `src/pages/mobile/LiveRoomPage.tsx`
- Create: `src/pages/mobile/AuctionListPage.tsx`

- [ ] **Step 1: 创建 LiveRoomPage**

```typescript
import { useParams } from 'react-router-dom';
import { useEffect, useState, useCallback } from 'react';
import { auctionApi } from '@/api/auction';
import { useWebSocket } from '@/hooks/useWebSocket';
import { useAuctionStore } from '@/store';
import { type WsMessage, type BidResult, type AuctionDetail } from '@/types';
import CountDown from '@/components/common/CountDown';
import PriceDisplay from '@/components/mobile/PriceDisplay';
import Leaderboard from '@/components/mobile/Leaderboard';
import BidInput from '@/components/mobile/BidInput';
import Loading from '@/components/common/Loading';
import Error from '@/components/common/Error';
import { Toast } from 'antd-mobile';

export default function LiveRoomPage() {
  const { auctionId } = useParams<{ auctionId: string }>();
  const userId = 1; // TODO: 从登录状态获取

  const {
    currentAuction,
    setCurrentAuction,
    updatePrice,
    updateLeaderboard,
    addNotification,
    clearNotifications,
  } = useAuctionStore();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // WebSocket 连接
  const { connected, sendMessage } = useWebSocket(
    auctionId ? `ws://localhost:8080/api/ws/auction/${auctionId}?userId=${userId}` : null,
    {
      onMessage: (message: WsMessage) => {
        handleWsMessage(message);
      },
      onConnected: () => {
        Toast.show({ content: '已连接到直播间', icon: 'success' });
      },
      onDisconnected: () => {
        Toast.show({ content: '连接断开', icon: 'fail' });
      },
    }
  );

  // 获取竞拍详情
  useEffect(() => {
    if (!auctionId) return;

    const fetchAuction = async () => {
      try {
        setLoading(true);
        const data = await auctionApi.getById(Number(auctionId));
        setCurrentAuction(data);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    fetchAuction();
  }, [auctionId, setCurrentAuction]);

  // 处理 WebSocket 消息
  const handleWsMessage = useCallback((message: WsMessage) => {
    switch (message.type) {
      case 'PRICE_UPDATE':
        updatePrice(message.data);
        break;

      case 'LEADERBOARD_UPDATE':
        updateLeaderboard(message.data);
        break;

      case 'NEW_BID':
        updatePrice({
          currentPrice: message.data.amount,
          bidCount: currentAuction?.bidCount || 0 + 1,
        });
        Toast.show({
          content: `${message.data.username} 出价 ¥${message.data.amount}`,
          duration: 2000,
        });
        break;

      case 'AUCTION_EXTENDED':
        Toast.show({
          content: `竞拍延长 ${message.data.extendedSeconds} 秒！`,
          icon: '⏰',
        });
        break;

      case 'YOU_ARE_LEADING':
        Toast.show({ content: '🎉 你目前领先！', icon: 'success' });
        break;

      case 'YOU_WERE_OVERTAKEN':
        Toast.show({ content: '⚡ 你被超越了！', icon: 'fail' });
        break;

      case 'AUCTION_END':
        Toast.show({ content: '竞拍已结束！', icon: 'info' });
        break;

      default:
        break;
    }
  }, [currentAuction, updatePrice, updateLeaderboard]);

  // 出价成功
  const handleBidSuccess = (result: BidResult) => {
    Toast.show({
      content: result.message || '出价成功！',
      icon: 'success',
    });
    if (result.wasExtended) {
      Toast.show({
        content: `竞拍自动延长，剩余 ${result.newEndTime} 秒`,
        icon: '⏰',
      });
    }
  };

  // 出价失败
  const handleBidError = (error: Error) => {
    Toast.show({
      content: error.message,
      icon: 'fail',
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loading />
      </div>
    );
  }

  if (error || !currentAuction) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Error message={error || '竞拍不存在'} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 pb-4">
      {/* 顶部导航 */}
      <div className="bg-white p-4 flex items-center justify-between sticky top-0 z-10 shadow-sm">
        <button className="text-gray-600">←</button>
        <span className="font-bold">{currentAuction.title}</span>
        <div className="flex items-center gap-1 text-sm text-gray-500">
          <span className={`w-2 h-2 rounded-full ${connected ? 'bg-green-500' : 'bg-red-500'}`} />
          {connected ? '在线' : '离线'}
        </div>
      </div>

      {/* 直播画面 */}
      <div className="bg-gray-900 aspect-video flex items-center justify-center">
        <div className="text-white text-center">
          <div className="text-4xl mb-2">📺</div>
          <p className="text-sm opacity-75">模拟直播画面</p>
        </div>
      </div>

      {/* 价格和倒计时 */}
      <div className="p-4 space-y-3">
        <div className="flex items-center justify-between">
          <PriceDisplay />
          <div className="bg-white rounded-xl px-4 py-3">
            <div className="text-xs text-gray-500 mb-1">剩余时间</div>
            <CountDown
              endTime={currentAuction.endTime}
              className="text-xl font-bold text-primary"
            />
          </div>
        </div>

        {/* 排行榜 */}
        <Leaderboard currentUserId={userId} />

        {/* 出价输入 */}
        <BidInput
          auctionId={Number(auctionId)}
          userId={userId}
          onBidSuccess={handleBidSuccess}
          onBidError={handleBidError}
        />
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 创建 AuctionListPage**

```typescript
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { auctionApi } from '@/api/auction';
import { type Auction } from '@/types';
import Loading from '@/components/common/Loading';
import Error from '@/components/common/Error';

export default function AuctionListPage() {
  const [auctions, setAuctions] = useState<Auction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'active' | 'pending'>('active');

  useEffect(() => {
    const fetchAuctions = async () => {
      try {
        setLoading(true);
        const apiFunc = activeTab === 'active' ? auctionApi.listActive : auctionApi.listPending;
        const data = await apiFunc();
        setAuctions(data);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    };

    fetchAuctions();
  }, [activeTab]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loading />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Error message={error} onRetry={() => window.location.reload()} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* 顶部导航 */}
      <div className="bg-white p-4 sticky top-0 z-10 shadow-sm">
        <h1 className="text-xl font-bold text-center">竞拍大厅</h1>
      </div>

      {/* Tab 切换 */}
      <div className="bg-white flex border-b">
        <button
          className={`flex-1 py-3 text-center ${activeTab === 'active' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
          onClick={() => setActiveTab('active')}
        >
          进行中
        </button>
        <button
          className={`flex-1 py-3 text-center ${activeTab === 'pending' ? 'text-primary border-b-2 border-primary' : 'text-gray-500'}`}
          onClick={() => setActiveTab('pending')}
        >
          即将开始
        </button>
      </div>

      {/* 竞拍列表 */}
      <div className="p-4 space-y-4">
        {auctions.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <div className="text-4xl mb-2">📭</div>
            <p>暂无竞拍</p>
          </div>
        ) : (
          auctions.map((auction) => (
            <Link
              key={auction.id}
              to={`/live/${auction.id}`}
              className="block bg-white rounded-xl overflow-hidden shadow-sm hover:shadow-md transition"
            >
              <div className="flex">
                <div className="w-32 h-32 bg-gray-200 flex-shrink-0">
                  {auction.productImageUrl ? (
                    <img src={auction.productImageUrl} alt="" className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-400">
                      📦
                    </div>
                  )}
                </div>
                <div className="flex-1 p-3">
                  <h3 className="font-bold mb-1">{auction.title}</h3>
                  <p className="text-sm text-gray-500 mb-2">{auction.productName}</p>
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-xs text-gray-400">当前价格</div>
                      <div className="text-lg font-bold text-primary">¥ {auction.currentPrice.toLocaleString()}</div>
                    </div>
                    {auction.status === 'ACTIVE' && (
                      <div className="px-3 py-1 bg-red-100 text-red-500 text-xs rounded-full">
                        进行中
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </Link>
          ))
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 3: 添加 react-router-dom 依赖**

```bash
npm install react-router-dom
```

- [ ] **Step 4: 更新 main.tsx 导入 antd-mobile 样式**

```typescript
import 'antd-mobile/bundle.css';
```

- [ ] **Step 5: 提交**

```bash
git add .
git commit -m "feat: 添加移动端页面"
```

---

## Task 9: 测试前端应用

- [ ] **Step 1: 启动后端服务**

```bash
cd auction-system
mvn spring-boot:run
```

- [ ] **Step 2: 启动前端服务**

```bash
cd auction-frontend
npm run dev
```

- [ ] **Step 3: 访问页面测试**

打开浏览器访问:
- http://localhost:3000 - 竞拍列表页
- http://localhost:3000/live/1 - 直播间页（需要先创建竞拍数据）

- [ ] **Step 4: 测试功能清单**

| 功能 | 测试方法 | 预期结果 |
|------|---------|---------|
| 竞拍列表 | 访问首页 | 显示竞拍列表 |
| 进入直播间 | 点击竞拍卡片 | 跳转到直播间页 |
| WebSocket 连接 | 打开直播间 | 右上角显示"在线" |
| 出价 | 输入金额点击出价 | 出价成功，价格更新 |
| 排行榜 | 查看排行榜 | 显示当前排名 |
| 倒计时 | 查看倒计时 | 实时更新 |

- [ ] **Step 5: 提交**

```bash
git add .
git commit -m "test: 验证前端功能"
```

---

## 验收标准

完成本计划后，应该能够：

1. ✅ 前端项目成功启动，端口 3000
2. ✅ 显示竞拍列表
3. ✅ 进入直播间页面
4. ✅ WebSocket 连接成功
5. ✅ 实时显示价格和倒计时
6. ✅ 排行榜实时更新
7. ✅ 出价功能正常
8. ✅ 收到出价通知

---

## 下一步

完成本计划后，继续 **Plan 5: AI 功能 + 风控检测**，实现智能建议、自动出价和风控功能。
