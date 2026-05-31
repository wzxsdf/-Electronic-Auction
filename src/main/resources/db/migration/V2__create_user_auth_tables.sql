-- ============================================================================
-- 用户认证和权限系统数据库迁移脚本
-- 版本: V2
-- 描述: 创建用户、角色、权限及相关表
-- ============================================================================

-- 1. 扩展用户表（保留现有字段，添加认证和状态字段）
ALTER TABLE `users`
ADD COLUMN IF NOT EXISTS `password` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '密码（BCrypt加密）' AFTER `username`,
ADD COLUMN IF NOT EXISTS `email` VARCHAR(100) UNIQUE COMMENT '邮箱' AFTER `avatar_url`,
ADD COLUMN IF NOT EXISTS `phone` VARCHAR(20) UNIQUE COMMENT '手机号' AFTER `email`,
ADD COLUMN IF NOT EXISTS `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态' AFTER `phone`,
ADD COLUMN IF NOT EXISTS `last_login_at` DATETIME COMMENT '最后登录时间' AFTER `status`,
ADD COLUMN IF NOT EXISTS `last_login_ip` VARCHAR(50) COMMENT '最后登录IP' AFTER `last_login_at`,
ADD COLUMN IF NOT EXISTS `version` INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_at`,
ADD INDEX IF NOT EXISTS `idx_email` (`email`),
ADD INDEX IF NOT EXISTS `idx_phone` (`phone`),
ADD INDEX IF NOT EXISTS `idx_status` (`status`);

-- 2. 创建角色表
CREATE TABLE IF NOT EXISTS `roles` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code`            VARCHAR(50) UNIQUE NOT NULL COMMENT '角色编码',
    `name`            VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description`     VARCHAR(500) COMMENT '角色描述',
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 创建权限表
CREATE TABLE IF NOT EXISTS `permissions` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code`            VARCHAR(100) UNIQUE NOT NULL COMMENT '权限编码',
    `name`            VARCHAR(100) NOT NULL COMMENT '权限名称',
    `resource`        VARCHAR(200) COMMENT '资源路径',
    `action`          VARCHAR(50) COMMENT '操作类型',
    `description`     VARCHAR(500) COMMENT '权限描述',
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_code` (`code`),
    INDEX `idx_resource` (`resource`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 4. 创建用户-角色关联表
CREATE TABLE IF NOT EXISTS `user_roles` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `role_id`         BIGINT NOT NULL COMMENT '角色ID',
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_role_id` (`role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 5. 创建角色-权限关联表
CREATE TABLE IF NOT EXISTS `role_permissions` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role_id`         BIGINT NOT NULL COMMENT '角色ID',
    `permission_id`   BIGINT NOT NULL COMMENT '权限ID',
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_permission_id` (`permission_id`),
    FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`permission_id`) REFERENCES `permissions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 6. 创建登录日志表
CREATE TABLE IF NOT EXISTS `login_logs` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT COMMENT '用户ID',
    `username`        VARCHAR(50) COMMENT '用户名',
    `login_type`      VARCHAR(20) NOT NULL COMMENT '登录类型',
    `ip_address`      VARCHAR(50) COMMENT 'IP地址',
    `user_agent`      VARCHAR(500) COMMENT '浏览器UA',
    `status`          VARCHAR(20) NOT NULL COMMENT '登录状态',
    `failure_reason` VARCHAR(500) COMMENT '失败原因',
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- 7. 创建操作日志表
CREATE TABLE IF NOT EXISTS `operation_logs` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT COMMENT '操作用户ID',
    `username`        VARCHAR(50) COMMENT '操作用户名',
    `module`          VARCHAR(50) NOT NULL COMMENT '操作模块',
    `operation`       VARCHAR(100) NOT NULL COMMENT '操作类型',
    `method`          VARCHAR(200) COMMENT '请求方法',
    `params`          TEXT COMMENT '请求参数',
    `ip_address`      VARCHAR(50) COMMENT 'IP地址',
    `status`          VARCHAR(20) NOT NULL COMMENT '操作状态',
    `error_msg`       VARCHAR(500) COMMENT '错误信息',
    `duration`        INT COMMENT '执行耗时(ms)',
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_module` (`module`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 8. 初始化角色数据
INSERT INTO `roles` (`code`, `name`, `description`) VALUES
('ADMIN', '管理员', '系统管理员，拥有所有权限'),
('MERCHANT', '商家', '商家用户，可以创建和管理拍卖'),
('STREAMER', '主播', '主播用户，可以主持拍卖活动'),
('USER', '普通用户', '普通用户，可以参与拍卖')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 9. 初始化权限数据
INSERT INTO `permissions` (`code`, `name`, `resource`, `action`, `description`) VALUES
-- 用户权限
('user:read', '查看用户', '/api/users/*', 'GET', '查看用户信息'),
('user:update', '更新用户信息', '/api/users/*', 'PUT', '更新用户信息'),
('user:delete', '删除用户', '/api/users/*', 'DELETE', '删除用户'),

-- 拍卖权限
('auction:create', '创建拍卖', '/api/auctions', 'POST', '创建拍卖活动'),
('auction:manage', '管理拍卖', '/api/auctions/*', '*', '管理拍卖活动'),
('auction:bid', '参与拍卖', '/api/bids', 'POST', '参与拍卖出价'),

-- 管理权限
('admin:user:manage', '管理用户', '/api/admin/users/*', '*', '管理系统用户'),
('admin:system:config', '系统配置', '/api/admin/config/*', '*', '系统配置管理'),
('admin:data:export', '数据导出', '/api/admin/export/*', 'GET', '导出系统数据')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 10. 初始化角色-权限关联
-- 管理员拥有所有权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT
    (SELECT id FROM `roles` WHERE `code` = 'ADMIN' LIMIT 1),
    id
FROM `permissions`
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- 商家拥有拍卖权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT
    (SELECT id FROM `roles` WHERE `code` = 'MERCHANT' LIMIT 1),
    id
FROM `permissions`
WHERE `code` IN ('auction:create', 'auction:manage', 'user:update')
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- 主播拥有拍卖权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT
    (SELECT id FROM `roles` WHERE `code` = 'STREAMER' LIMIT 1),
    id
FROM `permissions`
WHERE `code` IN ('auction:bid', 'user:update')
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- 普通用户拥有基础权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT
    (SELECT id FROM `roles` WHERE `code` = 'USER' LIMIT 1),
    id
FROM `permissions`
WHERE `code` IN ('user:read', 'user:update', 'auction:bid')
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;
