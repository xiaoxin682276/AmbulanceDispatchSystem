@echo off
cd /d %~dp0

echo 正在检测 Node.js 是否已安装...
where node >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Node.js，请从 https://nodejs.org 下载并安装。
    pause
    exit /b
)
echo Node.js 已安装。


:: 检查 axios 是否已安装
echo 正在检测 axios 是否已安装...
if not exist node_modules\axios (
    echo 未检测到 axios，正在自动安装 axios...
    npm install axios
    if errorlevel 1 (
        echo [错误] axios 安装失败，请检查网络。
        pause
        exit /b
    )
) else (
    echo axios 已安装。
)

:: 启动前端服务
echo 启动前端开发服务器中...
start powershell -ExecutionPolicy Bypass -Command "npm start"

timeout /t 5 >nul
start http://localhost:3000

pause
