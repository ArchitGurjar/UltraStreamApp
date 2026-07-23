package com.ultrastream.app.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private lateinit var exoPlayer: ExoPlayer

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun initializePlayer(url: String, title: String) {
        viewModelScope.launch {
            try {
                val trackSelector = DefaultTrackSelector(android.content.ContextWrapper(null))
                exoPlayer = ExoPlayer.Builder(androidx.media3.common.util.Util.getApplicationContext())
                    .setTrackSelector(trackSelector)
                    .build()

                val dataSourceFactory = DefaultHttpDataSource.Factory()
                val mediaSource = createMediaSource(url, dataSourceFactory)
                
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true

                _uiState.value = _uiState.value.copy(
                    isPlaying = true,
                    title = title,
                    currentUrl = url
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun createMediaSource(url: String, dataSourceFactory: DefaultHttpDataSource.Factory): MediaSource {
        val uri = android.net.Uri.parse(url)
        return when {
            url.contains(".m3u8") -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
            url.contains(".mpd") -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
            else -> androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
        }
    }

    fun playPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _uiState.value = _uiState.value.copy(isPlaying = false)
        } else {
            exoPlayer.play()
            _uiState.value = _uiState.value.copy(isPlaying = true)
        }
    }

    fun skipForward(seconds: Long = 10) {
        exoPlayer.seekTo(exoPlayer.currentPosition + seconds * 1000)
    }

    fun skipBackward(seconds: Long = 10) {
        exoPlayer.seekTo(exoPlayer.currentPosition - seconds * 1000)
    }

    fun setSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(speed = speed)
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
        _uiState.value = _uiState.value.copy(volume = volume)
    }

    fun setBrightness(brightness: Float) {
        _uiState.value = _uiState.value.copy(brightness = brightness)
    }

    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }

    fun togglePiP() {
        _uiState.value = _uiState.value.copy(isPiP = !_uiState.value.isPiP)
    }

    fun releasePlayer() {
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
    }

    data class PlayerUiState(
        val isPlaying: Boolean = false,
        val title: String = "",
        val currentUrl: String = "",
        val speed: Float = 1.0f,
        val volume: Float = 1.0f,
        val brightness: Float = 1.0f,
        val isFullscreen: Boolean = false,
        val isPiP: Boolean = false,
        val error: String? = null
    )
}
