-- ================================================
-- 拍卖系统并发安全数据库迁移脚本
-- 执行方式: mysql -u root -p auction < migrate.sql
-- ================================================

-- 1. 为 auctions 表添加乐观锁 version 字段
ALTER TABLE `auctions`
ADD COLUMN IF NOT EXISTS `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `id`;

-- 2. 为 auction_items 表添加乐观锁 version 字段
ALTER TABLE `auction_items`
ADD COLUMN IF NOT EXISTS `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `id`;

-- 3. 创建限流相关索引（可选，提升性能）
-- CREATE INDEX idx_auctions_status ON auctions(status);
-- CREATE INDEX idx_auction_items_status ON auction_items(status);

SELECT '迁移完成！' AS message;
