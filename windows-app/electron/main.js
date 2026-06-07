const { app, BrowserWindow, ipcMain } = require('electron')
const path = require('path')
const { mouse, keyboard, Key, Point } = require('@nut-tree/nut-js')

mouse.config.mouseSpeed = 100

function createWindow() {
  const win = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    },
    frame: true,
    backgroundColor: '#ffffff'
  })

  if (process.env.NODE_ENV === 'development') {
    win.loadURL('http://localhost:5173')
    win.webContents.openDevTools()
  } else {
    win.loadFile(path.join(__dirname, '../dist/index.html'))
  }
}

ipcMain.handle('mouse-down', async (event, x, y, button) => {
  try {
    await mouse.setPosition(new Point(x, y))
    let mouseButton = mouse.Button.LEFT
    if (button === 1) mouseButton = mouse.Button.MIDDLE
    if (button === 2) mouseButton = mouse.Button.RIGHT
    await mouse.pressButton(mouseButton)
    return true
  } catch (error) {
    console.error('Mouse down error:', error)
    return false
  }
})

ipcMain.handle('mouse-up', async (event, x, y, button) => {
  try {
    await mouse.setPosition(new Point(x, y))
    let mouseButton = mouse.Button.LEFT
    if (button === 1) mouseButton = mouse.Button.MIDDLE
    if (button === 2) mouseButton = mouse.Button.RIGHT
    await mouse.releaseButton(mouseButton)
    return true
  } catch (error) {
    console.error('Mouse up error:', error)
    return false
  }
})

ipcMain.handle('mouse-move', async (event, x, y) => {
  try {
    await mouse.setPosition(new Point(x, y))
    return true
  } catch (error) {
    console.error('Mouse move error:', error)
    return false
  }
})

ipcMain.handle('mouse-wheel', async (event, deltaX, deltaY) => {
  try {
    await mouse.scrollDown(deltaY)
    if (deltaX !== 0) {
      await mouse.scrollLeft(deltaX)
    }
    return true
  } catch (error) {
    console.error('Mouse wheel error:', error)
    return false
  }
})

const keyMap = {
  'Backspace': Key.BACKSPACE,
  'Tab': Key.TAB,
  'Enter': Key.ENTER,
  'Shift': Key.SHIFT,
  'Control': Key.CONTROL,
  'Alt': Key.ALT,
  'Pause': Key.PAUSE,
  'CapsLock': Key.CAPS_LOCK,
  'Escape': Key.ESCAPE,
  'Space': Key.SPACE,
  'PageUp': Key.PAGE_UP,
  'PageDown': Key.PAGE_DOWN,
  'End': Key.END,
  'Home': Key.HOME,
  'ArrowLeft': Key.ARROW_LEFT,
  'ArrowUp': Key.ARROW_UP,
  'ArrowRight': Key.ARROW_RIGHT,
  'ArrowDown': Key.ARROW_DOWN,
  'Insert': Key.INSERT,
  'Delete': Key.DELETE,
  '0': Key.NUM0,
  '1': Key.NUM1,
  '2': Key.NUM2,
  '3': Key.NUM3,
  '4': Key.NUM4,
  '5': Key.NUM5,
  '6': Key.NUM6,
  '7': Key.NUM7,
  '8': Key.NUM8,
  '9': Key.NUM9,
  'a': Key.A,
  'b': Key.B,
  'c': Key.C,
  'd': Key.D,
  'e': Key.E,
  'f': Key.F,
  'g': Key.G,
  'h': Key.H,
  'i': Key.I,
  'j': Key.J,
  'k': Key.K,
  'l': Key.L,
  'm': Key.M,
  'n': Key.N,
  'o': Key.O,
  'p': Key.P,
  'q': Key.Q,
  'r': Key.R,
  's': Key.S,
  't': Key.T,
  'u': Key.U,
  'v': Key.V,
  'w': Key.W,
  'x': Key.X,
  'y': Key.Y,
  'z': Key.Z,
  'Meta': Key.META,
  'ContextMenu': Key.CONTEXT_MENU,
  'Numpad0': Key.NUMPAD0,
  'Numpad1': Key.NUMPAD1,
  'Numpad2': Key.NUMPAD2,
  'Numpad3': Key.NUMPAD3,
  'Numpad4': Key.NUMPAD4,
  'Numpad5': Key.NUMPAD5,
  'Numpad6': Key.NUMPAD6,
  'Numpad7': Key.NUMPAD7,
  'Numpad8': Key.NUMPAD8,
  'Numpad9': Key.NUMPAD9,
  'Multiply': Key.MULTIPLY,
  'Add': Key.ADD,
  'Subtract': Key.SUBTRACT,
  'Decimal': Key.DECIMAL,
  'Divide': Key.DIVIDE,
  'F1': Key.F1,
  'F2': Key.F2,
  'F3': Key.F3,
  'F4': Key.F4,
  'F5': Key.F5,
  'F6': Key.F6,
  'F7': Key.F7,
  'F8': Key.F8,
  'F9': Key.F9,
  'F10': Key.F10,
  'F11': Key.F11,
  'F12': Key.F12,
  'NumLock': Key.NUM_LOCK,
  'ScrollLock': Key.SCROLL_LOCK,
  'Semicolon': Key.SEMICOLON,
  'Equal': Key.EQUAL,
  'Comma': Key.COMMA,
  'Minus': Key.MINUS,
  'Period': Key.PERIOD,
  'Slash': Key.SLASH,
  'Backquote': Key.BACKQUOTE,
  'BracketLeft': Key.BRACKET_LEFT,
  'Backslash': Key.BACKSLASH,
  'BracketRight': Key.BRACKET_RIGHT,
  'Quote': Key.QUOTE
}

ipcMain.handle('key-down', async (event, key, code, ctrlKey, shiftKey, altKey) => {
  try {
    const keysToPress = []
    
    if (ctrlKey) keysToPress.push(Key.CONTROL)
    if (shiftKey) keysToPress.push(Key.SHIFT)
    if (altKey) keysToPress.push(Key.ALT)
    
    const mappedKey = keyMap[code] || keyMap[key] || (key.length === 1 ? key.toUpperCase() : null)
    if (mappedKey) {
      keysToPress.push(mappedKey)
    }
    
    if (keysToPress.length > 0) {
      await keyboard.pressKey(...keysToPress)
    }
    return true
  } catch (error) {
    console.error('Key down error:', error)
    return false
  }
})

ipcMain.handle('key-up', async (event, key, code) => {
  try {
    const mappedKey = keyMap[code] || keyMap[key] || (key.length === 1 ? key.toUpperCase() : null)
    if (mappedKey) {
      await keyboard.releaseKey(mappedKey)
    }
    return true
  } catch (error) {
    console.error('Key up error:', error)
    return false
  }
})

app.whenReady().then(createWindow)

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})
