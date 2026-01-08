package com.example.smartplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.smartplayer.data.db.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerController(
    context: Context,
    private val onComplete: ((TrackEntity) -> Unit)? = null
) {
    private val player = ExoPlayer.Builder(context).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val trackMap = mutableMapOf<String, TrackEntity>()
    private var lastCompletedId: Long? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val track = _currentTrack.value ?: return
                if (lastCompletedId != track.id) {
                    onComplete?.invoke(track)
                    lastCompletedId = track.id
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previous = _currentTrack.value
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && previous != null) {
                if (lastCompletedId != previous.id) {
                    onComplete?.invoke(previous)
                    lastCompletedId = previous.id
                }
            }
            val nextId = mediaItem?.mediaId
            _currentTrack.value = if (nextId != null) trackMap[nextId] else null
        }
    }

    init {
        player.addListener(listener)
    }

    fun hasMedia(): Boolean = player.mediaItemCount > 0

    fun setMedia(track: TrackEntity) {
        lastCompletedId = null
        trackMap.clear()
        trackMap[track.id.toString()] = track
        _currentTrack.value = track
        val item = MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .build()
        player.setMediaItem(item)
        player.prepare()
    }

    fun setQueueAndPlay(tracks: List<TrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        lastCompletedId = null
        trackMap.clear()
        tracks.forEach { trackMap[it.id.toString()] = it }
        val items = tracks.map {
            MediaItem.Builder()
                .setMediaId(it.id.toString())
                .setUri(it.uri)
                .build()
        }
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        player.play()
        _currentTrack.value = tracks.getOrNull(startIndex)
    }

    fun isCurrentTrack(track: TrackEntity): Boolean {
        return _currentTrack.value?.id == track.id
    }

    fun currentPositionMs(): Long = player.currentPosition

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}
