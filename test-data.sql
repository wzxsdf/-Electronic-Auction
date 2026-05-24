USE auction;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE risk_events;
TRUNCATE TABLE user_behaviors;
TRUNCATE TABLE auto_bid_configs;
TRUNCATE TABLE orders;
TRUNCATE TABLE bids;
TRUNCATE TABLE auctions;
TRUNCATE TABLE products;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (id, username, nickname, avatar_url, total_bids, total_wins) VALUES
(1, 'zhang_san', 'Zhang San', 'https://i.pravatar.cc/150?img=1', 15, 2),
(2, 'li_si', 'Li Si', 'https://i.pravatar.cc/150?img=2', 23, 1),
(3, 'wang_wu', 'Wang Wu', 'https://i.pravatar.cc/150?img=3', 8, 0),
(4, 'zhao_liu', 'Zhao Liu', 'https://i.pravatar.cc/150?img=4', 31, 5),
(5, 'collector_chen', 'Chen Collector', 'https://i.pravatar.cc/150?img=5', 67, 12);

INSERT INTO products (id, name, image_url, description, category) VALUES
(1, 'Jade Bangle', 'https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?w=400', 'Natural jadeite bangle from Burma', 'Jewelry'),
(2, 'Gold Necklace', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=400', '999 gold necklace 30g', 'Jewelry'),
(3, 'Blue White Porcelain', 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=400', 'Ming Dynasty blue and white vase', 'Ceramics'),
(4, 'Purple Clay Teapot', 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400', 'Master crafted purple clay teapot', 'Tea'),
(5, 'Swiss Watch', 'https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=400', 'Swiss mechanical watch', 'Watch'),
(6, 'Hetian Jade Pendant', 'https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=400', 'Hetian jade pendant with blessing design', 'Jewelry');

INSERT INTO auctions (id, product_id, title, start_price, bid_increment, max_price, delay_seconds,
                      start_time, end_time, original_end_time, current_price, highest_bidder, status) VALUES
(1, 1, 'Jade Bangle Auction', 5000.00, 200.00, 50000.00, 15,
 DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 30 MINUTE),
 6200.00, 4, 'ACTIVE'),
(2, 2, 'Gold Necklace Auction', 2000.00, 100.00, 30000.00, 15,
 DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_ADD(NOW(), INTERVAL 45 MINUTE), DATE_ADD(NOW(), INTERVAL 45 MINUTE),
 3500.00, 2, 'ACTIVE'),
(3, 3, 'Ming Porcelain Auction', 10000.00, 500.00, 150000.00, 20,
 DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 50 MINUTE), DATE_ADD(NOW(), INTERVAL 50 MINUTE),
 12500.00, 5, 'ACTIVE'),
(4, 4, 'Teapot Auction', 3000.00, 150.00, 40000.00, 15,
 DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR),
 3000.00, NULL, 'PENDING'),
(5, 5, 'Swiss Watch Auction', 8000.00, 300.00, 80000.00, 20,
 DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR),
 8000.00, NULL, 'PENDING'),
(6, 6, 'Jade Pendant Auction', 1500.00, 100.00, 20000.00, 10,
 DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR),
 4800.00, 5, 'COMPLETED');

