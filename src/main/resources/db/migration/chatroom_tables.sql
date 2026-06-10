-- ============================================================
-- 直播间功能数据库表脚本
-- ============================================================

-- 聊天消息表
CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `auction_id` BIGINT NOT NULL COMMENT '拍卖活动ID',
  `user_id` BIGINT NOT NULL COMMENT '发送用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（显示用）',
  `content` TEXT NOT NULL COMMENT '聊天内容',
  `message_type` TINYINT DEFAULT 1 COMMENT '消息类型：1-用户消息，2-系统消息',
  `is_deleted` BOOLEAN DEFAULT FALSE COMMENT '是否已删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_auction_created` (`auction_id`, `created_at`),
  INDEX `idx_user` (`user_id`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播间聊天消息表';

-- 直播间用户表
CREATE TABLE IF NOT EXISTS `live_room_users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `auction_id` BIGINT NOT NULL COMMENT '拍卖活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '用户头像URL',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `last_active_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  `is_online` BOOLEAN DEFAULT TRUE COMMENT '是否在线',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auction_user` (`auction_id`, `user_id`),
  INDEX `idx_online` (`auction_id`, `is_online`, `last_active_time`),
  INDEX `idx_auction` (`auction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播间用户表';

-- ============================================================
-- 初始化测试数据（可选）
-- ============================================================

-- 插入测试聊天消息
-- INSERT INTO `chat_messages` (`auction_id`, `user_id`, `username`, `content`, `message_type`) VALUES
-- (1, 1001, '张三', '大家好，这个拍品不错！', 1),
-- (1, 1002, '李四', '是的，价格也很合理', 1),
-- (1, 1003, '王五', '我已经出价了', 1);

-- 插入测试在线用户
-- INSERT INTO `live_room_users` (`auction_id`, `user_id`, `username`, `avatar`, `is_online`) VALUES
-- (1, 1001, '张三', 'https://example.com/avatar/1001.png', TRUE),
-- (1, 1002, '李四', 'https://example.com/avatar/1002.png', TRUE),
-- (1, 1003, '王五', 'https://example.com/avatar/1003.png', TRUE);