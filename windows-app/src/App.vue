<template>
  <div class="app-container">
    <div class="main-card">
      <h1 class="title">P2P 远程控制</h1>
      
      <div class="server-status">
        <span :class="['status-dot', isServerConnected ? 'connected' : 'disconnected']"></span>
        <span>信令服务器: {{ isServerConnected ? '已连接' : '未连接' }}</span>
      </div>
      
      <div class="connection-section">
        <div class="input-group">
          <label>服务器地址</label>
          <input v-model="serverUrl" placeholder="ws://localhost:3000" />
        </div>
        
        <div class="input-group">
          <label>房间号</label>
          <div class="room-input-container">
            <input v-model="roomId" placeholder="输入或创建房间" />
            <button @click="generateRoomId" class="btn-secondary">随机生成</button>
          </div>
        </div>
        
        <div class="input-group">
          <label>角色模式</label>
          <div class="mode-selector">
            <button 
              @click="mode = 'controller'" 
              :class="['mode-btn', { active: mode === 'controller' }]"
            >
              <span class="mode-icon">🎮</span>
              <span>控制端</span>
            </button>
            <button 
              @click="mode = 'controlled'" 
              :class="['mode-btn', { active: mode === 'controlled' }]"
            >
              <span class="mode-icon">🖥️</span>
              <span>被控端</span>
            </button>
          </div>
        </div>
        
        <button v-if="!isConnected" @click="connect" class="btn-primary">
          {{ mode === 'controller' ? '开始控制' : '等待控制' }}
        </button>
        
        <button v-else @click="disconnect" class="btn-danger">
          断开连接
        </button>
      </div>

      <div v-if="isConnected" class="status-section">
        <div class="status-item">
          <span class="status-dot connected"></span>
          <span>已连接到房间: {{ roomId }}</span>
          <span class="mode-badge">{{ mode === 'controller' ? '🎮 控制端' : '🖥️ 被控端' }}</span>
        </div>
        
        <div class="clients-list">
          <h3>在线设备</h3>
          <div v-for="client in clients" :key="client" class="client-item">
            <span>{{ client === myId ? '我' : client }}</span>
            <button 
              v-if="client !== myId && !isCalling && mode === 'controller'" 
              @click="callClient(client)" 
              class="btn-small"
            >
              控制
            </button>
            <button 
              v-if="client === remoteId" 
              @click="hangup" 
              class="btn-small btn-danger"
            >
              挂断
            </button>
          </div>
        </div>
      </div>

      <div v-if="isCalling" class="video-section">
        <div class="video-controls">
          <button @click="toggleFullscreen" class="control-btn">
            ⛶
          </button>
          <button @click="toggleControl" :class="['control-btn', { active: isControlEnabled }]">
            🖱️
          </button>
          <button @click="hangup" class="control-btn hangup-btn">
            📞
          </button>
        </div>
        <div ref="videoContainer" class="video-container" @mousedown="handleMouseDown" @mousemove="handleMouseMove" @mouseup="handleMouseUp" @mouseleave="handleMouseUp" @wheel="handleWheel" @keydown="handleKeyDown" @keyup="handleKeyUp" tabindex="0">
          <video 
            ref="remoteVideo" 
            autoplay 
            playsinline 
            class="video-main"
            :class="{ fullscreen: isFullscreen }"
          ></video>
          <video 
            ref="localVideo" 
            autoplay 
            muted 
            playsinline 
            class="video-pip"
          ></video>
          <div v-if="mode === 'controller'" class="control-hint">
            <span v-if="isElectron()">点击🖱️按钮启用远程控制</span>
            <span v-else>💡 完整控制需要使用Electron客户端</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { io } from 'socket.io-client'

const serverUrl = ref('ws://localhost:3000')
const roomId = ref('')
const mode = ref('controller')
const socket = ref(null)
const isConnected = ref(false)
const isServerConnected = ref(false)
const clients = ref([])
const myId = ref('')
const remoteId = ref('')
const isCalling = ref(false)
const isFullscreen = ref(false)
const isControlEnabled = ref(false)
const localVideo = ref(null)
const remoteVideo = ref(null)
const videoContainer = ref(null)
const peerConnection = ref(null)
const dataChannel = ref(null)
const localStream = ref(null)
const videoDimensions = ref({ width: 1920, height: 1080 })

