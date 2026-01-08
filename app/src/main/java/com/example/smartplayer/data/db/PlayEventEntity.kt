package com.example.smartplayer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_events")
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedAt: Long,
    val action: String
)
