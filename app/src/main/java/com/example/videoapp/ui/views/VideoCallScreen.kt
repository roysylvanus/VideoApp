package com.example.videoapp.ui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.videoapp.video.VideoManager

@Composable
fun VideoCallView(paddingValues: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        PublisherView()
        SubscriberView()
    }
}


@Composable
private fun PublisherView() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            VideoManager.publisher.view
        }
    )
}

@Composable
private fun SubscriberView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        VideoManager.subscriber?.let {
            AndroidView(
                modifier = Modifier
                    .size(150.dp, 200.dp),
                factory = { context ->
                    it.view
                }
            )
        }
    }
}