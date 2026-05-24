@echo off
REM ============================================
REM 拍卖系统启动脚本
REM ============================================

echo.
echo ============================================
echo   拍卖系统并发安全优化版
echo ============================================
echo.

REM 检查 Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Java，请先安装 JDK 17+
    pause
    exit /b 1
)

REM 检查 Redis
echo [1/3] 检查 Redis 连接...
ping 127.0.0.1 -n 2 >nul
redis-cli -h localhost -p 6379 ping >nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] Redis 未运行，分布式锁功能将不可用
    echo         请启动 Redis: redis-server
    echo.
)

REM 提示执行数据库迁移
echo [2/3] 数据库迁移提示...
echo         如尚未执行迁移，请运行: mysql -u root -p auction ^< migrate.sql
echo.

REM 启动应用
echo [3/3] 启动应用...
echo.
cd /d "%~dp0"
mvn spring-boot:run

pause
