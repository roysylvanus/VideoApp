package com.example.videoapp.ui.permissions

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Handles runtime camera and microphone permissions required for video calls.
 *
 * Displays a fallback UI if permissions are not granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoChatPermissionWrapper(
    content: @Composable () -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    if (permissionsState.allPermissionsGranted) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Camera and Audio permissions are required for video chat.")

            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Grant Permissions")
            }
        }

        LaunchedEffect(Unit) {
            if (!permissionsState.allPermissionsGranted) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}