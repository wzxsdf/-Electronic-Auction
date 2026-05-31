/*
 Navicat Premium Data Transfer

 Source Server         : xxx
 Source Server Type    : MySQL
 Source Server Version : 80036
 Source Host           : localhost:3306
 Source Schema         : auction

 Target Server Type    : MySQL
 Target Server Version : 80036
 File Encoding         : 65001

 Date: 23/05/2026 20:14:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for auction_items
-- ----------------------------
DROP TABLE IF EXISTS `auction_items`;
CREATE TABLE `auction_items`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `room_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `start_price` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `bid_increment` decimal(10, 2) NOT NULL,
  `max_price` decimal(10, 2) NULL DEFAULT NULL,
  `delay_seconds` int NOT NULL DEFAULT 15,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `original_end_time` datetime NOT NULL,
  `current_price` decimal(10, 2) NULL DEFAULT 0.00,
  `highest_bidder` bigint NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `bid_count` int NULL DEFAULT 0,
  `display_order` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_room`(`room_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_time`(`start_time` ASC, `end_time` ASC) USING BTREE,
  INDEX `product_id`(`product_id` ASC) USING BTREE,
  CONSTRAINT `auction_items_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `auction_rooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `auction_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auction_items
-- ----------------------------
INSERT INTO `auction_items` VALUES (1, 0, 1, 1, 'Jade Bangle Auction', 5000.00, 200.00, 50000.00, 15, '2026-05-23 08:21:22', '2026-05-23 09:21:22', '2026-05-23 09:21:22', 6410.00, 1, 'ACTIVE', 0, 1, '2026-05-23 09:30:25', '2026-05-23 09:30:25');
INSERT INTO `auction_items` VALUES (2, 0, 1, 2, 'Gold Necklace Auction', 2000.00, 100.00, 30000.00, 15, '2026-05-23 08:36:22', '2026-05-23 09:36:22', '2026-05-23 09:36:22', 4150.00, 1, 'ACTIVE', 0, 2, '2026-05-23 09:30:25', '2026-05-23 09:30:25');
INSERT INTO `auction_items` VALUES (3, 0, 1, 3, 'Ming Porcelain Auction', 10000.00, 500.00, 150000.00, 20, '2026-05-23 08:41:22', '2026-05-23 09:41:22', '2026-05-23 09:41:22', 12500.00, 5, 'ACTIVE', 0, 3, '2026-05-23 09:30:25', '2026-05-23 09:30:25');
INSERT INTO `auction_items` VALUES (4, 0, 1, 4, 'Teapot Auction', 3000.00, 150.00, 40000.00, 15, '2026-05-23 09:51:22', '2026-05-23 12:51:22', '2026-05-23 12:51:22', 3000.00, NULL, 'PENDING', 0, 4, '2026-05-23 09:30:25', '2026-05-23 09:30:25');
INSERT INTO `auction_items` VALUES (5, 0, 1, 5, 'Swiss Watch Auction', 8000.00, 300.00, 80000.00, 20, '2026-05-23 10:51:22', '2026-05-23 14:51:22', '2026-05-23 14:51:22', 8000.00, NULL, 'PENDING', 0, 5, '2026-05-23 09:30:25', '2026-05-23 09:30:25');
INSERT INTO `auction_items` VALUES (6, 0, 1, 6, 'Jade Pendant Auction', 1500.00, 100.00, 20000.00, 10, '2026-05-23 03:51:22', '2026-05-23 07:51:22', '2026-05-23 07:51:22', 4800.00, 5, 'COMPLETED', 0, 6, '2026-05-23 09:30:25', '2026-05-23 09:30:25');

-- ----------------------------
-- Table structure for auction_rooms
-- ----------------------------
DROP TABLE IF EXISTS `auction_rooms`;
CREATE TABLE `auction_rooms`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `host_id` bigint NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `viewer_count` int NULL DEFAULT 0,
  `start_time` datetime NOT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_time`(`start_time` ASC, `end_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auction_rooms
-- ----------------------------
INSERT INTO `auction_rooms` VALUES (1, 'Premium Auction Room', NULL, NULL, 'LIVE', 0, '2026-05-23 09:30:25', '2026-05-23 13:30:25', '2026-05-23 09:30:25', '2026-05-23 09:30:25');

-- ----------------------------
-- Table structure for auctions_backup
-- ----------------------------
DROP TABLE IF EXISTS `auctions_backup`;
CREATE TABLE `auctions_backup`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `start_price` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `bid_increment` decimal(10, 2) NOT NULL,
  `max_price` decimal(10, 2) NULL DEFAULT NULL,
  `delay_seconds` int NOT NULL DEFAULT 15,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `original_end_time` datetime NOT NULL,
  `current_price` decimal(10, 2) NULL DEFAULT 0.00,
  `highest_bidder` bigint NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_time`(`start_time` ASC, `end_time` ASC) USING BTREE,
  INDEX `product_id`(`product_id` ASC) USING BTREE,
  CONSTRAINT `auctions_backup_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '竞拍活动表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auctions_backup
-- ----------------------------
INSERT INTO `auctions_backup` VALUES (1, 1, 'Jade Bangle Auction', 5000.00, 200.00, 50000.00, 15, '2026-05-23 08:21:22', '2026-05-23 09:21:22', '2026-05-23 09:21:22', 6410.00, 1, 'ACTIVE', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `auctions_backup` VALUES (2, 2, 'Gold Necklace Auction', 2000.00, 100.00, 30000.00, 15, '2026-05-23 08:36:22', '2026-05-23 09:36:22', '2026-05-23 09:36:22', 4150.00, 1, 'ACTIVE', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `auctions_backup` VALUES (3, 3, 'Ming Porcelain Auction', 10000.00, 500.00, 150000.00, 20, '2026-05-23 08:41:22', '2026-05-23 09:41:22', '2026-05-23 09:41:22', 12500.00, 5, 'ACTIVE', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `auctions_backup` VALUES (4, 4, 'Teapot Auction', 3000.00, 150.00, 40000.00, 15, '2026-05-23 09:51:22', '2026-05-23 12:51:22', '2026-05-23 12:51:22', 3000.00, NULL, 'PENDING', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `auctions_backup` VALUES (5, 5, 'Swiss Watch Auction', 8000.00, 300.00, 80000.00, 20, '2026-05-23 10:51:22', '2026-05-23 14:51:22', '2026-05-23 14:51:22', 8000.00, NULL, 'PENDING', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `auctions_backup` VALUES (6, 6, 'Jade Pendant Auction', 1500.00, 100.00, 20000.00, 10, '2026-05-23 03:51:22', '2026-05-23 07:51:22', '2026-05-23 07:51:22', 4800.00, 5, 'COMPLETED', '2026-05-23 08:51:22', '2026-05-23 08:51:22');

-- ----------------------------
-- Table structure for auto_bid_configs
-- ----------------------------
DROP TABLE IF EXISTS `auto_bid_configs`;
CREATE TABLE `auto_bid_configs`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `auction_id` bigint NOT NULL,
  `max_price` decimal(10, 2) NOT NULL,
  `strategy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ACTIVE',
  `bid_count` int NULL DEFAULT 0,
  `current_bid` decimal(10, 2) NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_item`(`user_id` ASC, `item_id` ASC) USING BTREE,
  INDEX `idx_item`(`item_id` ASC) USING BTREE,
  INDEX `auction_id`(`auction_id` ASC) USING BTREE,
  CONSTRAINT `auto_bid_configs_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `auction_items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `auto_bid_configs_ibfk_2` FOREIGN KEY (`auction_id`) REFERENCES `auction_rooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auto_bid_configs
-- ----------------------------

-- ----------------------------
-- Table structure for auto_bid_configs_backup
-- ----------------------------
DROP TABLE IF EXISTS `auto_bid_configs_backup`;
CREATE TABLE `auto_bid_configs_backup`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `auction_id` bigint NOT NULL,
  `max_price` decimal(10, 2) NOT NULL,
  `strategy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'LAST_SEC/SMART/AGGRESSIVE',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ACTIVE',
  `bid_count` int NULL DEFAULT 0,
  `current_bid` decimal(10, 2) NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_auction`(`user_id` ASC, `auction_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代理出价配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of auto_bid_configs_backup
-- ----------------------------
INSERT INTO `auto_bid_configs_backup` VALUES (1, 4, 1, 8000.00, 'SMART', 'ACTIVE', 2, 6200.00, '2026-05-23 08:51:23', '2026-05-23 08:51:23');
INSERT INTO `auto_bid_configs_backup` VALUES (2, 5, 2, 5000.00, 'AGGRESSIVE', 'ACTIVE', 0, NULL, '2026-05-23 08:51:23', '2026-05-23 08:51:23');
INSERT INTO `auto_bid_configs_backup` VALUES (3, 5, 3, 20000.00, 'SMART', 'ACTIVE', 2, 12500.00, '2026-05-23 08:51:23', '2026-05-23 08:51:23');
INSERT INTO `auto_bid_configs_backup` VALUES (4, 1, 4, 12000.00, 'LAST_SEC', 'ACTIVE', 0, NULL, '2026-05-23 08:51:23', '2026-05-23 08:51:23');
INSERT INTO `auto_bid_configs_backup` VALUES (5, 2, 5, 25000.00, 'SMART', 'ACTIVE', 0, NULL, '2026-05-23 08:51:23', '2026-05-23 08:51:23');

-- ----------------------------
-- Table structure for bids
-- ----------------------------
DROP TABLE IF EXISTS `bids`;
CREATE TABLE `bids`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_id` bigint NOT NULL,
  `auction_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `amount` decimal(10, 2) NOT NULL,
  `rank_when_bid` int NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE',
  `is_auto_bid` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_item`(`item_id` ASC) USING BTREE,
  INDEX `idx_auction`(`auction_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_item_amount`(`item_id` ASC, `amount` DESC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE,
  CONSTRAINT `bids_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `auction_items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `bids_ibfk_2` FOREIGN KEY (`auction_id`) REFERENCES `auction_rooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `bids_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bids
-- ----------------------------
INSERT INTO `bids` VALUES (1, 1, 1, 1, 5000.00, 1, 'ACTIVE', 0, '2026-05-23 00:00:00');
INSERT INTO `bids` VALUES (2, 1, 1, 2, 5200.00, 1, 'ACTIVE', 0, '2026-05-23 00:05:00');
INSERT INTO `bids` VALUES (3, 1, 1, 3, 5400.00, 2, 'ACTIVE', 0, '2026-05-23 00:08:00');
INSERT INTO `bids` VALUES (4, 1, 1, 4, 5800.00, 1, 'ACTIVE', 0, '2026-05-23 00:12:00');
INSERT INTO `bids` VALUES (5, 1, 1, 2, 6000.00, 2, 'ACTIVE', 0, '2026-05-23 00:15:00');
INSERT INTO `bids` VALUES (6, 1, 1, 4, 6200.00, 1, 'ACTIVE', 0, '2026-05-23 00:18:00');
INSERT INTO `bids` VALUES (7, 2, 1, 3, 2000.00, 1, 'ACTIVE', 0, '2026-05-23 00:20:00');
INSERT INTO `bids` VALUES (8, 2, 1, 1, 2100.00, 2, 'ACTIVE', 0, '2026-05-23 00:22:00');
INSERT INTO `bids` VALUES (9, 2, 1, 2, 2500.00, 1, 'ACTIVE', 0, '2026-05-23 00:25:00');
INSERT INTO `bids` VALUES (10, 2, 1, 4, 3000.00, 2, 'ACTIVE', 0, '2026-05-23 00:28:00');
INSERT INTO `bids` VALUES (11, 2, 1, 2, 3500.00, 1, 'ACTIVE', 0, '2026-05-23 00:30:00');
INSERT INTO `bids` VALUES (12, 3, 1, 5, 10000.00, 1, 'ACTIVE', 0, '2026-05-23 00:25:00');
INSERT INTO `bids` VALUES (13, 3, 1, 4, 10500.00, 2, 'ACTIVE', 0, '2026-05-23 00:28:00');
INSERT INTO `bids` VALUES (14, 3, 1, 5, 11000.00, 1, 'ACTIVE', 1, '2026-05-23 00:30:00');
INSERT INTO `bids` VALUES (15, 3, 1, 4, 12000.00, 2, 'ACTIVE', 0, '2026-05-23 00:32:00');
INSERT INTO `bids` VALUES (16, 3, 1, 5, 12500.00, 1, 'ACTIVE', 1, '2026-05-23 00:33:00');
INSERT INTO `bids` VALUES (17, 6, 1, 1, 1500.00, 1, 'ACTIVE', 0, '2026-05-22 18:00:00');
INSERT INTO `bids` VALUES (18, 6, 1, 2, 1800.00, 1, 'ACTIVE', 0, '2026-05-22 18:15:00');
INSERT INTO `bids` VALUES (19, 6, 1, 3, 2200.00, 2, 'ACTIVE', 0, '2026-05-22 18:30:00');
INSERT INTO `bids` VALUES (20, 6, 1, 4, 3500.00, 1, 'ACTIVE', 0, '2026-05-22 19:00:00');
INSERT INTO `bids` VALUES (21, 6, 1, 5, 4800.00, 1, 'WINNER', 0, '2026-05-22 19:45:00');
INSERT INTO `bids` VALUES (22, 1, 1, 1, 6410.00, NULL, 'ACTIVE', 0, '2026-05-23 09:16:00');
INSERT INTO `bids` VALUES (23, 2, 1, 1, 4150.00, NULL, 'ACTIVE', 0, '2026-05-23 09:24:16');

-- ----------------------------
-- Table structure for bids_backup
-- ----------------------------
DROP TABLE IF EXISTS `bids_backup`;
CREATE TABLE `bids_backup`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `auction_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `amount` decimal(10, 2) NOT NULL,
  `rank_when_bid` int NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE',
  `is_auto_bid` tinyint(1) NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_auction_user`(`auction_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_auction_amount`(`auction_id` ASC, `amount` DESC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `bids_backup_ibfk_1` FOREIGN KEY (`auction_id`) REFERENCES `auctions_backup` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `bids_backup_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '出价记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bids_backup
-- ----------------------------
INSERT INTO `bids_backup` VALUES (1, 1, 1, 5000.00, 1, 'ACTIVE', 0, '2026-05-23 00:00:00');
INSERT INTO `bids_backup` VALUES (2, 1, 2, 5200.00, 1, 'ACTIVE', 0, '2026-05-23 00:05:00');
INSERT INTO `bids_backup` VALUES (3, 1, 3, 5400.00, 2, 'ACTIVE', 0, '2026-05-23 00:08:00');
INSERT INTO `bids_backup` VALUES (4, 1, 4, 5800.00, 1, 'ACTIVE', 0, '2026-05-23 00:12:00');
INSERT INTO `bids_backup` VALUES (5, 1, 2, 6000.00, 2, 'ACTIVE', 0, '2026-05-23 00:15:00');
INSERT INTO `bids_backup` VALUES (6, 1, 4, 6200.00, 1, 'ACTIVE', 0, '2026-05-23 00:18:00');
INSERT INTO `bids_backup` VALUES (7, 2, 3, 2000.00, 1, 'ACTIVE', 0, '2026-05-23 00:20:00');
INSERT INTO `bids_backup` VALUES (8, 2, 1, 2100.00, 2, 'ACTIVE', 0, '2026-05-23 00:22:00');
INSERT INTO `bids_backup` VALUES (9, 2, 2, 2500.00, 1, 'ACTIVE', 0, '2026-05-23 00:25:00');
INSERT INTO `bids_backup` VALUES (10, 2, 4, 3000.00, 2, 'ACTIVE', 0, '2026-05-23 00:28:00');
INSERT INTO `bids_backup` VALUES (11, 2, 2, 3500.00, 1, 'ACTIVE', 0, '2026-05-23 00:30:00');
INSERT INTO `bids_backup` VALUES (12, 3, 5, 10000.00, 1, 'ACTIVE', 0, '2026-05-23 00:25:00');
INSERT INTO `bids_backup` VALUES (13, 3, 4, 10500.00, 2, 'ACTIVE', 0, '2026-05-23 00:28:00');
INSERT INTO `bids_backup` VALUES (14, 3, 5, 11000.00, 1, 'ACTIVE', 1, '2026-05-23 00:30:00');
INSERT INTO `bids_backup` VALUES (15, 3, 4, 12000.00, 2, 'ACTIVE', 0, '2026-05-23 00:32:00');
INSERT INTO `bids_backup` VALUES (16, 3, 5, 12500.00, 1, 'ACTIVE', 1, '2026-05-23 00:33:00');
INSERT INTO `bids_backup` VALUES (17, 6, 1, 1500.00, 1, 'ACTIVE', 0, '2026-05-22 18:00:00');
INSERT INTO `bids_backup` VALUES (18, 6, 2, 1800.00, 1, 'ACTIVE', 0, '2026-05-22 18:15:00');
INSERT INTO `bids_backup` VALUES (19, 6, 3, 2200.00, 2, 'ACTIVE', 0, '2026-05-22 18:30:00');
INSERT INTO `bids_backup` VALUES (20, 6, 4, 3500.00, 1, 'ACTIVE', 0, '2026-05-22 19:00:00');
INSERT INTO `bids_backup` VALUES (21, 6, 5, 4800.00, 1, 'WINNER', 0, '2026-05-22 19:45:00');
INSERT INTO `bids_backup` VALUES (22, 1, 1, 6410.00, NULL, 'ACTIVE', 0, '2026-05-23 09:16:00');
INSERT INTO `bids_backup` VALUES (23, 2, 1, 4150.00, NULL, 'ACTIVE', 0, '2026-05-23 09:24:16');

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `item_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `final_amount` decimal(10, 2) NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_room`(`room_id` ASC) USING BTREE,
  INDEX `idx_item`(`item_id` ASC) USING BTREE,
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `auction_rooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `orders_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `auction_items` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `orders_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------

-- ----------------------------
-- Table structure for orders_backup
-- ----------------------------
DROP TABLE IF EXISTS `orders_backup`;
CREATE TABLE `orders_backup`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `auction_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `final_amount` decimal(10, 2) NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_auction`(`auction_id` ASC) USING BTREE,
  CONSTRAINT `orders_backup_ibfk_1` FOREIGN KEY (`auction_id`) REFERENCES `auctions_backup` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `orders_backup_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders_backup
-- ----------------------------
INSERT INTO `orders_backup` VALUES (1, 6, 5, 6, 4800.00, 'PENDING', '2026-05-23 08:51:23', '2026-05-23 08:51:23');

-- ----------------------------
-- Table structure for products
-- ----------------------------
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of products
-- ----------------------------
INSERT INTO `products` VALUES (1, 'Jade Bangle', 'https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?w=400', 'Natural jadeite bangle from Burma', 'Jewelry', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `products` VALUES (2, 'Gold Necklace', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=400', '999 gold necklace 30g', 'Jewelry', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `products` VALUES (3, 'Blue White Porcelain', 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=400', 'Ming Dynasty blue and white vase', 'Ceramics', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `products` VALUES (4, 'Purple Clay Teapot', 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400', 'Master crafted purple clay teapot', 'Tea', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `products` VALUES (5, 'Swiss Watch', 'https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=400', 'Swiss mechanical watch', 'Watch', '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `products` VALUES (6, 'Hetian Jade Pendant', 'https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=400', 'Hetian jade pendant with blessing design', 'Jewelry', '2026-05-23 08:51:22', '2026-05-23 08:51:22');

-- ----------------------------
-- Table structure for risk_events
-- ----------------------------
DROP TABLE IF EXISTS `risk_events`;
CREATE TABLE `risk_events`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `item_id` bigint NULL DEFAULT NULL,
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `severity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `metadata` json NULL,
  `action_taken` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_severity`(`severity` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_room`(`room_id` ASC) USING BTREE,
  CONSTRAINT `risk_events_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `auction_rooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of risk_events
-- ----------------------------

-- ----------------------------
-- Table structure for risk_events_backup
-- ----------------------------
DROP TABLE IF EXISTS `risk_events_backup`;
CREATE TABLE `risk_events_backup`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `auction_id` bigint NOT NULL,
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `severity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `metadata` json NULL,
  `action_taken` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_severity`(`severity` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '风控事件记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of risk_events_backup
-- ----------------------------
INSERT INTO `risk_events_backup` VALUES (1, 2, 1, 'RAPID_BIDDING', 'LOW', 'Rapid bids detected', '{\"count\": 3, \"interval\": 120}', 'MONITOR', '2026-05-23 00:15:00');
INSERT INTO `risk_events_backup` VALUES (2, 5, 3, 'AUTO_BID_DETECTED', 'INFO', 'Auto bid detected', '{\"strategy\": \"SMART\"}', 'ALLOW', '2026-05-23 00:30:00');

-- ----------------------------
-- Table structure for user_behaviors
-- ----------------------------
DROP TABLE IF EXISTS `user_behaviors`;
CREATE TABLE `user_behaviors`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `item_id` bigint NULL DEFAULT NULL,
  `bid_count` int NULL DEFAULT NULL,
  `avg_bid_interval` int NULL DEFAULT NULL,
  `last_bid_time` datetime NULL DEFAULT NULL,
  `risk_score` decimal(3, 2) NULL DEFAULT NULL,
  `risk_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `is_blocked` tinyint(1) NULL DEFAULT 0,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_risk`(`risk_score` ASC, `is_blocked` ASC) USING BTREE,
  INDEX `idx_user_room`(`user_id` ASC, `room_id` ASC) USING BTREE,
  INDEX `room_id`(`room_id` ASC) USING BTREE,
  CONSTRAINT `user_behaviors_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `auction_rooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behaviors
-- ----------------------------

-- ----------------------------
-- Table structure for user_behaviors_backup
-- ----------------------------
DROP TABLE IF EXISTS `user_behaviors_backup`;
CREATE TABLE `user_behaviors_backup`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `auction_id` bigint NOT NULL,
  `bid_count` int NULL DEFAULT NULL COMMENT '出价次数',
  `avg_bid_interval` int NULL DEFAULT NULL COMMENT '平均出价间隔(秒)',
  `last_bid_time` datetime NULL DEFAULT NULL COMMENT '最后出价时间',
  `risk_score` decimal(3, 2) NULL DEFAULT NULL COMMENT '风险评分 0-1',
  `risk_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '风险等级',
  `is_blocked` tinyint(1) NULL DEFAULT 0,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_risk`(`risk_score` ASC, `is_blocked` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_behaviors_backup
-- ----------------------------
INSERT INTO `user_behaviors_backup` VALUES (1, 1, 1, 2, 300, '2026-05-23 00:15:00', 0.05, 'LOW', 0, '2026-05-23 08:51:23');
INSERT INTO `user_behaviors_backup` VALUES (2, 2, 1, 3, 250, '2026-05-23 00:15:00', 0.08, 'LOW', 0, '2026-05-23 08:51:23');
INSERT INTO `user_behaviors_backup` VALUES (3, 3, 2, 2, 180, '2026-05-23 00:28:00', 0.03, 'LOW', 0, '2026-05-23 08:51:23');
INSERT INTO `user_behaviors_backup` VALUES (4, 4, 1, 2, 360, '2026-05-23 00:18:00', 0.06, 'LOW', 0, '2026-05-23 08:51:23');
INSERT INTO `user_behaviors_backup` VALUES (5, 5, 3, 3, 160, '2026-05-23 00:33:00', 0.10, 'LOW', 0, '2026-05-23 08:51:23');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `total_bids` int NULL DEFAULT 0,
  `total_wins` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'zhang_san', 'Zhang San', 'https://i.pravatar.cc/150?img=1', 17, 2, '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `users` VALUES (2, 'li_si', 'Li Si', 'https://i.pravatar.cc/150?img=2', 23, 1, '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `users` VALUES (3, 'wang_wu', 'Wang Wu', 'https://i.pravatar.cc/150?img=3', 8, 0, '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `users` VALUES (4, 'zhao_liu', 'Zhao Liu', 'https://i.pravatar.cc/150?img=4', 31, 5, '2026-05-23 08:51:22', '2026-05-23 08:51:22');
INSERT INTO `users` VALUES (5, 'collector_chen', 'Chen Collector', 'https://i.pravatar.cc/150?img=5', 67, 12, '2026-05-23 08:51:22', '2026-05-23 08:51:22');

SET FOREIGN_KEY_CHECKS = 1;
