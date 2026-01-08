package com.example.smartplayer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartplayer.data.db.PlayEventEntity
import com.example.smartplayer.data.db.TrackCount
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayEventDao {
    @Query("SELECT * FROM play_events ORDER BY playedAt DESC")
    fun getAll(): Flow<List<PlayEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PlayEventEntity)

    @Query(
        "SELECT trackId as trackId, COUNT(*) as count FROM play_events " +
            "WHERE action = 'START' AND playedAt >= :sinceMillis GROUP BY trackId"
    )
    suspend fun getStartCountsSince(sinceMillis: Long): List<TrackCount>

    @Query(
        "SELECT trackId as trackId, COUNT(*) as count FROM play_events " +
            "WHERE action = 'SKIP' AND playedAt >= :sinceMillis GROUP BY trackId"
    )
    suspend fun getSkipCountsSince(sinceMillis: Long): List<TrackCount>

    @Query(
        "SELECT DISTINCT trackId FROM play_events " +
            "WHERE action = 'START' AND playedAt >= :sinceMillis"
    )
    suspend fun getDistinctStartedTrackIdsSince(sinceMillis: Long): List<Long>
}
