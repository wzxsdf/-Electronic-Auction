USE auction;

-- Create auction_rooms table
CREATE TABLE IF NOT EXISTS auction_rooms (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    host_id         BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    viewer_count    INT DEFAULT 0,
    start_time      DATETIME NOT NULL,
    end_time        DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rename old tables
RENAME TABLE auctions TO auctions_backup;
RENAME TABLE bids TO bids_backup;
RENAME TABLE orders TO orders_backup;
RENAME TABLE auto_bid_configs TO auto_bid_configs_backup;
RENAME TABLE user_behaviors TO user_behaviors_backup;
RENAME TABLE risk_events TO risk_events_backup;

-- Create auction_items table
CREATE TABLE IF NOT EXISTS auction_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id         BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    start_price     DECIMAL(10,2) NOT NULL DEFAULT 0,
    bid_increment   DECIMAL(10,2) NOT NULL,
    max_price       DECIMAL(10,2),
    delay_seconds   INT NOT NULL DEFAULT 15,
    start_time      DATETIME NOT NULL,
    end_time        DATETIME NOT NULL,
    original_end_time DATETIME NOT NULL,
    current_price   DECIMAL(10,2) DEFAULT 0,
    highest_bidder  BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    bid_count       INT DEFAULT 0,
    display_order   INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room (room_id),
    INDEX idx_status (status),
    INDEX idx_time (start_time, end_time),
    FOREIGN KEY (room_id) REFERENCES auction_rooms(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create bids table
CREATE TABLE IF NOT EXISTS bids (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id         BIGINT NOT NULL,
    auction_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    rank_when_bid   INT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_auto_bid     BOOLEAN DEFAULT FALSE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_item (item_id),
    INDEX idx_auction (auction_id),
    INDEX idx_user (user_id),
    INDEX idx_item_amount (item_id, amount DESC),
    INDEX idx_created (created_at),
    FOREIGN KEY (item_id) REFERENCES auction_items(id),
    FOREIGN KEY (auction_id) REFERENCES auction_rooms(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id         BIGINT NOT NULL,
    item_id         BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    final_amount    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_room (room_id),
    INDEX idx_item (item_id),
    FOREIGN KEY (room_id) REFERENCES auction_rooms(id),
    FOREIGN KEY (item_id) REFERENCES auction_items(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create auto_bid_configs table
CREATE TABLE IF NOT EXISTS auto_bid_configs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    item_id         BIGINT NOT NULL,
    auction_id      BIGINT NOT NULL,
    max_price       DECIMAL(10,2) NOT NULL,
    strategy        VARCHAR(20) NOT NULL,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    bid_count       INT DEFAULT 0,
    current_bid     DECIMAL(10,2),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_item (user_id, item_id),
    INDEX idx_item (item_id),
    FOREIGN KEY (item_id) REFERENCES auction_items(id),
    FOREIGN KEY (auction_id) REFERENCES auction_rooms(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create user_behaviors table
CREATE TABLE IF NOT EXISTS user_behaviors (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    room_id         BIGINT NOT NULL,
    item_id         BIGINT NULL,
    bid_count       INT,
    avg_bid_interval INT,
    last_bid_time   DATETIME,
    risk_score      DECIMAL(3,2),
    risk_level      VARCHAR(20),
    is_blocked      BOOLEAN DEFAULT FALSE,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_risk (risk_score, is_blocked),
    INDEX idx_user_room (user_id, room_id),
    FOREIGN KEY (room_id) REFERENCES auction_rooms(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create risk_events table
CREATE TABLE IF NOT EXISTS risk_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    room_id         BIGINT NOT NULL,
    item_id         BIGINT NULL,
    event_type      VARCHAR(50) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    description     TEXT,
    metadata        JSON,
    action_taken    VARCHAR(50),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_severity (severity),
    INDEX idx_user (user_id),
    INDEX idx_room (room_id),
    FOREIGN KEY (room_id) REFERENCES auction_rooms(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default auction room
INSERT INTO auction_rooms (id, title, status, start_time, end_time) VALUES
(1, 'Premium Auction Room', 'LIVE', NOW(), DATE_ADD(NOW(), INTERVAL 4 HOUR));

-- Migrate auction_items data
INSERT INTO auction_items (
    id, room_id, product_id, title, start_price, bid_increment, max_price,
    delay_seconds, start_time, end_time, original_end_time, current_price,
    highest_bidder, status, bid_count, display_order
)
SELECT
    id,
    1 as room_id,
    product_id,
    title,
    start_price,
    bid_increment,
    max_price,
    delay_seconds,
    start_time,
    end_time,
    original_end_time,
    current_price,
    highest_bidder,
    status,
    0 as bid_count,
    id as display_order
FROM auctions_backup;

-- Migrate bids data
INSERT INTO bids (id, item_id, auction_id, user_id, amount, rank_when_bid, status, is_auto_bid, created_at)
SELECT
    b.id,
    b.auction_id as item_id,
    1 as auction_id,
    b.user_id,
    b.amount,
    b.rank_when_bid,
    b.status,
    b.is_auto_bid,
    b.created_at
FROM bids_backup b;

-- Verify migration
SELECT 'Migration completed' AS status;
SELECT CONCAT('Rooms: ', COUNT(*)) FROM auction_rooms
UNION ALL
SELECT CONCAT('Items: ', COUNT(*)) FROM auction_items
UNION ALL
SELECT CONCAT('Bids: ', COUNT(*)) FROM bids;
