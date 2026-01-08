package com.example.smartplayer.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.smartplayer.data.db.AppDatabase
import com.example.smartplayer.data.repository.MusicRepository
import com.example.smartplayer.player.PlayerController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun scanLocalMusic() {
        viewModelScope.launch {
            repository.scanLocalMusic()
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            val first = tracks.value.firstOrNull()
            if (first != null && !playerController.hasMedia()) {
                playerController.setMedia(first)
            }
            val isNowPlaying = playerController.togglePlayPause()
            if (first != null) {
                val action = if (isNowPlaying) "play" else "pause"
                repository.logPlayEvent(first.id, action)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
