-- 创建拍卖活动关注表
-- 支持用户关注待开始的拍卖活动，活动开始时向关注者推送通知

CREATE TABLE IF NOT EXISTS auction_follows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注记录ID',
    auction_id BIGINT NOT NULL COMMENT '拍卖活动ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '关注状态：ACTIVE-有效, CANCELLED-已取消',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    UNIQUE KEY uk_auction_user (auction_id, user_id) COMMENT '同一用户对同一活动只能关注一次',
    KEY idx_auction_id (auction_id) COMMENT '按活动查询关注者',
    KEY idx_user_id (user_id) COMMENT '按用户查询关注的活动',
    KEY idx_status (status) COMMENT '按状态筛选',

    -- 外键约束
    CONSTRAINT fk_auction_follows_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    CONSTRAINT fk_auction_follows_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖活动关注表';