# P2P 远程控制软件

一款跨平台的P2P远程控制软件，支持Windows和Android设备之间的互联。

## 项目结构

```
happy_yuankong/
├── signaling-server/      # WebRTC信令服务器 (Node.js + Socket.IO)
├── windows-app/          # Windows客户端 (Electron + Vue3)
├── android-app/          # Android客户端 (原生Kotlin)
└── README.md
```

## 快速开始

### 1. 启动信令服务器

```bash
cd signaling-server
npm install
npm start
```

信令服务器将在 `http://localhost:3000` 启动。

### 2. 运行Windows客户端

```bash
cd windows-app
npm install
npm run electron:dev
```

### 3. 运行Android客户端

使用Android Studio打开 `android-app/` 目录，构建并安装到Android设备上。

## 使用说明

1. 确保所有设备连接到同一网络（或信令服务器可公网访问）
2. 在Windows或Android客户端上生成或输入房间号
3. 两个设备加入同一房间
4. 在设备列表中点击连接按钮建立P2P连接
5. 开始远程控制

## 技术栈

- **信令服务器**: Node.js + Express + Socket.IO
- **Windows客户端**: Electron + Vue3 + WebRTC
- **Android客户端**: Kotlin + WebRTC Android SDK

## 注意事项

- 如需外网访问，需要配置STUN/TURN服务器
- 建议使用HTTPS部署生产环境的信令服务器
- 首次使用需要授予摄像头/麦克风权限
