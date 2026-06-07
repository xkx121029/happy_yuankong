package com.p2p.remote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var etServerUrl: EditText
    private lateinit var etRoomId: EditText
    private lateinit var btnGenerateRoom: Button
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var btnControllerMode: Button
    private lateinit var btnControlledMode: Button
    private lateinit var layoutStatus: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var layoutClients: LinearLayout
    private lateinit var surfaceViewLocal: SurfaceViewRenderer
    private lateinit var surfaceViewRemote: SurfaceViewRenderer

    private var socket: Socket? = null
    private var isConnected = false
    private var myId = ""
    private var roomId = ""
    private var remoteId = ""
    private var isCalling = false
    private var mode = "controller"

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var eglBase: EglBase? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("turn:relay1.expressturn.com:3478")
            .setUsername("efWSSgtU58YrjR21QlVY")
            .setPassword("8x52G5a0T0nF7G6u")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:numb.viagenie.ca:3478")
            .setUsername("webrtc@live.com")
            .setPassword("muazkh")
            .createIceServer()
    )

    private val PERMISSIONS_REQUEST_CODE = 1001
    private val PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initWebRTC()
        checkPermissions()
    }

    private fun initViews() {
        etServerUrl = findViewById(R.id.etServerUrl)
        etRoomId = findViewById(R.id.etRoomId)
        btnGenerateRoom = findViewById(R.id.btnGenerateRoom)
        btnConnect = findViewById(R.id.btnConnect)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        btnControllerMode = findViewById(R.id.btnControllerMode)
        btnControlledMode = findViewById(R.id.btnControlledMode)
        layoutStatus = findViewById(R.id.layoutStatus)
        tvStatus = findViewById(R.id.tvStatus)
        layoutClients = findViewById(R.id.layoutClients)
        surfaceViewLocal = findViewById(R.id.surfaceViewLocal)
        surfaceViewRemote = findViewById(R.id.surfaceViewRemote)

        etServerUrl.setText("ws://10.0.2.2:3000")

        btnGenerateRoom.setOnClickListener {
            generateRandomRoomId()
        }

        btnConnect.setOnClickListener {
            connectToServer()
        }

        btnDisconnect.setOnClickListener {
            disconnectFromServer()
        }

        btnControllerMode.setOnClickListener {
            setMode("controller")
        }

        btnControlledMode.setOnClickListener {
            setMode("controlled")
        }

        setMode("controller")
    }

    private fun setMode(newMode: String) {
        mode = newMode
        if (mode == "controller") {
            btnControllerMode.setBackgroundResource(R.drawable.button_primary)
            btnControllerMode.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnControlledMode.setBackgroundResource(R.drawable.button_secondary)
            btnControlledMode.setTextColor(ContextCompat.getColor(this, R.color.black))
            btnConnect.text = "开始控制"
        } else {
            btnControlledMode.setBackgroundResource(R.drawable.button_primary)
            btnControlledMode.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnControllerMode.setBackgroundResource(R.drawable.button_secondary)
            btnControllerMode.setTextColor(ContextCompat.getColor(this, R.color.black))
            btnConnect.text = "等待控制"
        }
    }

    private fun initWebRTC() {
        eglBase = EglBase.create()
        val eglBaseContext = eglBase?.eglBaseContext

        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(this)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory()

        surfaceViewLocal.init(eglBaseContext, null)
        surfaceViewLocal.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        surfaceViewLocal.setEnableHardwareScaler(true)
        surfaceViewLocal.setMirror(true)

        surfaceViewRemote.init(eglBaseContext, null)
        surfaceViewRemote.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        surfaceViewRemote.setEnableHardwareScaler(true)
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        for (permission in PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(this, "需要授予权限才能使用应用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateRandomRoomId() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random()
        val roomId = StringBuilder()
        for (i in 0 until 6) {
            roomId.append(chars[random.nextInt(chars.length)])
        }
        etRoomId.setText(roomId.toString())
    }

    private fun connectToServer() {
        val serverUrl = etServerUrl.text.toString()
        roomId = etRoomId.text.toString()

        if (roomId.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_room_id), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val options = IO.Options()
            socket = IO.socket(serverUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                runOnUiThread {
                    socket?.emit("join", JSONObject().apply {
                        put("roomId", roomId)
                        put("deviceType", "android")
                        put("mode", mode)
                    })
                }
            }

            socket?.on("room-joined") { args ->
                runOnUiThread {
                    val data = args[0] as JSONObject
                    myId = data.getString("yourId")
                    val clients = data.getJSONArray("clients")

                    isConnected = true
                    updateUI()
                    updateClientsList(clients)
                }
            }

            socket?.on("client-left") { args ->
                runOnUiThread {
                    val data = args[0] as JSONObject
                    val remaining = data.getJSONArray("remaining")
                    val leftId = data.getString("leftId")

                    if (leftId == remoteId) {
                        hangup()
                    }

                    updateClientsList(remaining)
                }
            }

            socket?.on("offer") { args ->
                runOnUiThread {
                    val data = args[0] as JSONObject
                    val from = data.getString("from")
                    val offerJson = data.getJSONObject("offer")

                    if (mode == "controlled") {
                        remoteId = from
                        handleOffer(offerJson)
                    }
                }
            }

            socket?.on("answer") { args ->
                runOnUiThread {
                    val data = args[0] as JSONObject
                    val answerJson = data.getJSONObject("answer")
                    handleAnswer(answerJson)
                }
            }

            socket?.on("ice-candidate") { args ->
                runOnUiThread {
                    val data = args[0] as JSONObject
                    val candidateJson = data.getJSONObject("candidate")
                    handleIceCandidate(candidateJson)
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                runOnUiThread {
                    isConnected = false
                    updateUI()
                }
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.connection_failed), Toast.LENGTH_SHORT).show()
                }
            }

            socket?.connect()

        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.connection_failed), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun disconnectFromServer() {
        hangup()
        socket?.disconnect()
        socket = null
        isConnected = false
        updateUI()
        layoutClients.removeAllViews()
    }

    private fun updateUI() {
        if (isConnected) {
            btnConnect.visibility = View.GONE
            btnControllerMode.visibility = View.GONE
            btnControlledMode.visibility = View.GONE
            layoutStatus.visibility = View.VISIBLE
            val modeText = if (mode == "controller") "🎮 控制端" else "🖥️ 被控端"
            tvStatus.text = String.format(getString(R.string.connected_to_room), roomId) + " - " + modeText
        } else {
            btnConnect.visibility = View.VISIBLE
            btnControllerMode.visibility = View.VISIBLE
            btnControlledMode.visibility = View.VISIBLE
            layoutStatus.visibility = View.GONE
            surfaceViewLocal.visibility = View.GONE
            surfaceViewRemote.visibility = View.GONE
        }
    }

    private fun updateClientsList(clients: org.json.JSONArray) {
        layoutClients.removeAllViews()

        for (i in 0 until clients.length()) {
            val clientId = clients.getString(i)
            val clientView = LayoutInflater.from(this).inflate(R.layout.client_item, layoutClients, false)

            val tvClientName = clientView.findViewById<TextView>(R.id.tvClientName)
            val btnClientAction = clientView.findViewById<Button>(R.id.btnClientAction)

            if (clientId == myId) {
                tvClientName.text = getString(R.string.me)
                btnClientAction.visibility = View.GONE
            } else {
                tvClientName.text = clientId
                if (isCalling && clientId == remoteId) {
                    btnClientAction.text = getString(R.string.hangup)
                    btnClientAction.setBackgroundResource(R.drawable.button_danger)
                    btnClientAction.setOnClickListener {
                        hangup()
                    }
                } else if (!isCalling && mode == "controller") {
                    btnClientAction.text = getString(R.string.call)
                    btnClientAction.setBackgroundResource(R.drawable.button_primary)
                    btnClientAction.setOnClickListener {
                        callClient(clientId)
                    }
                } else {
                    btnClientAction.visibility = View.GONE
                }
            }

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, 0, 8)
            clientView.layoutParams = layoutParams

            layoutClients.addView(clientView)
        }
    }

    private fun callClient(clientId: String) {
        remoteId = clientId
        startCall()
    }

    private fun startCall() {
        try {
            videoCapturer = createCameraCapturer()
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)
            val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer?.isScreencast ?: false)

            videoCapturer?.initialize(surfaceTextureHelper, this, videoSource?.capturerObserver)
            videoCapturer?.startCapture(1280, 720, 30)

            localVideoTrack = peerConnectionFactory?.createVideoTrack("100", videoSource)
            localVideoTrack?.addSink(surfaceViewLocal)
            surfaceViewLocal.visibility = View.VISIBLE

            val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
            val localAudioTrack = peerConnectionFactory?.createAudioTrack("101", audioSource)

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
            peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnectionObserver() {
                override fun onIceCandidate(candidate: IceCandidate) {
                    val candidateJson = JSONObject().apply {
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                        put("candidate", candidate.sdp)
                    }

                    socket?.emit("ice-candidate", JSONObject().apply {
                        put("roomId", roomId)
                        put("to", remoteId)
                        put("candidate", candidateJson)
                    })
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    super.onTrack(transceiver)
                    transceiver?.receiver?.track()?.let { track ->
                        if (track is VideoTrack) {
                            runOnUiThread {
                                track.addSink(surfaceViewRemote)
                                surfaceViewRemote.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            })

            localVideoTrack?.let { peerConnection?.addTrack(it) }
            localAudioTrack?.let { peerConnection?.addTrack(it) }

            val sdpConstraints = MediaConstraints()
            sdpConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
            sdpConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))

            peerConnection?.createOffer(object : SdpObserverAdapter() {
                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    peerConnection?.setLocalDescription(SdpObserverAdapter(), sessionDescription)
                    val offerJson = JSONObject().apply {
                        put("type", sessionDescription.type.canonicalForm())
                        put("sdp", sessionDescription.description)
                    }
                    socket?.emit("offer", JSONObject().apply {
                        put("roomId", roomId)
                        put("to", remoteId)
                        put("offer", offerJson)
                    })
                }
            }, sdpConstraints)

            isCalling = true

        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.call_failed), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun handleOffer(offerJson: JSONObject) {
        try {
            videoCapturer = createCameraCapturer()
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)
            val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer?.isScreencast ?: false)

            videoCapturer?.initialize(surfaceTextureHelper, this, videoSource?.capturerObserver)
            videoCapturer?.startCapture(1280, 720, 30)

            localVideoTrack = peerConnectionFactory?.createVideoTrack("100", videoSource)
            localVideoTrack?.addSink(surfaceViewLocal)
            surfaceViewLocal.visibility = View.VISIBLE

            val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
            val localAudioTrack = peerConnectionFactory?.createAudioTrack("101", audioSource)

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
            peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnectionObserver() {
                override fun onIceCandidate(candidate: IceCandidate) {
                    val candidateJson = JSONObject().apply {
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                        put("candidate", candidate.sdp)
                    }

                    socket?.emit("ice-candidate", JSONObject().apply {
                        put("roomId", roomId)
                        put("to", remoteId)
                        put("candidate", candidateJson)
                    })
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    super.onTrack(transceiver)
                    transceiver?.receiver?.track()?.let { track ->
                        if (track is VideoTrack) {
                            runOnUiThread {
                                track.addSink(surfaceViewRemote)
                                surfaceViewRemote.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            })

            localVideoTrack?.let { peerConnection?.addTrack(it) }
            localAudioTrack?.let { peerConnection?.addTrack(it) }

            val sessionDescription = SessionDescription(
                SessionDescription.Type.fromCanonicalForm(offerJson.getString("type")),
                offerJson.getString("sdp")
            )

            peerConnection?.setRemoteDescription(SdpObserverAdapter(), sessionDescription)

            val sdpConstraints = MediaConstraints()
            sdpConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
            sdpConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))

            peerConnection?.createAnswer(object : SdpObserverAdapter() {
                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    peerConnection?.setLocalDescription(SdpObserverAdapter(), sessionDescription)
                    val answerJson = JSONObject().apply {
                        put("type", sessionDescription.type.canonicalForm())
                        put("sdp", sessionDescription.description)
                    }
                    socket?.emit("answer", JSONObject().apply {
                        put("roomId", roomId)
                        put("to", remoteId)
                        put("answer", answerJson)
                    })
                }
            }, sdpConstraints)

            isCalling = true

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleAnswer(answerJson: JSONObject) {
        try {
            val sessionDescription = SessionDescription(
                SessionDescription.Type.fromCanonicalForm(answerJson.getString("type")),
                answerJson.getString("sdp")
            )
            peerConnection?.setRemoteDescription(SdpObserverAdapter(), sessionDescription)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleIceCandidate(candidateJson: JSONObject) {
        try {
            val candidate = IceCandidate(
                candidateJson.getString("sdpMid"),
                candidateJson.getInt("sdpMLineIndex"),
                candidateJson.getString("candidate")
            )
            peerConnection?.addIceCandidate(candidate)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hangup() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer = null
            localVideoTrack = null
            peerConnection?.close()
            peerConnection = null
            isCalling = false
            remoteId = ""

            surfaceViewLocal.visibility = View.GONE
            surfaceViewRemote.visibility = View.GONE

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val cameraEnumerator = Camera2Enumerator(this)
        val deviceNames = cameraEnumerator.deviceNames

        for (deviceName in deviceNames) {
            if (cameraEnumerator.isFrontFacing(deviceName)) {
                val capturer = cameraEnumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    return capturer
                }
            }
        }

        for (deviceName in deviceNames) {
            val capturer = cameraEnumerator.createCapturer(deviceName, null)
            if (capturer != null) {
                return capturer
            }
        }

        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
        eglBase?.release()
    }
}

open class PeerConnectionObserver : PeerConnection.Observer {
    override fun onSignalingChange(p0: PeerConnection.SignalingState) {}
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState) {}
    override fun onIceConnectionReceivingChange(p0: Boolean) {}
    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {}
    override fun onIceCandidate(p0: IceCandidate) {}
    override fun onIceCandidatesRemoved(p0: Array<IceCandidate>) {}
    override fun onAddStream(p0: MediaStream) {}
    override fun onRemoveStream(p0: MediaStream) {}
    override fun onDataChannel(p0: DataChannel) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(p0: RtpReceiver, p1: Array<MediaStream>) {}
}

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String) {}
    override fun onSetFailure(p0: String) {}
}