const pcConfig = ref({
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    { urls: 'stun:stun2.l.google.com:19302' },
    {
      urls: 'turn:relay1.expressturn.com:3478',
      username: 'efWSSgtU58YrjR21QlVY',
      credential: '8x52G5a0T0nF7G6u'
    },
    {
      urls: 'turn:numb.viagenie.ca:3478',
      username: 'webrtc@live.com',
      credential: 'muazkh'
    }
  ]
})

const sdpConstraints = {
  offerToReceiveVideo: true,
  offerToReceiveAudio: true
}

const generateRoomId = () => {
  roomId.value = Math.random().toString(36).substring(2, 8).toUpperCase()
}

const connect = async () => {
  if (!roomId.value) {
    alert('请输入房间号')
    return
  }

  try {
    socket.value = io(serverUrl.value)
    
    socket.value.on('connect', () => {
      isServerConnected.value = true
      socket.value.emit('join', { 
        roomId: roomId.value, 
        deviceType: 'windows',
        mode: mode.value
      })
    })

    socket.value.on('room-joined', (data) => {
      isConnected.value = true
      clients.value = data.clients
      myId.value = data.yourId
    })

    socket.value.on('client-left', (data) => {
      clients.value = data.remaining
      if (data.leftId === remoteId.value) {
        hangup()
      }
    })

    socket.value.on('offer', async (data) => {
      if (mode.value === 'controlled') {
        await handleOffer(data.from, data.offer)
      }
    })

    socket.value.on('answer', async (data) => {
      await handleAnswer(data.answer)
    })

    socket.value.on('ice-candidate', async (data) => {
      await handleIceCandidate(data.candidate)
    })

  } catch (error) {
    alert('连接失败，请检查服务器地址')
  }
}

const disconnect = () => {
  if (socket.value) {
    socket.value.disconnect()
    isServerConnected.value = false
  }
  hangup()
  isConnected.value = false
  clients.value = []
  myId.value = ''
}

const callClient = async (clientId) => {
  remoteId.value = clientId
  
  try {
    peerConnection.value = new RTCPeerConnection(pcConfig.value)
    
    peerConnection.value.onicecandidate = (event) => {
      if (event.candidate) {
        socket.value.emit('ice-candidate', {
          roomId: roomId.value,
          to: remoteId.value,
          candidate: event.candidate
        })
      }
    }

    peerConnection.value.ontrack = (event) => {
      if (remoteVideo.value && event.streams[0]) {
        remoteVideo.value.srcObject = event.streams[0]
      }
    }

    peerConnection.value.ondatachannel = (event) => {
      dataChannel.value = event.channel
      setupDataChannel()
    }

    dataChannel.value = peerConnection.value.createDataChannel('control')
    setupDataChannel()

    const offer = await peerConnection.value.createOffer(sdpConstraints)
    await peerConnection.value.setLocalDescription(offer)
    
    socket.value.emit('offer', {
      roomId: roomId.value,
      to: remoteId.value,
      offer: peerConnection.value.localDescription
    })

    isCalling.value = true
  } catch (error) {
    alert('建立连接失败')
  }
}

const setupDataChannel = () => {
  if (!dataChannel.value) return
  
  dataChannel.value.onmessage = (event) => {
    const message = JSON.parse(event.data)
    if (message.type === 'dimensions') {
      videoDimensions.value = message.data
    }
  }
}

const sendControlMessage = (type, data) => {
  if (dataChannel.value && dataChannel.value.readyState === 'open' && isControlEnabled.value) {
    const message = JSON.stringify({ type, data })
    dataChannel.value.send(message)
  }
}

const handleMouseDown = (e) => {
  if (!isControlEnabled.value || mode.value !== 'controller') return
  
  const rect = videoContainer.value.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * videoDimensions.value.width
  const y = ((e.clientY - rect.top) / rect.height) * videoDimensions.value.height
  
  sendControlMessage('mousedown', { x, y, button: e.button })
}

const handleMouseMove = (e) => {
  if (!isControlEnabled.value || mode.value !== 'controller') return
  
  const rect = videoContainer.value.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * videoDimensions.value.width
  const y = ((e.clientY - rect.top) / rect.height) * videoDimensions.value.height
  
  sendControlMessage('mousemove', { x, y })
}

