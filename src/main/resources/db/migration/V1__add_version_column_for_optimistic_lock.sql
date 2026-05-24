-- ================================================
-- 添加乐观锁 version 字段
-- 用于防止并发修改导致的数据不一致问题
-- ================================================

-- 为 auctions 表添加 version 字段
ALTER TABLE `auctions` ADD COLUMN IF NOT EXISTS `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 为 auction_items 表添加 version 字段
ALTER TABLE `auction_items` ADD COLUMN IF NOT EXISTS `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 为现有数据设置版本号
UPDATE `auctions` SET `version` = 0 WHERE `version` IS NULL;
UPDATE `auction_items` SET `version` = 0 WHERE `version` IS NULL;
