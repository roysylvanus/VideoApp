package com.example.videoapp


import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.videoapp.video.ConnectionState
import com.example.videoapp.viewmodels.VideoCallViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VideoCallViewModelTest {

    private lateinit var viewModel: VideoCallViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        viewModel = VideoCallViewModel(context)
    }

    @Test
    fun `when viewmodel is initialized, then state should be IDLE`() {
        val state = viewModel.state.value

        assertEquals(ConnectionState.IDLE, state.connectionState)
        assertNull(state.publisher)
        assertNull(state.subscriber)
        assertTrue(state.isAudioEnabled)
        assertTrue(state.isVideoEnabled)
    }

    @Test
    fun `when event is OnToggleAudio, then should update isAudioEnabled`() {
        val initial = viewModel.state.value.isAudioEnabled

        viewModel.onEvent(VideoCallViewModel.VideoCallEvents.OnToggleAudio)

        val updated = viewModel.state.value.isAudioEnabled

        assertEquals(!initial, updated)
    }

    @Test
    fun `when event is OnToggleVideo, then should update isVideoEnabled`() {
        val initial = viewModel.state.value.isVideoEnabled

        viewModel.onEvent(VideoCallViewModel.VideoCallEvents.OnToggleVideo)

        val updated = viewModel.state.value.isVideoEnabled

        assertEquals(!initial, updated)
    }
}