const handleMouseUp = (e) => {
  if (!isControlEnabled.value || mode.value !== 'controller') return
  
  const rect = videoContainer.value.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * videoDimensions.value.width
  const y = ((e.clientY - rect.top) / rect.height) * videoDimensions.value.height
  
  sendControlMessage('mouseup', { x, y, button: e.button })
}

const handleWheel = (e) => {
  if (!isControlEnabled.value || mode.value !== 'controller') return
  
  e.preventDefault()
  sendControlMessage('wheel', { deltaX: e.deltaX, deltaY: e.deltaY })
}

const handleKeyDown = (e) => {
  if (!isControlEnabled.value || mode.value !== 'controller') return
  
  sendControlMessage('keydown', { key: e.key, code: e.code, ctrlKey: e.ctrlKey, shiftKey: e.shiftKey, altKey: e.altKey })
}

const handleKeyUp = (e) => {
  if (!isControlEnabled.value || mode.value !== 'controller') return
  
  sendControlMessage('keyup', { key: e.key, code: e.code })
}

const toggleControl = () => {
  isControlEnabled.value = !isControlEnabled.value
  if (isControlEnabled.value && videoContainer.value) {
    videoContainer.value.focus()
  }
}

const handleOffer = async (from, offer) => {
  remoteId.value = from
  
  try {
    localStream.value = await navigator.mediaDevices.getDisplayMedia({
      video: {
        width: { ideal: 1920, max: 3840 },
        height: { ideal: 1080, max: 2160 },
        frameRate: { ideal: 30, max: 60 }
      },
      audio: false
    })
    
    const track = localStream.value.getVideoTracks()[0]
    if (track) {
      const settings = track.getSettings()
      videoDimensions.value = { width: settings.width || 1920, height: settings.height || 1080 }
    }
    
    if (localVideo.value) {
      localVideo.value.srcObject = localStream.value
    }

    peerConnection.value = new RTCPeerConnection(pcConfig.value)
    
    peerConnection.value.onicecandidate = (event) => {
      if (event.candidate) {
        socket.value.emit('ice-candidate', {
          roomId: roomId.value,
          to: remoteId.value,
          candidate: event.candidate
        })
      }
    }

    localStream.value.getTracks().forEach(track => {
      peerConnection.value.addTrack(track, localStream.value)
    })

    peerConnection.value.ondatachannel = (event) => {
      dataChannel.value = event.channel
      setupControlledDataChannel()
    }

    await peerConnection.value.setRemoteDescription(new RTCSessionDescription(offer))
    const answer = await peerConnection.value.createAnswer(sdpConstraints)
    await peerConnection.value.setLocalDescription(answer)
    
    socket.value.emit('answer', {
      roomId: roomId.value,
      to: remoteId.value,
      answer: peerConnection.value.localDescription
    })

    isCalling.value = true
  } catch (error) {
  }
}

const setupControlledDataChannel = () => {
  if (!dataChannel.value) return
  
  dataChannel.value.onopen = () => {
    sendControlMessage('dimensions', videoDimensions.value)
  }
  
  dataChannel.value.onmessage = (event) => {
    const message = JSON.parse(event.data)
    executeControlAction(message)
  }
}

const executeControlAction = (message) => {
  if (mode.value !== 'controlled') return
  
  const { type, data } = message
  
  if (type === 'mousedown') {
    simulateMouseDown(data.x, data.y, data.button)
  } else if (type === 'mousemove') {
    simulateMouseMove(data.x, data.y)
  } else if (type === 'mouseup') {
    simulateMouseUp(data.x, data.y, data.button)
  } else if (type === 'wheel') {
    simulateWheel(data.deltaX, data.deltaY)
  } else if (type === 'keydown') {
    simulateKeyDown(data)
  } else if (type === 'keyup') {
    simulateKeyUp(data)
  }
}

const isElectron = () => {
  return !!window.electronAPI
}

const simulateMouseDown = (x, y, button) => {
  if (isElectron()) {
    window.electronAPI.mouseDown(x, y, button)
  }
}

const simulateMouseMove = (x, y) => {
  if (isElectron()) {
    window.electronAPI.mouseMove(x, y)
  }
}

