const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('electronAPI', {
  mouseDown: (x, y, button) => ipcRenderer.invoke('mouse-down', x, y, button),
  mouseUp: (x, y, button) => ipcRenderer.invoke('mouse-up', x, y, button),
  mouseMove: (x, y) => ipcRenderer.invoke('mouse-move', x, y),
  mouseWheel: (deltaX, deltaY) => ipcRenderer.invoke('mouse-wheel', deltaX, deltaY),
  keyDown: (key, code, ctrlKey, shiftKey, altKey) => ipcRenderer.invoke('key-down', key, code, ctrlKey, shiftKey, altKey),
  keyUp: (key, code) => ipcRenderer.invoke('key-up', key, code)
})
