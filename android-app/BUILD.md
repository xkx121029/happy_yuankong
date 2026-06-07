# Android 客户端打包指南

## 📱 打包APK

### 方法一：使用Android Studio（推荐）

1. **打开项目**
   ```bash
   # 在Android Studio中打开项目
   File → Open → 选择 android-app 目录
   ```

2. **等待Gradle同步**
   - Android Studio会自动下载依赖
   - 等待同步完成（可能需要几分钟）

3. **构建APK**
   ```bash
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **获取APK**
   ```bash
   # APK位置
   android-app/app/build/outputs/apk/debug/app-debug.apk
   ```

### 方法二：使用命令行

#### Windows系统

1. **设置环境变量**
   ```powershell
   $env:ANDROID_HOME = "${env:LOCALAPPDATA}\Android\Sdk"
   $env:PATH += ";${env:ANDROID_HOME}\tools;${env:ANDROID_HOME}\platform-tools;${env:ANDROID_HOME}\build-tools"
   ```

2. **构建APK**
   ```powershell
   cd android-app
   .\gradlew.bat assembleDebug
   ```

#### Linux/Mac系统

1. **设置环境变量**
   ```bash
   export ANDROID_HOME=/path/to/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools
   ```

2. **构建APK**
   ```bash
   cd android-app
   chmod +x gradlew
   ./gradlew assembleDebug
   ```

## 🔧 配置服务器地址

### 修改默认服务器地址

编辑 `MainActivity.kt` 文件：

```kotlin
// 第87行左右
etServerUrl.setText("ws://10.0.2.2:3000")  // 模拟器默认地址

// 改为你的服务器地址
etServerUrl.setText("https://p2p-signaling.yourdomain.com")  // Cloudflare Tunnel地址
```

### 或在运行时修改

在应用界面中直接输入服务器地址。

## 📦 构建发布版APK

### 1. 生成签名密钥

```bash
keytool -genkey -v -keystore release.keystore \
  -alias p2p-remote \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 2. 配置签名

编辑 `app/build.gradle.kts`：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "your_password"
            keyAlias = "p2p-remote"
            keyPassword = "your_password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 3. 构建发布版

```bash
# Android Studio
Build → Generate Signed Bundle / APK → APK → 选择签名配置

# 命令行
./gradlew assembleRelease
```

### 4. 获取发布版APK

```bash
android-app/app/build/outputs/apk/release/app-release.apk
```

## 📋 APK信息

| 类型 | 文件名 | 说明 |
|------|--------|------|
| Debug版 | app-debug.apk | 用于测试，未签名 |
| Release版 | app-release.apk | 用于发布，已签名优化 |

## 🔍 验证APK

```bash
# 查看APK信息
aapt dump badging app-debug.apk

# 查看签名信息
apksigner verify app-release.apk

# 安装到设备
adb install app-debug.apk
```

## 📱 安装到Android设备

### 方法一：通过ADB

```bash
adb install app-debug.apk
```

### 方法二：直接传输

1. 将APK文件传输到Android设备
2. 在设备上打开文件管理器
3. 点击APK文件安装

### 方法三：通过应用商店

上传到Google Play或其他应用商店。

## 🛠️ 常见问题

### Gradle同步失败

```bash
# 清理项目
./gradlew clean

# 删除缓存
rm -rf .gradle
rm -rf build

# 重新同步
./gradlew build
```

### SDK未找到

```bash
# 安装Android SDK
# Windows: https://developer.android.com/studio
# Linux: sudo apt install android-sdk
```

### 权限问题

```bash
# 给gradlew执行权限
chmod +x gradlew
```

## 📊 APK大小优化

### 启用压缩

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}
```

### 使用App Bundle

```bash
./gradlew bundleRelease
# 生成 .aab 文件，比APK更小
```

## 🔒 安全建议

1. 使用正式签名密钥
2. 启用代码混淆（ProGuard）
3. 启用资源压缩
4. 定期更新依赖

## 📞 支持

如有问题，请检查：
1. Android SDK是否正确安装
2. Gradle同步是否成功
3. 依赖是否完整下载
4. 签名配置是否正确