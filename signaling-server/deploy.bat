@echo off
echo =========================================
echo P2P 远程控制 - 信令服务器部署脚本
echo =========================================
echo.

REM 检查 Docker 是否安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker 未安装，请先安装 Docker Desktop
    pause
    exit /b 1
)

echo ✅ Docker 已安装
echo.

REM 构建镜像
echo 📦 构建 Docker 镜像...
docker build -t p2p-signaling-server:latest .
if errorlevel 1 (
    echo ❌ 镜像构建失败
    pause
    exit /b 1
)

echo ✅ 镜像构建成功
echo.

REM 停止旧容器（如果存在）
echo 🔄 检查旧容器...
docker stop p2p-signaling-server >nul 2>&1
docker rm p2p-signaling-server >nul 2>&1

REM 运行容器
echo 🚀 启动容器...
docker run -d ^
    --name p2p-signaling-server ^
    -p 3000:3000 ^
    --restart unless-stopped ^
    p2p-signaling-server:latest

if errorlevel 1 (
    echo ❌ 容器启动失败
    pause
    exit /b 1
)

echo ✅ 容器启动成功
echo.
echo =========================================
echo 🎉 部署完成！
echo =========================================
echo.
echo 服务地址: http://localhost:3000
echo API 信息: http://localhost:3000/api/info
echo ICE 配置: http://localhost:3000/api/turn
echo.
echo 查看日志: docker logs p2p-signaling-server -f
echo 停止服务: docker stop p2p-signaling-server
echo 重启服务: docker restart p2p-signaling-server
echo.
echo 下一步：配置 Cloudflare Tunnel
echo 参考文档: DEPLOY.md
echo.
pause