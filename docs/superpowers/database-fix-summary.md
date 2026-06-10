# 数据库Mapper修复说明

## 问题描述

出价接口调用时出现SQL错误，错误信息显示引用了不存在的表 `auction.auction_rooms`。

### 错误详情
```
Table 'auction.auction_rooms' doesn't exist
SQL: SELECT ai.*, p.name as product_name, p.image_url as product_image_url, p.description, ar.title as room_title 
FROM auction_items ai 
LEFT JOIN products p ON ai.product_id = p.id 
LEFT JOIN auction_rooms ar ON ai.room_id = ar.id 
WHERE ai.id = ?
```

## 问题原因

项目已从"房间（Room）"架构重构为"拍卖活动（Auction）"架构：
- `AuctionItem` 实体类的字段从 `roomId` 改为 `auctionId`
- 数据库表从 `auction_rooms` 改为使用 `auctions` 表
- 但 `AuctionItemMapper.java` 中的SQL查询未同步更新

## 修复内容

### 文件：`AuctionItemMapper.java`

#### 修复1：`getDetailById` 方法
**修复前：**
```sql
SELECT ai.*, p.name as product_name, p.image_url as product_image_url, p.description, 
       ar.title as room_title 
FROM auction_items ai 
LEFT JOIN products p ON ai.product_id = p.id 
LEFT JOIN auction_rooms ar ON ai.room_id = ar.id 
WHERE ai.id = #{itemId}
```

**修复后：**
```sql
SELECT ai.*, p.name as product_name, p.image_url as product_image_url, p.description, 
       a.title as auction_title 
FROM auction_items ai 
LEFT JOIN products p ON ai.product_id = p.id 
LEFT JOIN auctions a ON ai.auction_id = a.id 
WHERE ai.id = #{itemId}
```

#### 修复2：`getItemsByRoomAndStatus` 方法
**修复前：**
```sql
SELECT ai.*, p.name as product_name, p.image_url as product_image_url 
FROM auction_items ai 
LEFT JOIN products p ON ai.product_id = p.id 
WHERE ai.room_id = #{roomId} AND ai.status = #{status} 
ORDER BY ai.display_order ASC, ai.id ASC
```

**修复后：**
```sql
SELECT ai.*, p.name as product_name, p.image_url as product_image_url 
FROM auction_items ai 
LEFT JOIN products p ON ai.product_id = p.id 
WHERE ai.auction_id = #{auctionId} AND ai.status = #{status} 
ORDER BY ai.display_order ASC, ai.id ASC
```

## 修改对比

| 项目 | 修复前 | 修复后 |
|------|--------|--------|
| 关联表 | `auction_rooms` | `auctions` |
| 关联字段 | `ai.room_id` | `ai.auction_id` |
| 表别名 | `ar` | `a` |
| 返回字段 | `ar.title as room_title` | `a.title as auction_title` |
| 参数名 | `roomId` | `auctionId` |

## 验证结果

✅ 所有SQL查询已更新为正确的表结构
✅ 不再引用已删除的 `auction_rooms` 表
✅ 使用正确的 `auctions` 表和 `auction_id` 字段
✅ 与 `AuctionItem` 实体类的字段结构保持一致

## 影响范围

- **出价功能**：修复后可以正常查询拍品详情和关联的商品、拍卖活动信息
- **拍品查询**：所有基于拍品ID的查询都能正常工作
- **数据一致性**：SQL查询与数据库表结构完全匹配

## 注意事项

如果项目中有其他地方仍在使用旧的 `room_id` 字段或引用 `auction_rooms` 表，需要同步更新：
- 检查其他Mapper文件
- 检查Service层代码
- 检查数据库迁移脚本

## 后续建议

1. 考虑重命名 `getItemsByRoomAndStatus` 方法为 `getItemsByAuctionIdAndStatus`
2. 检查是否有其他遗留的Room相关代码需要清理
3. 更新数据库文档，反映最新的表结构
