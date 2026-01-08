package com.example.smartplayer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartplayer.data.db.PlayEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayEventDao {
    @Query("SELECT * FROM play_events ORDER BY playedAt DESC")
    fun getAll(): Flow<List<PlayEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PlayEventEntity)
}
