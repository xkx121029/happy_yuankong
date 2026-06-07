# P2P 远程控制 - 信令服务器

## 📦 Docker 部署

### 快速部署（推荐）

**Windows**:
```bash
deploy.bat
```

**Linux/Mac/NAS**:
```bash
chmod +x deploy.sh
./deploy.sh
```

### 手动部署

**使用 docker-compose**:
```bash
docker-compose up -d
```

**手动构建**:
```bash
docker build -t p2p-signaling-server:latest .
docker run -d -p 3000:3000 --restart unless-stopped p2p-signaling-server:latest
```

## 🌐 Cloudflare Tunnel 配置

详细配置步骤请查看 [DEPLOY.md](DEPLOY.md)

### 快速配置

1. 在 Cloudflare Zero Trust 创建 Tunnel
2. 复制 Token
3. 运行 Cloudflared:
   ```bash
   docker run -d \
     --name cloudflared \
     --restart unless-stopped \
     -e TUNNEL_TOKEN=<YOUR_TOKEN> \
     cloudflare/cloudflared:latest \
     tunnel --no-autoupdate run --token $TUNNEL_TOKEN
   ```

4. 配置路由: `p2p-signaling.yourdomain.com → http://localhost:3000`

## 📋 文件说明

| 文件 | 说明 |
|------|------|
| `Dockerfile` | Docker 镜像构建文件 |
| `docker-compose.yml` | Docker Compose 配置 |
| `deploy.sh` | Linux/Mac/NAS 部署脚本 |
| `deploy.bat` | Windows 部署脚本 |
| `DEPLOY.md` | 详细部署文档 |
| `.dockerignore` | Docker 构建忽略文件 |
| `server.js` | 信令服务器主程序 |
| `package.json` | Node.js 依赖配置 |

## ✅ 验证部署

```bash
# 检查服务状态
curl http://localhost:3000/api/info

# 检查 ICE 配置
curl http://localhost:3000/api/turn
```

## 🔧 客户端配置

部署完成后，在客户端配置服务器地址：

```
服务器地址: https://p2p-signaling.yourdomain.com
```

## 🛠️ 常用命令

```bash
# 查看日志
docker logs p2p-signaling-server -f

# 重启服务
docker restart p2p-signaling-server

# 停止服务
docker stop p2p-signaling-server

# 删除容器
docker rm p2p-signaling-server

# 重新部署
docker-compose down
docker-compose up -d --build
```

## 📊 监控

```bash
# 查看容器状态
docker ps

# 查看资源使用
docker stats p2p-signaling-server

# 查看健康状态
docker inspect p2p-signaling-server | grep Health
```

## 🔒 安全建议

1. 使用 Cloudflare Access 限制访问
2. 配置 CORS 白名单
3. 定期更新 Docker 镜像
4. 启用 HTTPS（Cloudflare 自动提供）

## 📞 支持

如有问题，请查看 [DEPLOY.md](DEPLOY.md) 或检查日志：
```bash
docker logs p2p-signaling-server -f
docker logs cloudflared -f
```