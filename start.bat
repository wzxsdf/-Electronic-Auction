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

REM 停止可能存在的旧实例
echo [3/4] 停止可能存在的旧实例...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
    echo 发现进程 %%a 占用8080端口，正在终止...
    taskkill /F /PID %%a
)

REM 等待端口释放
timeout /t 2 /nobreak >nul

REM 启动应用
echo [4/4] 启动应用...
echo.
cd /d "%~dp0"
mvn spring-boot:run

pause
