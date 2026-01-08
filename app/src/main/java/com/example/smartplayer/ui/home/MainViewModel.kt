package com.example.smartplayer.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.smartplayer.data.PlayAction
import com.example.smartplayer.data.db.AppDatabase
import com.example.smartplayer.data.db.TrackEntity
import com.example.smartplayer.data.repository.MusicRepository
import com.example.smartplayer.player.PlayerController
import com.example.smartplayer.smart.Scene
import com.example.smartplayer.smart.SceneResolver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "smart_player.db"
    ).addMigrations(AppDatabase.MIGRATION_1_2)
        .build()

    private val repository = MusicRepository(database, application)
    private val playerController = PlayerController(
        application,
        onComplete = { track ->
            viewModelScope.launch {
                repository.logPlayEvent(track.id, PlayAction.COMPLETE)
            }
        },
        onSkip = { track ->
            viewModelScope.launch {
                repository.logPlayEvent(track.id, PlayAction.SKIP)
            }
        }
    )

    val tracks = repository.tracks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val currentTrack: StateFlow<TrackEntity?> = playerController.currentTrack
    val currentPositionMs: StateFlow<Long> = playerController.currentPositionMs
    val durationMs: StateFlow<Long> = playerController.durationMs
    val queue: StateFlow<List<TrackEntity>> = playerController.queue
    val currentIndex: StateFlow<Int> = playerController.currentIndex

    private val _selectedScene = MutableStateFlow(
        SceneResolver.resolve(System.currentTimeMillis())
    )
    val selectedScene: StateFlow<Scene> = _selectedScene.asStateFlow()

    private val _smartQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val smartQueue: StateFlow<List<TrackEntity>> = _smartQueue.asStateFlow()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun scanLocalMusic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            try {
                repository.scanLocalMusic()
                _uiState.value = _uiState.value.copy(isScanning = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = e.message ?: "Scan failed"
                )
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            val track = currentTrack.value ?: tracks.value.firstOrNull()
            if (track == null) {
                return@launch
            }
            if (!playerController.hasMedia()) {
                playerController.setMedia(track)
            }

            if (isPlaying.value) {
                playerController.pause()
                repository.logPlayEvent(track.id, PlayAction.PAUSE)
            } else {
                playerController.play()
                repository.logPlayEvent(track.id, PlayAction.START)
            }
        }
    }

    fun onTrackClicked(track: TrackEntity) {
        viewModelScope.launch {
            val isSameTrack = playerController.isCurrentTrack(track)
            val previous = currentTrack.value
            if (!isSameTrack) {
                if (previous != null && isPlaying.value) {
                    val positionMs = playerController.currentPositionMs()
                    if (positionMs < 15_000) {
                        repository.logPlayEvent(previous.id, PlayAction.SKIP)
                    }
                }
                playerController.setMedia(track)
                playerController.play()
                repository.logPlayEvent(track.id, PlayAction.START)
                return@launch
            }

            if (isPlaying.value) {
                playerController.pause()
                repository.logPlayEvent(track.id, PlayAction.PAUSE)
            } else {
                playerController.play()
                repository.logPlayEvent(track.id, PlayAction.START)
            }
        }
    }

    fun onSceneSelected(scene: Scene) {
        _selectedScene.value = scene
    }

    fun playSmartQueue() {
        viewModelScope.launch {
            val nowMillis = System.currentTimeMillis()
            val queue = repository.generateSmartQueue(selectedScene.value, nowMillis)
            _smartQueue.value = queue
            if (queue.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Smart queue is empty."
                )
                return@launch
            }
            playerController.setQueueAndPlay(queue, 0)
            repository.logPlayEvent(queue[0].id, PlayAction.START)
        }
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            repository.updateFavorite(track.id, !track.isFavorite)
        }
    }

    fun clearLocalMusic() {
        viewModelScope.launch {
            repository.clearLocalMusic()
        }
    }

    fun importFromUris(uris: List<android.net.Uri>) {
        viewModelScope.launch {
            repository.importFromUris(uris)
        }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            playerController.seekTo(positionMs)
        }
    }

    fun skipToNext() {
        viewModelScope.launch {
            playerController.skipToNext()
        }
    }

    fun skipToPrevious() {
        viewModelScope.launch {
            playerController.skipToPrevious()
        }
    }

    fun playFromQueue(index: Int) {
        viewModelScope.launch {
            playerController.playFromQueue(index)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
