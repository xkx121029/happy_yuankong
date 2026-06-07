# Windows 客户端打包指南

## 📦 便携版客户端（已打包）

### 位置
```
windows-app/dist-portable/
```

### 使用方法
```bash
# 直接运行
cd windows-app/dist-portable
.\electron.exe
```

### 文件结构
```
dist-portable/
├── electron.exe           ← 主程序
├── resources/
│   └── app/
│       ├── index.html     ← 应用界面
│       ├── assets/        ← 静态资源
│       ├── main.js        ← Electron主进程
│       └── package.json   ← 应用配置
└── 其他依赖文件
```

## 🔧 重新构建

### 构建前端
```bash
cd windows-app
npm run build
```

### 更新便携版
```powershell
Copy-Item -Path "dist\*" -Destination "dist-portable\resources\app" -Recurse -Force
```

## 📦 创建安装包（可选）

### 使用Electron Builder

1. **安装依赖**
   ```bash
   npm install electron-builder --save-dev
   ```

2. **配置package.json**
   ```json
   {
     "build": {
       "appId": "com.p2p.remote",
       "productName": "P2P Remote Control",
       "win": {
         "target": ["nsis", "portable"],
         "icon": "build/icon.ico"
       }
     }
   }
   ```

3. **构建安装包**
   ```bash
   npm run electron:build
   ```

### 输出文件
```
dist-electron/
├── P2P Remote Control Setup.exe    ← 安装程序
├── P2P Remote Control.exe          ← 便携版
└── win-unpacked/                   ← 解压版
```

## 🌐 配置服务器地址

### 在应用界面修改
直接在应用界面输入服务器地址。

### 修改默认地址
编辑 `src/App.vue` 文件：
```javascript
const serverUrl = ref('ws://localhost:3000')

// 改为你的服务器地址
const serverUrl = ref('https://p2p-signaling.yourdomain.com')
```

## 📋 客户端功能

### 控制端（🎮）
- 远程控制其他设备
- 鼠标和键盘操作
- 高清屏幕共享

### 被控端（🖥️）
- 允许被远程控制
- 屏幕共享
- 接收控制指令

## 🚀 快速部署

### 方法一：直接使用便携版
```bash
# 复制整个 dist-portable 目录到目标机器
# 运行 electron.exe
```

### 方法二：创建安装包
```bash
# 构建安装程序
npm run electron:build

# 分发 Setup.exe
```

## 🔒 安全建议

1. 使用HTTPS服务器地址
2. 定期更新客户端
3. 使用强密码保护房间
4. 限制控制权限

## 📊 性能优化

### 视频质量设置
```javascript
// 高清模式
width: { ideal: 1920, max: 3840 }
height: { ideal: 1080, max: 2160 }
frameRate: { ideal: 30, max: 60 }

// 流畅模式
width: { ideal: 1280, max: 1920 }
height: { ideal: 720, max: 1080 }
frameRate: { ideal: 15, max: 30 }
```

## 🛠️ 故障排查

### 客户端无法启动
```bash
# 检查依赖
cd windows-app
npm install

# 重新构建
npm run build
```

### 连接失败
```bash
# 检查服务器地址
# 检查网络连接
# 检查防火墙设置
```

### 控制功能不工作
```bash
# 确保使用Electron客户端
# 浏览器模式无法控制
# 点击🖱️按钮启用控制
```

## 📞 支持

如有问题，请检查：
1. Electron是否正确安装
2. 前端是否成功构建
3. 服务器地址是否正确
4. 网络连接是否正常