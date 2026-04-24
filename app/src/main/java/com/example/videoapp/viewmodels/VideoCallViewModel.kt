package com.example.videoapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.example.videoapp.config.VonageVideoConfig
import com.example.videoapp.video.ConnectionState
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.Session
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VideoCallViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(VideoCallState())
    val state: StateFlow<VideoCallState> = _state

    private var session: Session? = null

    // Tracks whether disconnect was triggered by user action vs system/network
    private var isUserInitiatedDisconnect = false

    // Determines whether session should be resumed after app returns to foreground
    private var shouldReconnect = false

    // Initializes a new session only when user explicitly starts a call
    // Prevents accidental reconnections after user ends the call
    private fun connectToSession() {
        if (isUserInitiatedDisconnect) return
        if (session != null) return

        _state.value = _state.value.copy(
            connectionState = ConnectionState.CONNECTING
        )

        session = Session.Builder(
            application,
            VonageVideoConfig.APP_ID,
            VonageVideoConfig.SESSION_ID
        ).build().apply {
            setSessionListener(sessionListener)
            connect(VonageVideoConfig.TOKEN)
        }
    }

    private val sessionListener = object : Session.SessionListener {

        override fun onConnected(session: Session?) {
            Log.d(
                TAG,
                "onConnected in session: ${session?.sessionId}"
            )
            val publisher = Publisher.Builder(application).build()
                .apply {
                    setPublisherListener(publisherListener)
                    renderer.setStyle(
                        BaseVideoRenderer.STYLE_VIDEO_SCALE,
                        BaseVideoRenderer.STYLE_VIDEO_FILL
                    )

                    publishAudio = _state.value.isAudioEnabled
                    publishVideo = _state.value.isVideoEnabled
                }

            session?.publish(publisher)

            _state.value = _state.value.copy(
                connectionState = ConnectionState.CONNECTED,
                publisher = publisher,
                errorMessage = null
            )
        }

        override fun onStreamReceived(session: Session?, stream: Stream?) {
            stream ?: return
            Log.d(
                TAG,
                "onStreamReceived: New Stream Received ${stream.streamId} in session: ${session?.sessionId}"
            )
            val subscriber =
                Subscriber.Builder(application, stream).build().apply {
                    renderer.setStyle(
                        BaseVideoRenderer.STYLE_VIDEO_SCALE,
                        BaseVideoRenderer.STYLE_VIDEO_FILL
                    )
                    setSubscriberListener(subscriberListener)
                }

            _state.value = _state.value.copy(
                subscriber = subscriber
            )

            session?.subscribe(subscriber)
        }

        override fun onStreamDropped(session: Session?, stream: Stream?) {
            Log.d(TAG, "onStreamDropped: dropped from session: ${session?.sessionId}")

            _state.value = _state.value.copy(
                subscriber = null
            )
        }

        // Called for BOTH user-initiated and unexpected disconnects
        // We map to IDLE or DISCONNECTED based on user intent
        override fun onDisconnected(session: Session?) {
            Log.d(TAG, "onDisconnected: Disconnected from session: ${session?.sessionId}")

            val nextState = when {
                isUserInitiatedDisconnect -> ConnectionState.IDLE

                shouldReconnect -> ConnectionState.RECONNECTING   // 🔥 key fix

                else -> ConnectionState.DISCONNECTED
            }

            _state.value = _state.value.copy(
                connectionState = nextState,
                publisher = null,
                subscriber = null
            )

            isUserInitiatedDisconnect = false
        }

        override fun onError(session: Session?, error: OpentokError?) {
            Log.e(TAG, "Session error: ${error?.message}")

            _state.value = _state.value.copy(
                connectionState = ConnectionState.ERROR,
                errorMessage = error?.message ?: "Unknown error"
            )
        }
    }

    private val publisherListener = object : PublisherKit.PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamCreated: Publisher Stream Created. Stream ID: ${stream.streamId}")
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.d(
                TAG,
                "onStreamDestroyed: Publisher Stream Destroyed. Stream ID: ${stream.streamId}"
            )
            _state.value = _state.value.copy(
                publisher = null
            )
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            Log.e(TAG, "PublisherKit onError: ${opentokError.message}")

            _state.value = _state.value.copy(
                errorMessage = "Camera/Mic error: ${opentokError.message}"
            )
        }
    }

    private val subscriberListener = object : SubscriberKit.SubscriberListener {
        override fun onConnected(subscriberKit: SubscriberKit) {
            Log.d(
                TAG,
                "onConnected: Subscriber connected. Stream: ${subscriberKit.stream?.streamId}"
            )
        }

        override fun onDisconnected(subscriberKit: SubscriberKit) {
            Log.d(
                TAG,
                "onDisconnected: Subscriber disconnected. Stream: ${subscriberKit.stream?.streamId}"
            )
            _state.value = _state.value.copy(
                subscriber = null
            )
        }

        override fun onError(subscriberKit: SubscriberKit, opentokError: OpentokError) {
            Log.e(TAG, "SubscriberKit onError: ${opentokError.message}")

            _state.value = _state.value.copy(
                errorMessage = "Camera/Mic error: ${opentokError.message}",
            )
        }
    }

    fun onEvent(event: VideoCallEvents) {
        when (event) {
            is VideoCallEvents.OnResume -> {
                onResume()
            }

            is VideoCallEvents.OnDestroy -> {
                onDestroy(event.isChangingConfigurations)
            }

            is VideoCallEvents.OnPause -> {
                onPause(event.isChangingConfigurations)
            }

            is VideoCallEvents.OnEndCall -> {
                endCall()
            }

            is VideoCallEvents.OnToggleVideo -> {
                toggleVideo()
            }

            is VideoCallEvents.OnToggleAudio -> {
                toggleAudio()
            }

            is VideoCallEvents.OnRetry -> {
                reconnect()
            }

            is VideoCallEvents.OnStartCall -> {
                startCall()
            }
        }
    }

    private fun reconnect() {
        if (_state.value.connectionState == ConnectionState.CONNECTING) return

        session?.disconnect()
        session = null
        isUserInitiatedDisconnect = false

        _state.value = _state.value.copy(
            publisher = null,
            subscriber = null,
            connectionState = ConnectionState.CONNECTING
        )

        connectToSession()
    }

    private fun toggleAudio() {
        val newValue = !_state.value.isAudioEnabled

        // mutes audio sent
        _state.value.publisher?.publishAudio = newValue

        _state.value = _state.value.copy(
            isAudioEnabled = newValue,
        )
    }

    private fun toggleVideo() {
        val newValue = !_state.value.isVideoEnabled
        _state.value.publisher?.publishVideo = newValue

        _state.value = _state.value.copy(
            isVideoEnabled = newValue,
        )
    }

    private fun endCall() {
        isUserInitiatedDisconnect = true

        session?.disconnect()
        session = null

        shouldReconnect = false

        _state.value = _state.value.copy(
            connectionState = ConnectionState.IDLE,
            publisher = null,
            subscriber = null
        )
    }

    private fun startCall() {
        isUserInitiatedDisconnect = false
        shouldReconnect = false

        connectToSession()
    }

    private fun onResume() {
        if (isUserInitiatedDisconnect || session == null) return

        if (shouldReconnect) {
            session?.onResume()

            if (_state.value.connectionState != ConnectionState.CONNECTED) {
                _state.value = _state.value.copy(
                    connectionState = ConnectionState.RECONNECTING
                )
            }
        }

        shouldReconnect = false
    }

    private fun onPause(isChangingConfigurations: Boolean) {
        if (isChangingConfigurations) return

        shouldReconnect = true

        session?.onPause()

    }

    fun onDestroy(isChangingConfigurations: Boolean) {

        // DO NOT destroy session during rotation
        if (isChangingConfigurations) return

        session?.disconnect()
        session = null

        _state.value = _state.value.copy(
            connectionState = ConnectionState.IDLE,
            publisher = null,
            subscriber = null
        )

        isUserInitiatedDisconnect = false
        shouldReconnect = false
    }

    sealed class VideoCallEvents {
        data class OnPause(val isChangingConfigurations: Boolean) : VideoCallEvents()
        data object OnResume : VideoCallEvents()
        data class OnDestroy(val isChangingConfigurations: Boolean) : VideoCallEvents()
        data object OnEndCall : VideoCallEvents()
        data object OnToggleAudio : VideoCallEvents()
        data object OnToggleVideo : VideoCallEvents()
        data object OnRetry : VideoCallEvents()
        data object OnStartCall : VideoCallEvents()
    }

    data class VideoCallState(
        val connectionState: ConnectionState = ConnectionState.IDLE,
        val publisher: Publisher? = null,
        val subscriber: Subscriber? = null,
        val errorMessage: String? = null,
        val isAudioEnabled: Boolean = true,
        val isVideoEnabled: Boolean = true
    )

    private companion object {
        const val TAG = "VideoCallViewModel"
    }
}