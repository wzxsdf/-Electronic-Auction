@echo off
echo 正在停止电子拍卖系统...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
    echo 发现进程 %%a 占用8080端口，正在终止...
    taskkill /F /PID %%a
)
echo 应用已停止。
pause