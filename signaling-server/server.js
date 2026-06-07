const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const os = require('os');

const app = express();
app.use(cors());

app.get('/api/turn', (req, res) => {
  res.json({
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
      { urls: 'stun:stun2.l.google.com:19302' },
      { urls: 'stun:stun3.l.google.com:19302' },
      { urls: 'stun:stun4.l.google.com:19302' },
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
  });
});

app.get('/api/info', (req, res) => {
  const interfaces = os.networkInterfaces();
  const addresses = [];
  
  Object.keys(interfaces).forEach((name) => {
    interfaces[name].forEach((iface) => {
      if (iface.family === 'IPv4' && !iface.internal) {
        addresses.push(iface.address);
      }
    });
  });
  
  res.json({
    port: PORT,
    localAddresses: addresses,
    publicAddress: req.ip || 'unknown'
  });
});

const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  },
  allowEIO3: true,
  transports: ['websocket', 'polling']
});

const rooms = new Map();

io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);

  socket.on('join', (data) => {
    const { roomId, deviceType, mode } = data;
    console.log(`Device ${socket.id} (${deviceType}, ${mode}) joining room ${roomId}`);
    
    socket.join(roomId);
    
    if (!rooms.has(roomId)) {
      rooms.set(roomId, new Map());
    }
    
    rooms.get(roomId).set(socket.id, { deviceType, mode, socket });
    
    const clientsInRoom = Array.from(rooms.get(roomId).keys());
    const clientInfo = [];
    rooms.get(roomId).forEach((info, id) => {
      clientInfo.push({ id, deviceType: info.deviceType, mode: info.mode });
    });
    
    io.to(roomId).emit('room-joined', { 
      roomId, 
      clients: clientsInRoom, 
      clientInfo,
      yourId: socket.id 
    });
  });

  socket.on('offer', (data) => {
    const { roomId, to, offer } = data;
    console.log(`Offer from ${socket.id} to ${to} in room ${roomId}`);
    io.to(to).emit('offer', { from: socket.id, offer });
  });

  socket.on('answer', (data) => {
    const { roomId, to, answer } = data;
    console.log(`Answer from ${socket.id} to ${to} in room ${roomId}`);
    io.to(to).emit('answer', { from: socket.id, answer });
  });

  socket.on('ice-candidate', (data) => {
    const { roomId, to, candidate } = data;
    console.log(`ICE candidate from ${socket.id} to ${to} in room ${roomId}`);
    io.to(to).emit('ice-candidate', { from: socket.id, candidate });
  });

  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
    
    rooms.forEach((clients, roomId) => {
      if (clients.has(socket.id)) {
        clients.delete(socket.id);
        socket.leave(roomId);
        
        const remainingClients = Array.from(clients.keys());
        const clientInfo = [];
        clients.forEach((info, id) => {
          clientInfo.push({ id, deviceType: info.deviceType, mode: info.mode });
        });
        
        if (remainingClients.length > 0) {
          io.to(roomId).emit('client-left', { 
            leftId: socket.id, 
            remaining: remainingClients,
            clientInfo 
          });
        }
        
        if (clients.size === 0) {
          rooms.delete(roomId);
        }
      }
    });
  });
});

const PORT = process.env.PORT || 3000;

server.listen(PORT, '0.0.0.0', () => {
  const interfaces = os.networkInterfaces();
  console.log('\n=========================================');
  console.log('Signaling Server Started');
  console.log(`Port: ${PORT}`);
  console.log('Local Addresses:');
  Object.keys(interfaces).forEach((name) => {
    interfaces[name].forEach((iface) => {
      if (iface.family === 'IPv4' && !iface.internal) {
        console.log(`  ${name}: ${iface.address}:${PORT}`);
      }
    });
  });
  console.log('=========================================\n');
});
