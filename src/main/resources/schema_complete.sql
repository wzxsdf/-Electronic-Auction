-- ====================================================
-- 电子拍卖系统完整数据库脚本
-- 生成时间: 2026-05-30
-- 说明: 包含所有业务模块的表结构和初始化数据
-- ====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ====================================================
-- 1. 用户与权限模块
-- ====================================================

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `nickname` varchar(100) DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态: ACTIVE-激活, LOCKED-锁定, DISABLED-禁用',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `total_bids` int DEFAULT 0 COMMENT '总出价次数',
  `total_wins` int DEFAULT 0 COMMENT '总获胜次数',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE,
  KEY `idx_email` (`email`) USING BTREE,
  KEY `idx_phone` (`phone`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `code` varchar(50) NOT NULL COMMENT '角色编码: ADMIN-管理员, USER-普通用户',
  `name` varchar(100) NOT NULL COMMENT '角色名称',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- Table structure for permissions
-- ----------------------------
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `code` varchar(100) NOT NULL COMMENT '权限编码',
  `name` varchar(100) NOT NULL COMMENT '权限名称',
  `resource` varchar(255) DEFAULT NULL COMMENT '资源路径',
  `action` varchar(50) DEFAULT NULL COMMENT '操作类型: READ-读, WRITE-写, DELETE-删除',
  `description` varchar(255) DEFAULT NULL COMMENT '权限描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ----------------------------
-- Table structure for user_roles
-- ----------------------------
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles` (
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

-- ----------------------------
-- Table structure for role_permissions
-- ----------------------------
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE `role_permissions` (
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

-- ----------------------------
-- Table structure for login_logs
-- ----------------------------
DROP TABLE IF EXISTS `login_logs`;
CREATE TABLE `login_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `login_type` varchar(20) DEFAULT NULL COMMENT '登录类型: PASSWORD-密码, SOCIAL-社交, SSO-单点登录',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '浏览器UA',
  `status` varchar(20) NOT NULL COMMENT '登录状态: SUCCESS-成功, FAILED-失败',
  `failure_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  CONSTRAINT `fk_login_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ----------------------------
-- Table structure for operation_logs
-- ----------------------------
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
  `module` varchar(50) DEFAULT NULL COMMENT '操作模块',
  `operation` varchar(50) DEFAULT NULL COMMENT '操作类型',
  `method` varchar(255) DEFAULT NULL COMMENT '请求方法',
  `params` text COMMENT '请求参数',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `status` varchar(20) NOT NULL COMMENT '操作状态: SUCCESS-成功, FAILED-失败',
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
-- 2. 商品模块
-- ====================================================

-- ----------------------------
-- Table structure for products
-- ----------------------------
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` varchar(200) NOT NULL COMMENT '商品名称',
  `image_url` varchar(500) DEFAULT NULL COMMENT '商品图片URL',
  `description` text COMMENT '商品描述',
  `category` varchar(50) DEFAULT NULL COMMENT '商品分类',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ====================================================
-- 3. 竞拍核心模块
-- ====================================================

-- ----------------------------
-- Table structure for auction_rooms
-- ----------------------------
DROP TABLE IF EXISTS `auction_rooms`;
CREATE TABLE `auction_rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房间ID',
  `title` varchar(200) NOT NULL COMMENT '房间标题',
  `description` text COMMENT '房间描述',
  `host_id` bigint DEFAULT NULL COMMENT '主持人ID',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '房间状态: PENDING-待开始, LIVE-进行中, ENDED-已结束',
  `viewer_count` int DEFAULT 0 COMMENT '观看人数',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_time` (`start_time`, `end_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞拍房间表';

-- ----------------------------
-- Table structure for auction_items
-- ----------------------------
DROP TABLE IF EXISTS `auction_items`;
CREATE TABLE `auction_items` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '竞拍项ID',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `room_id` bigint NOT NULL COMMENT '房间ID',
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
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待开始, ACTIVE-进行中, COMPLETED-已完成',
  `bid_count` int DEFAULT 0 COMMENT '出价次数',
  `display_order` int DEFAULT 0 COMMENT '展示顺序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_room` (`room_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_time` (`start_time`, `end_time`) USING BTREE,
  KEY `idx_product` (`product_id`) USING BTREE,
  CONSTRAINT `fk_auction_items_room` FOREIGN KEY (`room_id`) REFERENCES `auction_rooms` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_auction_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞拍项表';

-- ----------------------------
-- Table structure for auctions
-- ----------------------------
DROP TABLE IF EXISTS `auctions`;
CREATE TABLE `auctions` (
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
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待开始, ACTIVE-进行中, COMPLETED-已完成',
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

-- ----------------------------
-- Table structure for bids
-- ----------------------------
DROP TABLE IF EXISTS `bids`;
CREATE TABLE `bids` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '出价ID',
  `item_id` bigint NOT NULL COMMENT '竞拍项ID',
  `auction_id` bigint NOT NULL COMMENT '竞拍活动ID',
  `user_id` bigint NOT NULL COMMENT '出价用户ID',
  `amount` decimal(10,2) NOT NULL COMMENT '出价金额',
  `rank_when_bid` int DEFAULT NULL COMMENT '出价时的排名',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-有效, WINNER-获胜, EXPIRED-已过期',
  `is_auto_bid` tinyint(1) DEFAULT 0 COMMENT '是否为代理出价',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_item` (`item_id`) USING BTREE,
  KEY `idx_auction` (`auction_id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_item_amount` (`item_id`, `amount` DESC) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  CONSTRAINT `fk_bids_item` FOREIGN KEY (`item_id`) REFERENCES `auction_items` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_bids_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_bids_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出价记录表';

-- ----------------------------
-- Table structure for auto_bid_configs
-- ----------------------------
DROP TABLE IF EXISTS `auto_bid_configs`;
CREATE TABLE `auto_bid_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `item_id` bigint NOT NULL COMMENT '竞拍项ID',
  `auction_id` bigint NOT NULL COMMENT '竞拍活动ID',
  `max_price` decimal(10,2) NOT NULL COMMENT '最高心理价位',
  `strategy` varchar(20) NOT NULL COMMENT '策略: LAST_SEC-最后秒出价, SMART-智能出价, AGGRESSIVE-激进出价',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-激活, PAUSED-暂停, COMPLETED-完成, CANCELLED-取消',
  `bid_count` int DEFAULT 0 COMMENT '已出价次数',
  `current_bid` decimal(10,2) DEFAULT NULL COMMENT '当前出价',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_item` (`user_id`, `item_id`) USING BTREE,
  KEY `idx_item` (`item_id`) USING BTREE,
  KEY `idx_auction` (`auction_id`) USING BTREE,
  CONSTRAINT `fk_auto_bid_configs_item` FOREIGN KEY (`item_id`) REFERENCES `auction_items` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_auto_bid_configs_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_auto_bid_configs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理出价配置表';

-- ====================================================
-- 4. 订单与结算模块
-- ====================================================

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `room_id` bigint NOT NULL COMMENT '竞拍房间ID',
  `item_id` bigint NOT NULL COMMENT '竞拍项ID',
  `user_id` bigint NOT NULL COMMENT '用户ID(获胜者)',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `final_amount` decimal(10,2) NOT NULL COMMENT '成交金额',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态: PENDING_PAYMENT-待支付, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_room` (`room_id`) USING BTREE,
  KEY `idx_item` (`item_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  CONSTRAINT `fk_orders_room` FOREIGN KEY (`room_id`) REFERENCES `auction_rooms` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_orders_item` FOREIGN KEY (`item_id`) REFERENCES `auction_items` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_orders_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ====================================================
-- 5. 风控与行为分析模块
-- ====================================================

-- ----------------------------
-- Table structure for user_behaviors
-- ----------------------------
DROP TABLE IF EXISTS `user_behaviors`;
CREATE TABLE `user_behaviors` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `room_id` bigint NOT NULL COMMENT '房间ID',
  `item_id` bigint DEFAULT NULL COMMENT '竞拍项ID',
  `bid_count` int DEFAULT NULL COMMENT '出价次数',
  `avg_bid_interval` int DEFAULT NULL COMMENT '平均出价间隔(秒)',
  `last_bid_time` datetime DEFAULT NULL COMMENT '最后出价时间',
  `risk_score` decimal(3,2) DEFAULT NULL COMMENT '风险评分(0-1)',
  `risk_level` varchar(20) DEFAULT NULL COMMENT '风险等级: LOW-低, MEDIUM-中, HIGH-高',
  `is_blocked` tinyint(1) DEFAULT 0 COMMENT '是否被拦截',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_risk` (`risk_score`, `is_blocked`) USING BTREE,
  KEY `idx_user_room` (`user_id`, `room_id`) USING BTREE,
  KEY `idx_room` (`room_id`) USING BTREE,
  CONSTRAINT `fk_user_behaviors_room` FOREIGN KEY (`room_id`) REFERENCES `auction_rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为表';

-- ----------------------------
-- Table structure for risk_events
-- ----------------------------
DROP TABLE IF EXISTS `risk_events`;
CREATE TABLE `risk_events` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `room_id` bigint NOT NULL COMMENT '房间ID',
  `item_id` bigint DEFAULT NULL COMMENT '竞拍项ID',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型: RAPID_BIDDING-快速出价, ABNORMAL_PATTERN-异常模式, AUTO_BID_DETECTED-代理出价',
  `severity` varchar(20) NOT NULL COMMENT '严重程度: INFO-信息, LOW-低, MEDIUM-中, HIGH-高, CRITICAL-严重',
  `description` text COMMENT '事件描述',
  `metadata` json DEFAULT NULL COMMENT '元数据(JSON格式)',
  `action_taken` varchar(50) DEFAULT NULL COMMENT '采取的行动: ALLOW-允许, MONITOR-监控, BLOCK-拦截, MANUAL_REVIEW-人工审核',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_severity` (`severity`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_room` (`room_id`) USING BTREE,
  KEY `idx_event_type` (`event_type`) USING BTREE,
  CONSTRAINT `fk_risk_events_room` FOREIGN KEY (`room_id`) REFERENCES `auction_rooms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控事件记录表';

SET FOREIGN_KEY_CHECKS = 1;
