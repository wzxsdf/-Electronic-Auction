#!/bin/bash
# ============================================
# 拍卖系统启动脚本
# ============================================

echo ""
echo "============================================"
echo "  拍卖系统并发安全优化版"
echo "============================================"
echo ""

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "[错误] 未找到 Java，请先安装 JDK 17+"
    exit 1
fi

# 检查 Redis
echo "[1/3] 检查 Redis 连接..."
if ! redis-cli -h localhost -p 6379 ping &> /dev/null; then
    echo "[警告] Redis 未运行，分布式锁功能将不可用"
    echo "        请启动 Redis: redis-server"
    echo ""
fi

# 提示执行数据库迁移
echo "[2/3] 数据库迁移提示..."
echo "        如尚未执行迁移，请运行: mysql -u root -p auction < migrate.sql"
echo ""

# 启动应用
echo "[3/3] 启动应用..."
echo ""
cd "$(dirname "$0")"
mvn spring-boot:run