const simulateMouseUp = (x, y, button) => {
  if (isElectron()) {
    window.electronAPI.mouseUp(x, y, button)
  }
}

const simulateWheel = (deltaX, deltaY) => {
  if (isElectron()) {
    window.electronAPI.mouseWheel(deltaX, deltaY)
  }
}

const simulateKeyDown = (keyData) => {
  if (isElectron()) {
    window.electronAPI.keyDown(keyData.key, keyData.code, keyData.ctrlKey, keyData.shiftKey, keyData.altKey)
  }
}

const simulateKeyUp = (keyData) => {
  if (isElectron()) {
    window.electronAPI.keyUp(keyData.key, keyData.code)
  }
}

const handleAnswer = async (answer) => {
  if (peerConnection.value) {
    await peerConnection.value.setRemoteDescription(new RTCSessionDescription(answer))
  }
}

const handleIceCandidate = async (candidate) => {
  if (peerConnection.value && candidate) {
    await peerConnection.value.addIceCandidate(new RTCIceCandidate(candidate))
  }
}

const hangup = () => {
  if (dataChannel.value) {
    dataChannel.value.close()
    dataChannel.value = null
  }
  
  if (peerConnection.value) {
    peerConnection.value.close()
    peerConnection.value = null
  }
  
  if (localStream.value) {
    localStream.value.getTracks().forEach(track => track.stop())
    localStream.value = null
  }
  
  if (localVideo.value) {
    localVideo.value.srcObject = null
  }
  
  if (remoteVideo.value) {
    remoteVideo.value.srcObject = null
  }
  
  isCalling.value = false
  isControlEnabled.value = false
  remoteId.value = ''
  isFullscreen.value = false
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    videoContainer.value?.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

onUnmounted(() => {
  disconnect()
})
</script>

<style scoped>
.app-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.main-card {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 800px;
}

.title {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 28px;
  font-weight: 600;
}

.connection-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 30px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-group label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.input-group input {
  padding: 14px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.input-group input:focus {
  outline: none;
  border-color: #667eea;
}

.room-input-container {
  display: flex;
  gap: 10px;
}

.room-input-container input {
  flex: 1;
}

.mode-selector {
  display: flex;
  gap: 10px;
}

.mode-btn {
  flex: 1;
  padding: 16px;
  background: #f5f5f5;
  border: 2px solid transparent;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.mode-btn:hover {
  background: #eee;
}

.mode-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: #667eea;
}

.mode-icon {
  font-size: 24px;
}

.btn-primary {
  padding: 16px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  padding: 14px 20px;
  background: #f0f0f0;
  color: #333;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.btn-danger {
  padding: 16px 24px;
  background: #ff4757;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-danger:hover {
  background: #ff3838;
}

.btn-small {
  padding: 8px 16px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-small:hover {
  background: #5568d3;
}

.btn-small.btn-danger {
  padding: 8px 16px;
  font-size: 14px;
  background: #ff4757;
}

.status-section {
  margin-bottom: 30px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f0fff4;
  border-radius: 12px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #ccc;
}

.status-dot.connected {
  background: #2ed573;
  animation: pulse 2s infinite;
}

.mode-badge {
  margin-left: auto;
  padding: 6px 12px;
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.clients-list {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
}

.clients-list h3 {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.client-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-radius: 10px;
  margin-bottom: 10px;
}

.video-section {
  margin-top: 20px;
}

.video-controls {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 10px;
}

.control-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.control-btn:hover {
  background: white;
  transform: scale(1.1);
}

.control-btn.active {
  background: #667eea;
  color: white;
}

.control-btn.hangup-btn {
  background: #ff4757;
  color: white;
}

.video-container {
  position: relative;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16/9;
  cursor: crosshair;
  outline: none;
}

.video-main {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-main.fullscreen {
  width: 100vw;
  height: 100vh;
}

.video-pip {
  position: absolute;
  bottom: 15px;
  right: 15px;
  width: 150px;
  height: 84px;
  border: 3px solid white;
  border-radius: 8px;
  object-fit: cover;
  background: #000;
}

.control-hint {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border-radius: 8px;
  font-size: 12px;
}

@media (max-width: 600px) {
  .video-pip {
    width: 100px;
    height: 56px;
  }
}
</style>
