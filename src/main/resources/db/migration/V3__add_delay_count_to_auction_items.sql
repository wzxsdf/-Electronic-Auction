-- 添加delay_count字段到auction_items表
-- 用于持久化延时次数计数，防止系统重启后重置为0导致无限延时
-- Date: 2026-06-10

-- 使用存储过程方式安全添加字段（兼容所有MySQL版本）
DROP PROCEDURE IF EXISTS add_delay_count_column;

DELIMITER //
CREATE PROCEDURE add_delay_count_column()
BEGIN
    -- 检查列是否存在
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'auction_items'
        AND COLUMN_NAME = 'delay_count'
    ) THEN
        -- 列不存在，添加列
        ALTER TABLE `auction_items`
        ADD COLUMN `delay_count` INT NOT NULL DEFAULT 0 COMMENT '延时次数计数' AFTER `bid_count`;
    END IF;
END //
DELIMITER ;

-- 执行存储过程
CALL add_delay_count_column();

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_delay_count_column;
