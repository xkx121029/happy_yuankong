#!/bin/bash

echo "========================================="
echo "P2P 远程控制 - 信令服务器部署脚本"
echo "========================================="
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

echo "✅ Docker 已安装"
echo ""

# 构建镜像
echo "📦 构建 Docker 镜像..."
docker build -t p2p-signaling-server:latest .

if [ $? -eq 0 ]; then
    echo "✅ 镜像构建成功"
else
    echo "❌ 镜像构建失败"
    exit 1
fi

echo ""

# 运行容器
echo "🚀 启动容器..."
docker run -d \
    --name p2p-signaling-server \
    -p 3000:3000 \
    --restart unless-stopped \
    p2p-signaling-server:latest

if [ $? -eq 0 ]; then
    echo "✅ 容器启动成功"
else
    echo "❌ 容器启动失败"
    exit 1
fi

echo ""
echo "========================================="
echo "🎉 部署完成！"
echo "========================================="
echo ""
echo "服务地址: http://localhost:3000"
echo "API 信息: http://localhost:3000/api/info"
echo "ICE 配置: http://localhost:3000/api/turn"
echo ""
echo "查看日志: docker logs p2p-signaling-server -f"
echo "停止服务: docker stop p2p-signaling-server"
echo "重启服务: docker restart p2p-signaling-server"
echo ""
echo "下一步：配置 Cloudflare Tunnel"
echo "参考文档: DEPLOY.md"
echo ""