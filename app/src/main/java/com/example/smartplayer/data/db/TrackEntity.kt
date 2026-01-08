package com.example.smartplayer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Long,
    val uri: String,
    val dateAdded: Long
)
