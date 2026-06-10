/*
 Navicat Premium Data Transfer

 Source Server         : weizixuan
 Source Server Type    : MySQL
 Source Server Version : 80033 (8.0.33)
 Source Host           : localhost:3306
 Source Schema         : auction

 Target Server Type    : MySQL
 Target Server Version : 80033 (8.0.33)
 File Encoding         : 65001

 Date: 09/06/2026 02:30:54

 ⚠️ 重构说明：
 1. 删除auction_rooms表，功能合并到auctions表
 2. auctions表重构为活动容器，不再直接关联商品
 3. auction_items表的room_id改为auction_id
 4. 更新所有关联表的外键约束
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for auctions (重构后)
-- ----------------------------
DROP TABLE IF EXISTS `auctions`;
CREATE TABLE `auctions`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '竞拍活动ID（拍卖会）',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '活动描述',
  `host_id` bigint NOT NULL COMMENT '创建者ID（商家或管理员）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待开始, ACTIVE-进行中, ENDED-已结束, CANCELLED-已取消',
  `start_time` datetime NOT NULL COMMENT '活动开始时间',
  `end_time` datetime NOT NULL COMMENT '活动结束时间（可延长）',
  `min_deposit` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '最低保证金要求',
  `max_items` int NOT NULL DEFAULT 50 COMMENT '最大拍品数量限制',
  `viewer_count` int NULL DEFAULT 0 COMMENT '当前在线观看人数',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_time`(`start_time` ASC, `end_time` ASC) USING BTREE,
  INDEX `idx_host`(`host_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '竞拍活动表（拍卖会）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for auction_items (重构后)
-- ----------------------------
DROP TABLE IF EXISTS `auction_items`;
CREATE TABLE `auction_items`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '竞拍项ID',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `auction_id` bigint NOT NULL COMMENT '所属活动ID',
  `product_id` bigint NOT NULL COMMENT '关联商品ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '拍品标题',
  `start_price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '起拍价',
  `bid_increment` decimal(10, 2) NOT NULL COMMENT '加价幅度',
  `max_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '最高限价',
  `delay_seconds` int NOT NULL DEFAULT 15 COMMENT '延时拍卖秒数',
  `start_time` datetime NULL COMMENT '开始时间（可覆盖活动时间）',
  `end_time` datetime NULL COMMENT '结束时间（可延长）',
  `original_end_time` datetime NULL COMMENT '原始结束时间（不因延时而变化）',
  `current_price` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '当前价格',
  `highest_bidder` bigint NULL DEFAULT NULL COMMENT '当前最高出价者ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待开始, ACTIVE-进行中, ENDED-已结束, UNSOLD-流拍, CANCELLED-已取消',
  `bid_count` int NULL DEFAULT 0 COMMENT '出价次数',
  `display_order` int NULL DEFAULT 0 COMMENT '展示顺序',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_auction`(`auction_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_time`(`start_time` ASC, `end_time` ASC) USING BTREE,
  INDEX `idx_product`(`product_id` ASC) USING BTREE,
  INDEX `idx_auction_status`(`auction_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `fk_auction_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_auction_items_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '竞拍项表（拍品）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for auto_bid_configs (重构后)
-- ----------------------------
DROP TABLE IF EXISTS `auto_bid_configs`;
CREATE TABLE `auto_bid_configs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `auction_item_id` bigint NOT NULL COMMENT '竞拍项ID（替代item_id）',
  `max_price` decimal(10, 2) NOT NULL COMMENT '最高心理价位',
  `strategy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '策略: LAST_SEC-最后秒出价, SMART-智能出价, AGGRESSIVE-激进出价',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-激活, PAUSED-暂停, COMPLETED-完成, CANCELLED-取消',
  `bid_count` int NULL DEFAULT 0 COMMENT '已出价次数',
  `current_bid` decimal(10, 2) NULL DEFAULT NULL COMMENT '当前出价',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_item`(`user_id` ASC, `auction_item_id` ASC) USING BTREE,
  INDEX `idx_item`(`auction_item_id` ASC) USING BTREE,
  CONSTRAINT `fk_auto_bid_configs_item` FOREIGN KEY (`auction_item_id`) REFERENCES `auction_items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_auto_bid_configs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '代理出价配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bids (重构后)
-- ----------------------------
DROP TABLE IF EXISTS `bids`;
CREATE TABLE `bids`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '出价ID',
  `auction_item_id` bigint NOT NULL COMMENT '竞拍项ID（替代item_id）',
  `auction_id` bigint NOT NULL COMMENT '竞拍活动ID',
  `user_id` bigint NOT NULL COMMENT '出价用户ID',
  `amount` decimal(10, 2) NOT NULL COMMENT '出价金额',
  `rank_when_bid` int NULL DEFAULT NULL COMMENT '出价时的排名',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-有效, WINNER-获胜, EXPIRED-已过期',
  `is_auto_bid` tinyint(1) NULL DEFAULT 0 COMMENT '是否为代理出价',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_item`(`auction_item_id` ASC) USING BTREE,
  INDEX `idx_auction`(`auction_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_item_amount`(`auction_item_id` ASC, `amount` DESC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  CONSTRAINT `fk_bids_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_bids_item` FOREIGN KEY (`auction_item_id`) REFERENCES `auction_items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_bids_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '出价记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for orders (重构后)
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `auction_id` bigint NOT NULL COMMENT '竞拍活动ID',
  `auction_item_id` bigint NOT NULL COMMENT '竞拍项ID（拍品）',
  `user_id` bigint NOT NULL COMMENT '用户ID（获胜者）',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `final_amount` decimal(10, 2) NOT NULL COMMENT '成交金额',
  `deposit_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '保证金抵扣金额',
  `payable_amount` decimal(10, 2) NOT NULL COMMENT '应付金额（成交价-保证金）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态: PENDING_PAYMENT-待支付, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_auction`(`auction_id` ASC) USING BTREE,
  INDEX `idx_item`(`auction_item_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_product`(`product_id` ASC) USING BTREE,
  CONSTRAINT `fk_orders_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_orders_item` FOREIGN KEY (`auction_item_id`) REFERENCES `auction_items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_orders_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for risk_events (重构后)
-- ----------------------------
DROP TABLE IF EXISTS `risk_events`;
CREATE TABLE `risk_events`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `auction_id` bigint NOT NULL COMMENT '活动ID（替代room_id）',
  `auction_item_id` bigint NULL DEFAULT NULL COMMENT '竞拍项ID',
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型: RAPID_BIDDING-快速出价, ABNORMAL_PATTERN-异常模式, AUTO_BID_DETECTED-代理出价',
  `severity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '严重程度: INFO-信息, LOW-低, MEDIUM-中, HIGH-高, CRITICAL-严重',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '事件描述',
  `metadata` json NULL COMMENT '元数据(JSON格式)',
  `action_taken` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '采取的行动: ALLOW-允许, MONITOR-监控, BLOCK-拦截, MANUAL_REVIEW-人工审核',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_severity`(`severity` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_auction`(`auction_id` ASC) USING BTREE,
  INDEX `idx_event_type`(`event_type` ASC) USING BTREE,
  CONSTRAINT `fk_risk_events_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '风控事件记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_behaviors (重构后)
-- ----------------------------
DROP TABLE IF EXISTS `user_behaviors`;
CREATE TABLE `user_behaviors`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `auction_id` bigint NOT NULL COMMENT '活动ID（替代room_id）',
  `auction_item_id` bigint NULL DEFAULT NULL COMMENT '竞拍项ID',
  `bid_count` int NULL DEFAULT NULL COMMENT '出价次数',
  `avg_bid_interval` int NULL DEFAULT NULL COMMENT '平均出价间隔(秒)',
  `last_bid_time` datetime NULL DEFAULT NULL COMMENT '最后出价时间',
  `risk_score` decimal(3, 2) NULL DEFAULT NULL COMMENT '风险评分(0-1)',
  `risk_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '风险等级: LOW-低, MEDIUM-中, HIGH-高',
  `is_blocked` tinyint(1) NULL DEFAULT 0 COMMENT '是否被拦截',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_risk`(`risk_score` ASC, `is_blocked` ASC) USING BTREE,
  INDEX `idx_user_auction`(`user_id` ASC, `auction_id` ASC) USING BTREE,
  INDEX `idx_auction`(`auction_id` ASC) USING BTREE,
  CONSTRAINT `fk_user_behaviors_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户行为表' ROW_FORMAT = Dynamic;

-- 其他表保持不变
-- (products, users, roles, permissions, role_permissions, user_roles, login_logs, operation_logs)

SET FOREIGN_KEY_CHECKS = 1;
