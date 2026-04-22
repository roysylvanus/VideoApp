package com.example.videoapp.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.videoapp.video.ConnectionState
import com.example.videoapp.viewmodels.VideoCallViewModel
import com.opentok.android.Publisher
import com.opentok.android.Subscriber


/**
 * Main video call screen.
 *
 * Responsible for:
 * - Rendering publisher and subscriber video
 * - Displaying connection state overlays
 * - Providing call controls (audio/video/end)
 */
@Composable
fun VideoCallScreen(
    paddingValues: PaddingValues,
    uiState: VideoCallViewModel.VideoCallState,
    onEvent: (VideoCallViewModel.VideoCallEvents) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(paddingValues)
    ) {
        uiState.subscriber?.let { SubscriberView(subscriber = it) }

        uiState.publisher?.let { PublisherView(publisher = it) }

        VideoCallOverlay(
            uiState = uiState,
            onEvent = onEvent
        )
    }
}

@Composable
private fun BoxScope.PublisherView(publisher: Publisher) {
    AndroidView(
        modifier = Modifier
            .size(90.dp, 120.dp)
            .align(Alignment.TopEnd)
            .padding(16.dp)
            .background(Color.LightGray),
        factory = {
            (publisher.view.parent as? ViewGroup)?.removeView(publisher.view)

            publisher.view.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}

@Composable
private fun SubscriberView(
    subscriber: Subscriber,
) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize(),
        factory = {
            (subscriber.view.parent as? ViewGroup)?.removeView(subscriber.view)

            subscriber.view.apply {
                this.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}

@Composable
private fun BoxScope.VideoCallOverlay(
    uiState: VideoCallViewModel.VideoCallState,
    onEvent: (VideoCallViewModel.VideoCallEvents) -> Unit
) {

    val connectionState = uiState.connectionState
    val hasSubscriber = uiState.subscriber != null

    // 🔹 Connection / waiting state
    ConnectionStatus(
        connectionState = connectionState,
        hasSubscriber = hasSubscriber,
        errorMessage = uiState.errorMessage,
        onEvent = onEvent
    )

    // 🔹 Controls
    CallControls(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(24.dp),
        uiState = uiState,
        onEvent = onEvent
    )
}

@Composable
private fun BoxScope.ConnectionStatus(
    connectionState: ConnectionState,
    hasSubscriber: Boolean,
    errorMessage: String?,
    onEvent: (VideoCallViewModel.VideoCallEvents) -> Unit
) {
    when (connectionState) {

        ConnectionState.CONNECTING -> {
            Text(
                text = "Connecting...",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        ConnectionState.IDLE -> {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ready to join call",
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onEvent(VideoCallViewModel.VideoCallEvents.OnStartCall)
                    }
                ) {
                    Text("Join Call")
                }
            }
        }

        ConnectionState.RECONNECTING -> {
            Text(
                text = "Reconnecting...",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Yellow
            )
        }

        ConnectionState.CONNECTED -> {
            if (!hasSubscriber) {
                Text(
                    text = "Waiting for participant...",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
        }

        ConnectionState.DISCONNECTED -> {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Disconnected", color = Color.Red)

                Button(
                    onClick = {
                        onEvent(VideoCallViewModel.VideoCallEvents.OnRetry)
                    }
                ) {
                    Text("Reconnect")
                }
            }
        }

        ConnectionState.ERROR -> {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onEvent(VideoCallViewModel.VideoCallEvents.OnRetry)
                    }
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun CallControls(
    modifier: Modifier = Modifier,
    uiState: VideoCallViewModel.VideoCallState,
    onEvent: (VideoCallViewModel.VideoCallEvents) -> Unit
) {
    val isConnected = uiState.connectionState == ConnectionState.CONNECTED

    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        Button(
            onClick = {
                onEvent(VideoCallViewModel.VideoCallEvents.OnToggleAudio)
            },
            enabled = isConnected
        ) {
            Text(text = if (uiState.isAudioEnabled) "Mute" else "Unmute")
        }

        Button(onClick = {
            onEvent(VideoCallViewModel.VideoCallEvents.OnToggleVideo)
        }, enabled = isConnected) {
            Text(text = if (uiState.isVideoEnabled) "Turn Camera Off" else "Turn Camera On")
        }

        Button(
            onClick = {
                onEvent(VideoCallViewModel.VideoCallEvents.OnEndCall)
            },
            enabled = isConnected,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            Text("End")
        }
    }
}