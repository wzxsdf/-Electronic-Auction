-- 创建数据库
CREATE DATABASE IF NOT EXISTS auction DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE auction;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50) UNIQUE NOT NULL,
    nickname        VARCHAR(100),
    avatar_url      VARCHAR(500),
    total_bids      INT DEFAULT 0,
    total_wins      INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    image_url       VARCHAR(500),
    description     TEXT,
    category        VARCHAR(50),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 竞拍活动表
CREATE TABLE IF NOT EXISTS auctions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_time (start_time, end_time),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞拍活动表';

-- 出价记录表
CREATE TABLE IF NOT EXISTS bids (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    rank_when_bid   INT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_auto_bid     BOOLEAN DEFAULT FALSE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auction_user (auction_id, user_id),
    INDEX idx_auction_amount (auction_id, amount DESC),
    INDEX idx_created (created_at),
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出价记录表';

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    final_amount    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_auction (auction_id),
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 代理出价配置表
CREATE TABLE IF NOT EXISTS auto_bid_configs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    auction_id      BIGINT NOT NULL,
    max_price       DECIMAL(10,2) NOT NULL,
    strategy        VARCHAR(20) NOT NULL COMMENT 'LAST_SEC/SMART/AGGRESSIVE',
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    bid_count       INT DEFAULT 0,
    current_bid     DECIMAL(10,2),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_auction (user_id, auction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理出价配置表';

-- 用户行为表
CREATE TABLE IF NOT EXISTS user_behaviors (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    auction_id      BIGINT NOT NULL,
    bid_count       INT COMMENT '出价次数',
    avg_bid_interval INT COMMENT '平均出价间隔(秒)',
    last_bid_time   DATETIME COMMENT '最后出价时间',
    risk_score      DECIMAL(3,2) COMMENT '风险评分 0-1',
    risk_level      VARCHAR(20) COMMENT '风险等级',
    is_blocked      BOOLEAN DEFAULT FALSE,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_risk (risk_score, is_blocked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

-- 风控事件表
CREATE TABLE IF NOT EXISTS risk_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    auction_id      BIGINT NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    description     TEXT,
    metadata        JSON,
    action_taken    VARCHAR(50),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_severity (severity),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控事件记录表';

-- 插入测试数据
INSERT INTO users (username, nickname) VALUES
('test_user_1', '测试用户1'),
('test_user_2', '测试用户2');

INSERT INTO products (name, description, category) VALUES
('翡翠手镯', '天然A货翡翠手镯，色泽温润，质地细腻', '珠宝'),
('黄金项链', '999足金项链，精致工艺，时尚百搭', '珠宝');
