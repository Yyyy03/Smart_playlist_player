package com.example.smartplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.smartplayer.data.db.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerController(context: Context) {
    private val player = ExoPlayer.Builder(context).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
    }

    init {
        player.addListener(listener)
    }

    fun hasMedia(): Boolean = player.mediaItemCount > 0

    fun setMedia(track: TrackEntity) {
        val item = MediaItem.fromUri(track.uri)
        player.setMediaItem(item)
        player.prepare()
    }

    fun togglePlayPause(): Boolean {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        return player.isPlaying
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}
