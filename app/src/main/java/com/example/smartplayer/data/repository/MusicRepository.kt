package com.example.smartplayer.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.smartplayer.data.db.AppDatabase
import com.example.smartplayer.data.db.PlayEventEntity
import com.example.smartplayer.data.db.TrackEntity
import com.example.smartplayer.smart.RuleEngine
import com.example.smartplayer.smart.Scene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    val tracks: Flow<List<TrackEntity>> = db.trackDao().getAll()

    suspend fun scanLocalMusic() {
        val resolver = context.contentResolver
        val favoriteIds = db.trackDao().getFavoriteIds().toSet()
        val tracks = queryMediaStore(resolver, favoriteIds)
        db.trackDao().upsertAll(tracks)
    }

    suspend fun logPlayEvent(trackId: Long, action: String) {
        db.playEventDao().insertEvent(
            PlayEventEntity(
                trackId = trackId,
                playedAt = System.currentTimeMillis(),
                action = action
            )
        )
    }

    suspend fun updateFavorite(trackId: Long, isFavorite: Boolean) {
        db.trackDao().updateFavorite(trackId, isFavorite)
    }

    suspend fun generateSmartQueue(scene: Scene, nowMillis: Long): List<TrackEntity> {
        val since7d = nowMillis - 7L * 24 * 60 * 60 * 1000
        val since24h = nowMillis - 24L * 60 * 60 * 1000

        val allTracks = db.trackDao().getAllList()
        if (allTracks.isEmpty()) return emptyList()

        val startCounts = db.playEventDao().getStartCountsSince(since7d)
        val skipCounts = db.playEventDao().getSkipCountsSince(since7d)
        val playedIn24h = db.playEventDao().getDistinctStartedTrackIdsSince(since24h)

        val playCountsMap = startCounts.associate { it.trackId to it.count }
        val skipCountsMap = skipCounts.associate { it.trackId to it.count }

        return RuleEngine.generateQueue(
            allTracks = allTracks,
            playCounts7d = playCountsMap,
            skipCounts7d = skipCountsMap,
            playedIn24h = playedIn24h.toSet(),
            scene = scene,
            nowMillis = nowMillis
        )
    }

    private suspend fun queryMediaStore(
        resolver: ContentResolver,
        favoriteIds: Set<Long>
    ): List<TrackEntity> {
        return withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val result = mutableListOf<TrackEntity>()
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown"
                    val artist = cursor.getString(artistCol)
                    val album = cursor.getString(albumCol)
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    result += TrackEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uri = contentUri.toString(),
                        dateAdded = dateAdded,
                        isFavorite = favoriteIds.contains(id)
                    )
                }
            }
            result
        }
    }
}
