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
    ).build()

    private val repository = MusicRepository(database, application)
    private val playerController = PlayerController(application)

    val tracks = repository.tracks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val currentTrack: StateFlow<TrackEntity?> = playerController.currentTrack

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
            if (!isSameTrack) {
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

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
