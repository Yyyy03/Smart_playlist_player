package com.example.smartplayer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.smartplayer.data.db.dao.PlayEventDao
import com.example.smartplayer.data.db.dao.PlaylistDao
import com.example.smartplayer.data.db.dao.TrackDao

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlayEventEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playEventDao(): PlayEventDao
}
