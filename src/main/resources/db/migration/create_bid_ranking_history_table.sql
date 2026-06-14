-- 创建竞拍排行榜历史记录表
CREATE TABLE IF NOT EXISTS bid_ranking_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    auction_item_id BIGINT NOT NULL COMMENT '拍品ID',
    auction_id BIGINT NOT NULL COMMENT '拍卖活动ID',
    auction_item_title VARCHAR(255) COMMENT '拍品标题（冗余字段）',
    ranking_position INT NOT NULL COMMENT '排名',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    masked_username VARCHAR(50) COMMENT '脱敏用户名',
    highest_bid_amount DECIMAL(10,2) NOT NULL COMMENT '最高出价金额',
    bid_count INT NOT NULL DEFAULT 0 COMMENT '出价次数',
    last_bid_time DATETIME COMMENT '最后出价时间',
    is_highest_bidder BOOLEAN DEFAULT FALSE COMMENT '是否为当前最高出价者',
    current_price DECIMAL(10,2) COMMENT '当前拍品价格',
    total_participants INT COMMENT '总参与人数',
    item_status VARCHAR(20) COMMENT '拍品状态',
    snapshot_time DATETIME NOT NULL COMMENT '快照时间戳',
    snapshot_type VARCHAR(20) NOT NULL COMMENT '快照类型（HOURLY, DAILY, EVENT, FINAL）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_auction_item (auction_item_id),
    INDEX idx_auction_id (auction_id),
    INDEX idx_user_id (user_id),
    INDEX idx_snapshot_time (snapshot_time),
    INDEX idx_snapshot_type (snapshot_type),
    INDEX idx_auction_snapshot (auction_item_id, snapshot_time),
    UNIQUE KEY uk_user_snapshot (user_id, auction_item_id, snapshot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞拍排行榜历史记录表';

-- 创建索引优化查询性能
CREATE INDEX idx_ranking_analysis ON bid_ranking_history(auction_item_id, snapshot_time, total_participants);
CREATE INDEX idx_user_performance ON bid_ranking_history(user_id, ranking_position, snapshot_time);
