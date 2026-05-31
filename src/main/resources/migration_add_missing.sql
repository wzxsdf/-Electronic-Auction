-- ====================================================
-- 数据库迁移脚本：从auction.sql迁移到完整版本
-- 生成时间: 2026-05-30
-- 说明：用于在现有auction.sql基础上添加缺失的表和字段
-- ====================================================

SET NAMES utf8mb4;

-- ====================================================
-- 1. 修改users表 - 添加缺失字段
-- ====================================================

ALTER TABLE `users`
ADD COLUMN `password` varchar(255) NOT NULL COMMENT '密码(BCrypt加密)' AFTER `username`,
ADD COLUMN `email` varchar(100) DEFAULT NULL COMMENT '邮箱' AFTER `avatar_url`,
ADD COLUMN `phone` varchar(20) DEFAULT NULL COMMENT '手机号' AFTER `email`,
ADD COLUMN `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态' AFTER `phone`,
ADD COLUMN `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间' AFTER `status`,
ADD COLUMN `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP' AFTER `last_login_at`,
ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `total_wins`,
ADD UNIQUE INDEX `uk_username` (`username`),
ADD INDEX `idx_email` (`email`),
ADD INDEX `idx_phone` (`phone`),
ADD INDEX `idx_status` (`status`);

-- 为现有用户添加默认密码（均为123456）
UPDATE `users` SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH' WHERE `password` IS NULL OR `password` = '';
UPDATE `users` SET `status` = 'ACTIVE' WHERE `status` IS NULL;

-- ====================================================
-- 2. 创建auctions表（竞拍活动表）
-- ====================================================

CREATE TABLE IF NOT EXISTS `auctions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '竞拍ID',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `title` varchar(200) NOT NULL COMMENT '竞拍标题',
  `start_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '起拍价',
  `bid_increment` decimal(10,2) NOT NULL COMMENT '加价幅度',
  `max_price` decimal(10,2) DEFAULT NULL COMMENT '最高限价',
  `delay_seconds` int NOT NULL DEFAULT 15 COMMENT '延时拍卖秒数',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `original_end_time` datetime NOT NULL COMMENT '原始结束时间',
  `current_price` decimal(10,2) DEFAULT 0.00 COMMENT '当前价格',
  `highest_bidder` bigint DEFAULT NULL COMMENT '当前最高出价者ID',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `winner_id` bigint DEFAULT NULL COMMENT '获胜者ID',
  `final_price` decimal(10,2) DEFAULT NULL COMMENT '最终成交价',
  `settled_at` datetime DEFAULT NULL COMMENT '结算时间',
  `room_id` bigint DEFAULT NULL COMMENT '竞拍房间ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_time` (`start_time`, `end_time`) USING BTREE,
  KEY `idx_product` (`product_id`) USING BTREE,
  KEY `idx_room` (`room_id`) USING BTREE,
  CONSTRAINT `fk_auctions_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_auctions_room` FOREIGN KEY (`room_id`) REFERENCES `auction_rooms` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞拍活动表';

-- ====================================================
-- 3. 创建权限管理相关表
-- ====================================================

-- 角色表
CREATE TABLE IF NOT EXISTS `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `code` varchar(50) NOT NULL COMMENT '角色编码',
  `name` varchar(100) NOT NULL COMMENT '角色名称',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `code` varchar(100) NOT NULL COMMENT '权限编码',
  `name` varchar(100) NOT NULL COMMENT '权限名称',
  `resource` varchar(255) DEFAULT NULL COMMENT '资源路径',
  `action` varchar(50) DEFAULT NULL COMMENT '操作类型',
  `description` varchar(255) DEFAULT NULL COMMENT '权限描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `user_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE,
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE,
  KEY `idx_permission_id` (`permission_id`) USING BTREE,
  CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ====================================================
-- 4. 创建日志相关表
-- ====================================================

-- 登录日志表
CREATE TABLE IF NOT EXISTS `login_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `login_type` varchar(20) DEFAULT NULL COMMENT '登录类型',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '浏览器UA',
  `status` varchar(20) NOT NULL COMMENT '登录状态',
  `failure_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  CONSTRAINT `fk_login_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
  `module` varchar(50) DEFAULT NULL COMMENT '操作模块',
  `operation` varchar(50) DEFAULT NULL COMMENT '操作类型',
  `method` varchar(255) DEFAULT NULL COMMENT '请求方法',
  `params` text COMMENT '请求参数',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `status` varchar(20) NOT NULL COMMENT '操作状态',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `duration` int DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_module` (`module`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  CONSTRAINT `fk_operation_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ====================================================
-- 5. 修改auto_bid_configs表 - 添加auction_id字段
-- ====================================================

ALTER TABLE `auto_bid_configs`
ADD COLUMN `auction_id` bigint NOT NULL COMMENT '竞拍活动ID' AFTER `item_id`,
ADD INDEX `idx_auction` (`auction_id`),
ADD CONSTRAINT `fk_auto_bid_configs_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE RESTRICT;

-- ====================================================
-- 6. 修改bids表 - 添加auction_id字段
-- ====================================================

ALTER TABLE `bids`
ADD COLUMN `auction_id` bigint NOT NULL COMMENT '竞拍活动ID' AFTER `item_id`,
ADD INDEX `idx_auction` (`auction_id`),
ADD CONSTRAINT `fk_bids_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE RESTRICT;

-- ====================================================
-- 7. 修改orders表 - 添加缺失的外键约束
-- ====================================================

ALTER TABLE `orders`
ADD CONSTRAINT `fk_orders_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT;

-- ====================================================
-- 迁移完成
-- ====================================================
