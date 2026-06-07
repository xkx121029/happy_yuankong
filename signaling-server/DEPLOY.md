# 信令服务器 Docker 部署指南

## 📦 部署到 NAS Docker

### 方法一：使用 docker-compose（推荐）

1. **上传文件到NAS**
   ```bash
   # 将整个 signaling-server 目录上传到 NAS
   scp -r signaling-server/ user@nas-ip:/volume1/docker/
   ```

2. **在NAS上运行**
   ```bash
   cd /volume1/docker/signaling-server
   docker-compose up -d
   ```

3. **查看运行状态**
   ```bash
   docker-compose ps
   docker-compose logs -f
   ```

### 方法二：手动构建镜像

1. **构建镜像**
   ```bash
   cd signaling-server
   docker build -t p2p-signaling-server:latest .
   ```

2. **运行容器**
   ```bash
   docker run -d \
     --name p2p-signaling-server \
     -p 3000:3000 \
     --restart unless-stopped \
     p2p-signaling-server:latest
   ```

## 🌐 Cloudflare Tunnel 配置

### 1. 安装 Cloudflared

**在NAS上安装（群晖）**：
```bash
# 通过套件中心安装 Cloudflared
# 或使用 Docker
docker run -d \
  --name cloudflared \
  --restart unless-stopped \
  cloudflare/cloudflared:latest \
  tunnel --no-autoupdate run --token <YOUR_TOKEN>
```

### 2. 创建 Tunnel

**在 Cloudflare Zero Trust Dashboard**：
1. 访问 https://one.dash.cloudflare.com/
2. 进入 **Networks** → **Tunnels**
3. 点击 **Create a tunnel**
4. 选择 **Cloudflared**
5. 命名隧道（如：`p2p-remote`）
6. 复制生成的 Token

### 3. 配置路由

在 Tunnel 配置中添加公共服务：

| 子域名 | 服务 | URL |
|--------|------|-----|
| `p2p-signaling.yourdomain.com` | HTTP | `http://localhost:3000` |

### 4. 启动 Tunnel

**使用 Token 启动**：
```bash
cloudflared tunnel run --token <YOUR_TOKEN>
```

**或使用 Docker**：
```bash
docker run -d \
  --name cloudflared \
  --restart unless-stopped \
  -e TUNNEL_TOKEN=<YOUR_TOKEN> \
  cloudflare/cloudflared:latest \
  tunnel --no-autoupdate run --token $TUNNEL_TOKEN
```

## 🔧 客户端配置

### Windows 客户端
```
服务器地址: https://p2p-signaling.yourdomain.com
```

### Android 客户端
```
服务器地址: https://p2p-signaling.yourdomain.com
```

## 📋 完整部署流程

### 群晖 NAS 部署示例

```bash
# 1. 上传文件
scp -r signaling-server/ admin@nas.local:/volume1/docker/

# 2. SSH 连接到 NAS
ssh admin@nas.local

# 3. 启动信令服务器
cd /volume1/docker/signaling-server
docker-compose up -d

# 4. 启动 Cloudflared（使用你的 Token）
docker run -d \
  --name cloudflared \
  --restart unless-stopped \
  -e TUNNEL_TOKEN=eyJh... \
  cloudflare/cloudflared:latest \
  tunnel --no-autoupdate run --token $TUNNEL_TOKEN

# 5. 检查状态
docker ps
docker logs p2p-signaling-server
docker logs cloudflared
```

## ✅ 验证部署

访问以下地址验证服务是否正常：
- `https://p2p-signaling.yourdomain.com/api/info` - 应返回服务器信息
- `https://p2p-signaling.yourdomain.com/api/turn` - 应返回 ICE 服务器配置

## 🔒 安全建议

1. **启用 Cloudflare Access**（可选）
   - 在 Zero Trust Dashboard 添加访问策略
   - 限制特定邮箱或IP访问

2. **使用环境变量**
   ```yaml
   environment:
     - PORT=3000
     - NODE_ENV=production
     - CORS_ORIGIN=https://yourdomain.com
   ```

3. **定期更新镜像**
   ```bash
   docker-compose pull
   docker-compose up -d
   ```

## 🛠️ 故障排查

### 查看日志
```bash
docker logs p2p-signaling-server -f
docker logs cloudflared -f
```

### 重启服务
```bash
docker-compose restart
```

### 检查端口
```bash
docker exec p2p-signaling-server wget -q -O- http://localhost:3000/api/info
```

## 📊 监控

### 健康检查
```bash
curl https://p2p-signaling.yourdomain.com/api/info
```

### 连接数统计
```bash
docker exec p2p-signaling-server node -e "console.log(process.connections)"
```