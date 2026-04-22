package com.example.videoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.videoapp.ui.permissions.VideoChatPermissionWrapper
import com.example.videoapp.ui.theme.VideoAppTheme
import com.example.videoapp.ui.screens.VideoCallScreen
import com.example.videoapp.viewmodels.VideoCallViewModel

/**
 * Entry point for the video call screen.
 *
 * Responsible only for:
 * - Setting up Compose UI
 * - Forwarding lifecycle events to the ViewModel
 */
class MainActivity : ComponentActivity() {

    private val viewModel: VideoCallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VideoChatPermissionWrapper {
                        val uiState = viewModel.state.collectAsStateWithLifecycle()

                        VideoCallScreen(
                            paddingValues = innerPadding,
                            uiState = uiState.value,
                            onEvent = viewModel::onEvent
                        )
                    }
                }
            }
        }

    }


    override fun onResume() {
        super.onResume()
        // Forward lifecycle events to ViewModel to manage session state
        viewModel.onEvent(VideoCallViewModel.VideoCallEvents.OnResume)
    }

    override fun onPause() {
        super.onPause()
        // Pass configuration change flag to avoid unnecessary reconnects during configuration change
        viewModel.onEvent(VideoCallViewModel.VideoCallEvents.OnPause(isChangingConfigurations))
    }

    override fun onDestroy() {
        // Avoid disconnecting session during configuration changes
        viewModel.onEvent(VideoCallViewModel.VideoCallEvents.OnDestroy(isChangingConfigurations))
        super.onDestroy()
    }
}