INSERT INTO bids (id, auction_id, user_id, amount, rank_when_bid, status, is_auto_bid, created_at) VALUES
(1, 1, 1, 5000.00, 1, 'ACTIVE', 0, '2026-05-23 00:00:00'),
(2, 1, 2, 5200.00, 1, 'ACTIVE', 0, '2026-05-23 00:05:00'),
(3, 1, 3, 5400.00, 2, 'ACTIVE', 0, '2026-05-23 00:08:00'),
(4, 1, 4, 5800.00, 1, 'ACTIVE', 0, '2026-05-23 00:12:00'),
(5, 1, 2, 6000.00, 2, 'ACTIVE', 0, '2026-05-23 00:15:00'),
(6, 1, 4, 6200.00, 1, 'ACTIVE', 0, '2026-05-23 00:18:00'),
(7, 2, 3, 2000.00, 1, 'ACTIVE', 0, '2026-05-23 00:20:00'),
(8, 2, 1, 2100.00, 2, 'ACTIVE', 0, '2026-05-23 00:22:00'),
(9, 2, 2, 2500.00, 1, 'ACTIVE', 0, '2026-05-23 00:25:00'),
(10, 2, 4, 3000.00, 2, 'ACTIVE', 0, '2026-05-23 00:28:00'),
(11, 2, 2, 3500.00, 1, 'ACTIVE', 0, '2026-05-23 00:30:00'),
(12, 3, 5, 10000.00, 1, 'ACTIVE', 0, '2026-05-23 00:25:00'),
(13, 3, 4, 10500.00, 2, 'ACTIVE', 0, '2026-05-23 00:28:00'),
(14, 3, 5, 11000.00, 1, 'ACTIVE', 1, '2026-05-23 00:30:00'),
(15, 3, 4, 12000.00, 2, 'ACTIVE', 0, '2026-05-23 00:32:00'),
(16, 3, 5, 12500.00, 1, 'ACTIVE', 1, '2026-05-23 00:33:00'),
(17, 6, 1, 1500.00, 1, 'ACTIVE', 0, '2026-05-22 18:00:00'),
(18, 6, 2, 1800.00, 1, 'ACTIVE', 0, '2026-05-22 18:15:00'),
(19, 6, 3, 2200.00, 2, 'ACTIVE', 0, '2026-05-22 18:30:00'),
(20, 6, 4, 3500.00, 1, 'ACTIVE', 0, '2026-05-22 19:00:00'),
(21, 6, 5, 4800.00, 1, 'WINNER', 0, '2026-05-22 19:45:00');

INSERT INTO orders (id, auction_id, user_id, product_id, final_amount, status) VALUES
(1, 6, 5, 6, 4800.00, 'PENDING');

INSERT INTO auto_bid_configs (id, user_id, auction_id, max_price, strategy, status, bid_count, current_bid) VALUES
(1, 4, 1, 8000.00, 'SMART', 'ACTIVE', 2, 6200.00),
(2, 5, 2, 5000.00, 'AGGRESSIVE', 'ACTIVE', 0, NULL),
(3, 5, 3, 20000.00, 'SMART', 'ACTIVE', 2, 12500.00),
(4, 1, 4, 12000.00, 'LAST_SEC', 'ACTIVE', 0, NULL),
(5, 2, 5, 25000.00, 'SMART', 'ACTIVE', 0, NULL);

INSERT INTO user_behaviors (id, user_id, auction_id, bid_count, avg_bid_interval, last_bid_time, risk_score, risk_level, is_blocked) VALUES
(1, 1, 1, 2, 300, '2026-05-23 00:15:00', 0.05, 'LOW', 0),
(2, 2, 1, 3, 250, '2026-05-23 00:15:00', 0.08, 'LOW', 0),
(3, 3, 2, 2, 180, '2026-05-23 00:28:00', 0.03, 'LOW', 0),
(4, 4, 1, 2, 360, '2026-05-23 00:18:00', 0.06, 'LOW', 0),
(5, 5, 3, 3, 160, '2026-05-23 00:33:00', 0.10, 'LOW', 0);

INSERT INTO risk_events (id, user_id, auction_id, event_type, severity, description, metadata, action_taken, created_at) VALUES
(1, 2, 1, 'RAPID_BIDDING', 'LOW', 'Rapid bids detected', '{\"interval\": 120, \"count\": 3}', 'MONITOR', '2026-05-23 00:15:00'),
(2, 5, 3, 'AUTO_BID_DETECTED', 'INFO', 'Auto bid detected', '{\"strategy\": \"SMART\"}', 'ALLOW', '2026-05-23 00:30:00');

SELECT 'Data insertion completed' AS status;
