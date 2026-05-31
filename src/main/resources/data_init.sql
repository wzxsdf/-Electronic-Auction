-- ====================================================
-- 电子拍卖系统初始化数据脚本
-- 生成时间: 2026-05-30
-- 说明: 包含系统初始化所需的基础数据
-- ====================================================

SET NAMES utf8mb4;

-- ====================================================
-- 1. 用户数据 (密码均为: 123456)
-- ====================================================

INSERT INTO `users` (`id`, `username`, `password`, `nickname`, `avatar_url`, `email`, `phone`, `status`, `total_bids`, `total_wins`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'https://i.pravatar.cc/150?img=1', 'admin@auction.com', '13800138001', 'ACTIVE', 0, 0),
(2, 'zhang_san', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张三', 'https://i.pravatar.cc/150?img=2', 'zhangsan@example.com', '13800138002', 'ACTIVE', 17, 2),
(3, 'li_si', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李四', 'https://i.pravatar.cc/150?img=3', 'lisi@example.com', '13800138003', 'ACTIVE', 23, 1),
(4, 'wang_wu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王五', 'https://i.pravatar.cc/150?img=4', 'wangwu@example.com', '13800138004', 'ACTIVE', 8, 0),
(5, 'zhao_liu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '赵六', 'https://i.pravatar.cc/150?img=5', 'zhaoliu@example.com', '13800138005', 'ACTIVE', 31, 5),
(6, 'collector_chen', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '陈收藏家', 'https://i.pravatar.cc/150?img=6', 'chen@example.com', '13800138006', 'ACTIVE', 67, 12);

-- ====================================================
-- 2. 角色数据
-- ====================================================

INSERT INTO `roles` (`id`, `code`, `name`, `description`) VALUES
(1, 'ADMIN', '管理员', '系统管理员，拥有所有权限'),
(2, 'USER', '普通用户', '普通用户，可参与竞拍');

-- ====================================================
-- 3. 权限数据
-- ====================================================

INSERT INTO `permissions` (`id`, `code`, `name`, `resource`, `action`, `description`) VALUES
-- 用户管理权限
(1, 'user:read', '查看用户', '/api/users/**', 'READ', '查看用户信息'),
(2, 'user:write', '管理用户', '/api/users/**', 'WRITE', '创建、更新用户'),
(3, 'user:delete', '删除用户', '/api/users/**', 'DELETE', '删除用户'),

-- 竞拍管理权限
(4, 'auction:read', '查看竞拍', '/api/auctions/**', 'READ', '查看竞拍信息'),
(5, 'auction:write', '管理竞拍', '/api/auctions/**', 'WRITE', '创建、更新竞拍'),
(6, 'auction:delete', '删除竞拍', '/api/auctions/**', 'DELETE', '删除竞拍'),

-- 出价权限
(7, 'bid:create', '参与出价', '/api/bids/**', 'WRITE', '参与竞拍出价'),
(8, 'bid:read', '查看出价', '/api/bids/**', 'READ', '查看出价记录'),

-- 订单管理权限
(9, 'order:read', '查看订单', '/api/orders/**', 'READ', '查看订单信息'),
(10, 'order:write', '管理订单', '/api/orders/**', 'WRITE', '处理订单'),
(11, 'order:delete', '删除订单', '/api/orders/**', 'DELETE', '删除订单'),

-- 风控权限
(12, 'risk:read', '查看风控', '/api/risk/**', 'READ', '查看风控信息'),
(13, 'risk:manage', '管理风控', '/api/risk/**', 'WRITE', '管理风控规则'),

-- 系统管理权限
(14, 'system:config', '系统配置', '/api/system/**', 'WRITE', '修改系统配置'),
(15, 'log:read', '查看日志', '/api/logs/**', 'READ', '查看系统日志');

-- ====================================================
-- 4. 用户角色关联
-- ====================================================

INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(1, 1),  -- admin -> ADMIN
(2, 2),  -- zhang_san -> USER
(3, 2),  -- li_si -> USER
(4, 2),  -- wang_wu -> USER
(5, 2),  -- zhao_liu -> USER
(6, 2);  -- collector_chen -> USER

-- ====================================================
-- 5. 角色权限关联
-- ====================================================

-- 管理员拥有所有权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3),  -- 用户管理
(1, 4), (1, 5), (1, 6),  -- 竞拍管理
(1, 7), (1, 8),          -- 出价
(1, 9), (1, 10), (1, 11), -- 订单管理
(1, 12), (1, 13),        -- 风控
(1, 14), (1, 15);        -- 系统管理

-- 普通用户权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) VALUES
(2, 4),  -- 查看竞拍
(2, 7),  -- 参与出价
(2, 8),  -- 查看出价记录
(2, 9);  -- 查看订单

-- ====================================================
-- 6. 商品数据
-- ====================================================

INSERT INTO `products` (`id`, `name`, `image_url`, `description`, `category`) VALUES
(1, '缅甸翡翠手镯', 'https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?w=400', '天然翡翠A货，种质通透，色泽饱满', '珠宝'),
(2, '999足金项链', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=400', '999足金项链，重量约30克，工艺精湛', '珠宝'),
(3, '明代青花瓷瓶', 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=400', '明代青花瓷瓶，保存完好，极具收藏价值', '陶瓷'),
(4, '紫砂壶', 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400', '大师手工紫砂壶，泥料上乘，造型精美', '茶具'),
(5, '瑞士机械手表', 'https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=400', '瑞士制造机械手表，自动上链，精密可靠', '手表'),
(6, '和田玉吊坠', 'https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=400', '新疆和田玉吊坠，温润如脂，寓意吉祥', '珠宝');

-- ====================================================
-- 7. 竞拍房间数据
-- ====================================================

INSERT INTO `auction_rooms` (`id`, `title`, `description`, `host_id`, `status`, `viewer_count`, `start_time`, `end_time`) VALUES
(1, '高端艺术品拍卖会', '汇集珍贵艺术品与古董珍玩，为藏家提供优质竞拍平台', 1, 'LIVE', 156, '2026-05-30 10:00:00', '2026-05-30 18:00:00');

-- ====================================================
-- 8. 竞拍项数据
-- ====================================================

INSERT INTO `auction_items` (`id`, `version`, `room_id`, `product_id`, `title`, `start_price`, `bid_increment`, `max_price`, `delay_seconds`, `start_time`, `end_time`, `original_end_time`, `current_price`, `highest_bidder`, `status`, `bid_count`, `display_order`) VALUES
(1, 0, 1, 1, '缅甸翡翠手镯拍卖', 5000.00, 200.00, 50000.00, 15, '2026-05-30 10:30:00', '2026-05-30 11:30:00', '2026-05-30 11:30:00', 6200.00, 3, 'ACTIVE', 8, 1),
(2, 0, 1, 2, '999足金项链拍卖', 2000.00, 100.00, 30000.00, 15, '2026-05-30 10:45:00', '2026-05-30 11:45:00', '2026-05-30 11:45:00', 2500.00, 4, 'ACTIVE', 6, 2),
(3, 0, 1, 3, '明代青花瓷瓶拍卖', 10000.00, 500.00, 150000.00, 20, '2026-05-30 11:00:00', '2026-05-30 12:00:00', '2026-05-30 12:00:00', 11500.00, 5, 'ACTIVE', 4, 3),
(4, 0, 1, 4, '紫砂壶拍卖', 3000.00, 150.00, 40000.00, 15, '2026-05-30 13:00:00', '2026-05-30 14:00:00', '2026-05-30 14:00:00', 3000.00, NULL, 'PENDING', 0, 4),
(5, 0, 1, 5, '瑞士机械手表拍卖', 8000.00, 300.00, 80000.00, 20, '2026-05-30 14:00:00', '2026-05-30 15:00:00', '2026-05-30 15:00:00', 8000.00, NULL, 'PENDING', 0, 5);

-- ====================================================
-- 9. 出价记录数据
-- ====================================================

INSERT INTO `bids` (`id`, `item_id`, `auction_id`, `user_id`, `amount`, `rank_when_bid`, `status`, `is_auto_bid`, `created_at`) VALUES
-- 翡翠手镯出价记录
(1, 1, 1, 2, 5000.00, 1, 'ACTIVE', 0, '2026-05-30 10:32:00'),
(2, 1, 1, 3, 5200.00, 1, 'ACTIVE', 0, '2026-05-30 10:35:00'),
(3, 1, 1, 4, 5400.00, 2, 'ACTIVE', 0, '2026-05-30 10:38:00'),
(4, 1, 1, 3, 5600.00, 1, 'ACTIVE', 0, '2026-05-30 10:42:00'),
(5, 1, 1, 5, 5800.00, 2, 'ACTIVE', 0, '2026-05-30 10:45:00'),
(6, 1, 1, 3, 6000.00, 1, 'ACTIVE', 0, '2026-05-30 10:48:00'),
(7, 1, 1, 2, 6200.00, 1, 'ACTIVE', 1, '2026-05-30 10:52:00'),

-- 足金项链出价记录
(8, 2, 2, 3, 2000.00, 1, 'ACTIVE', 0, '2026-05-30 10:47:00'),
(9, 2, 2, 2, 2100.00, 2, 'ACTIVE', 0, '2026-05-30 10:50:00'),
(10, 2, 2, 4, 2300.00, 1, 'ACTIVE', 0, '2026-05-30 10:53:00'),
(11, 2, 2, 5, 2400.00, 2, 'ACTIVE', 0, '2026-05-30 10:55:00'),
(12, 2, 2, 4, 2500.00, 1, 'ACTIVE', 1, '2026-05-30 10:58:00'),

-- 青花瓷瓶出价记录
(13, 3, 3, 2, 10000.00, 1, 'ACTIVE', 0, '2026-05-30 11:05:00'),
(14, 3, 3, 3, 10500.00, 2, 'ACTIVE', 0, '2026-05-30 11:08:00'),
(15, 3, 3, 5, 11000.00, 1, 'ACTIVE', 0, '2026-05-30 11:12:00'),
(16, 3, 3, 6, 11500.00, 1, 'ACTIVE', 1, '2026-05-30 11:15:00');

-- ====================================================
-- 10. 代理出价配置数据
-- ====================================================

INSERT INTO `auto_bid_configs` (`id`, `user_id`, `item_id`, `auction_id`, `max_price`, `strategy`, `status`, `bid_count`, `current_bid`) VALUES
(1, 2, 1, 1, 8000.00, 'SMART', 'ACTIVE', 1, 6200.00),
(2, 4, 2, 2, 5000.00, 'LAST_SEC', 'ACTIVE', 1, 2500.00),
(3, 6, 3, 3, 20000.00, 'SMART', 'ACTIVE', 1, 11500.00);

-- ====================================================
-- 11. 用户行为数据
-- ====================================================

INSERT INTO `user_behaviors` (`id`, `user_id`, `room_id`, `item_id`, `bid_count`, `avg_bid_interval`, `last_bid_time`, `risk_score`, `risk_level`, `is_blocked`) VALUES
(1, 2, 1, 1, 3, 240, '2026-05-30 10:52:00', 0.05, 'LOW', 0),
(2, 3, 1, 1, 3, 300, '2026-05-30 10:48:00', 0.04, 'LOW', 0),
(3, 4, 1, 2, 2, 180, '2026-05-30 10:58:00', 0.03, 'LOW', 0),
(4, 5, 1, 1, 1, 0, '2026-05-30 10:45:00', 0.02, 'LOW', 0),
(5, 6, 1, 3, 1, 0, '2026-05-30 11:15:00', 0.02, 'LOW', 0);

-- ====================================================
-- 12. 风控事件数据
-- ====================================================

INSERT INTO `risk_events` (`id`, `user_id`, `room_id`, `item_id`, `event_type`, `severity`, `description`, `metadata`, `action_taken`) VALUES
(1, 2, 1, 1, 'AUTO_BID_DETECTED', 'INFO', '检测到代理出价', '{"strategy": "SMART"}', 'ALLOW'),
(2, 4, 1, 2, 'AUTO_BID_DETECTED', 'INFO', '检测到代理出价', '{"strategy": "LAST_SEC"}', 'ALLOW'),
(3, 6, 1, 3, 'AUTO_BID_DETECTED', 'INFO', '检测到代理出价', '{"strategy": "SMART"}', 'ALLOW');

-- ====================================================
-- 13. 登录日志示例数据
-- ====================================================

INSERT INTO `login_logs` (`id`, `user_id`, `username`, `login_type`, `ip_address`, `user_agent`, `status`, `failure_reason`) VALUES
(1, 1, 'admin', 'PASSWORD', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'SUCCESS', NULL),
(2, 2, 'zhang_san', 'PASSWORD', '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'SUCCESS', NULL),
(3, 3, 'li_si', 'PASSWORD', '192.168.1.101', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 'SUCCESS', NULL),
(4, 2, 'zhang_san', 'PASSWORD', '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'FAILED', '密码错误'),
(5, 2, 'zhang_san', 'PASSWORD', '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'SUCCESS', NULL);

-- ====================================================
-- 数据初始化完成
-- ====================================================
