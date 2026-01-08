package com.example.smartplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.smartplayer.data.db.TrackEntity
import com.example.smartplayer.playback.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class PlayerController(
    context: Context,
    private val onComplete: ((TrackEntity) -> Unit)? = null,
    private val onSkip: ((TrackEntity) -> Unit)? = null
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _controller = MutableStateFlow<MediaController?>(null)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val queue: StateFlow<List<TrackEntity>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val trackMap = mutableMapOf<String, TrackEntity>()
    private var queueCache: List<TrackEntity> = emptyList()
    private var lastCompletedId: Long? = null
    private var suppressSkipOnce = false
    private var tickerJob: Job? = null

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
            updateDurationAndIndex()
            updateCurrentFromQueue()
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
            updateDurationAndIndex()
            updateCurrentFromQueue()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            updateDurationAndIndex()
            updateCurrentFromQueue()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK &&
                oldPosition.mediaItemIndex != newPosition.mediaItemIndex
            ) {
                if (suppressSkipOnce) {
                    suppressSkipOnce = false
                    return
                }
                val controller = _controller.value ?: return
                val oldItem = controller.getMediaItemAt(oldPosition.mediaItemIndex)
                val oldTrack = trackMap[oldItem.mediaId] ?: return
                if (oldPosition.positionMs < 15_000) {
                    onSkip?.invoke(oldTrack)
                }
            }
            updateDurationAndIndex()
            updateCurrentFromQueue()
        }
    }

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        val executor = Executor { command -> command.run() }
        future.addListener(
            {
                val controller = future.get()
                controller.addListener(listener)
                _controller.value = controller
                _isPlaying.value = controller.isPlaying
                updateDurationAndIndex()
                updateCurrentFromQueue()
                startPositionUpdates()
            },
            executor
        )
    }

    private fun startPositionUpdates() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                val controller = _controller.value
                if (controller != null) {
                    _currentPositionMs.value = controller.currentPosition
                }
                delay(500)
            }
        }
    }

    fun hasMedia(): Boolean = (_controller.value?.mediaItemCount ?: 0) > 0

    suspend fun setMedia(track: TrackEntity) {
        val controller = awaitController()
        lastCompletedId = null
        suppressSkipOnce = true
        trackMap.clear()
        trackMap[track.id.toString()] = track
        queueCache = listOf(track)
        _currentTrack.value = track
        _queue.value = listOf(track)
        _currentIndex.value = 0
        controller.setMediaItem(buildMediaItem(track))
        controller.prepare()
    }

    suspend fun setQueueAndPlay(tracks: List<TrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        ensureServiceStarted()
        val controller = awaitController()
        lastCompletedId = null
        suppressSkipOnce = true
        trackMap.clear()
        tracks.forEach { trackMap[it.id.toString()] = it }
        queueCache = tracks
        val items = tracks.map { buildMediaItem(it) }
        controller.setMediaItems(items, startIndex, 0L)
        controller.prepare()
        controller.play()
        _queue.value = tracks
        _currentIndex.value = startIndex
        _currentTrack.value = tracks.getOrNull(startIndex)
    }

    fun isCurrentTrack(track: TrackEntity): Boolean {
        return _currentTrack.value?.id == track.id
    }

    fun currentPositionMs(): Long = _currentPositionMs.value

    suspend fun seekTo(positionMs: Long) {
        awaitController().seekTo(positionMs)
    }

    suspend fun skipToNext() {
        awaitController().seekToNext()
    }

    suspend fun skipToPrevious() {
        awaitController().seekToPrevious()
    }

    suspend fun playFromQueue(index: Int) {
        val controller = awaitController()
        if (index < 0 || index >= controller.mediaItemCount) return
        suppressSkipOnce = true
        controller.seekTo(index, 0L)
        controller.play()
        _currentIndex.value = index
        _currentTrack.value = _queue.value.getOrNull(index)
    }

    suspend fun play() {
        ensureServiceStarted()
        awaitController().play()
    }

    suspend fun pause() {
        awaitController().pause()
    }

    suspend fun togglePlayPause() {
        val controller = awaitController()
        if (controller.isPlaying) {
            controller.pause()
        } else {
            ensureServiceStarted()
            controller.play()
        }
    }

    fun release() {
        _controller.value?.removeListener(listener)
        _controller.value?.release()
        scope.cancel()
    }

    private suspend fun awaitController(): MediaController {
        return _controller.filterNotNull().first()
    }

    private fun updateDurationAndIndex() {
        val controller = _controller.value ?: return
        val duration = if (controller.duration == C.TIME_UNSET) 0L else controller.duration
        _durationMs.value = duration
        _currentIndex.value = controller.currentMediaItemIndex
    }

    private fun updateCurrentFromQueue() {
        val controller = _controller.value ?: return
        val index = controller.currentMediaItemIndex
        _currentIndex.value = index
        _queue.value = queueCache
        _currentTrack.value = queueCache.getOrNull(index)
    }

    private fun ensureServiceStarted() {
        PlaybackService.start(appContext)
    }

    private fun buildMediaItem(track: TrackEntity): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .setMediaMetadata(metadata)
            .build()
    }
}
