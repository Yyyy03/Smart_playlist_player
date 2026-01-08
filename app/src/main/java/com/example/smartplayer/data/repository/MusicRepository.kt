package com.example.smartplayer.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.smartplayer.data.db.AppDatabase
import com.example.smartplayer.data.db.PlayEventEntity
import com.example.smartplayer.data.db.TrackEntity
import com.example.smartplayer.smart.RuleEngine
import com.example.smartplayer.smart.Scene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

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

    suspend fun clearLocalMusic() {
        db.trackDao().clearAll()
    }

    suspend fun importFromUris(uris: List<android.net.Uri>) {
        val resolver = context.contentResolver
        val favoriteIds = db.trackDao().getFavoriteIds().toSet()
        val tracks = buildTracksFromUris(resolver, uris, favoriteIds)
        if (tracks.isNotEmpty()) {
            db.trackDao().upsertAll(tracks)
        }
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
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.RELATIVE_PATH
            )

            val supportedMimeTypes = arrayOf(
                "audio/mpeg",
                "audio/flac",
                "audio/aac"
            )

            val selection: String
            val selectionArgs: Array<String>
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                selection = buildString {
                    append("${MediaStore.Audio.Media.IS_MUSIC} = 1")
                    append(" AND ${MediaStore.Audio.Media.MIME_TYPE} IN (?,?,?)")
                    append(" AND (")
                    append("${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?")
                    append(" OR ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?")
                    append(")")
                }
                selectionArgs = arrayOf(
                    supportedMimeTypes[0],
                    supportedMimeTypes[1],
                    supportedMimeTypes[2],
                    "netease/cloudmusic/Music/%",
                    "netease/cloudmusic/Download/%"
                )
            } else {
                @Suppress("DEPRECATION")
                val dataColumn = MediaStore.Audio.Media.DATA
                selection = buildString {
                    append("${MediaStore.Audio.Media.IS_MUSIC} = 1")
                    append(" AND ${MediaStore.Audio.Media.MIME_TYPE} IN (?,?,?)")
                    append(" AND (")
                    append("$dataColumn LIKE ?")
                    append(" OR $dataColumn LIKE ?")
                    append(")")
                }
                selectionArgs = arrayOf(
                    supportedMimeTypes[0],
                    supportedMimeTypes[1],
                    supportedMimeTypes[2],
                    "%/netease/cloudmusic/Music/%",
                    "%/netease/cloudmusic/Download/%"
                )
            }
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val result = mutableListOf<TrackEntity>()
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
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

    private suspend fun buildTracksFromUris(
        resolver: ContentResolver,
        uris: List<android.net.Uri>,
        favoriteIds: Set<Long>
    ): List<TrackEntity> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackEntity>()
        for (uri in uris) {
            val mime = resolver.getType(uri)
            val displayName = queryDisplayName(resolver, uri)
            if (!isSupportedAudio(mime, displayName)) continue

            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val title = retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_TITLE
                ) ?: displayName ?: "Unknown"
                val artist = retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST
                )
                val album = retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM
                )
                val duration = retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L

                val id = generateLocalId(uri)
                tracks += TrackEntity(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    uri = uri.toString(),
                    dateAdded = System.currentTimeMillis(),
                    isFavorite = favoriteIds.contains(id)
                )
            } catch (_: Exception) {
                // Ignore unreadable URIs
            } finally {
                retriever.release()
            }
        }
        tracks
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: android.net.Uri): String? {
        var name: String? = null
        val cursor: Cursor? = resolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) {
                name = it.getString(index)
            }
        }
        return name
    }

    private fun isSupportedAudio(mime: String?, displayName: String?): Boolean {
        val lowerName = displayName?.lowercase(Locale.ROOT) ?: ""
        val extOk = lowerName.endsWith(".mp3") || lowerName.endsWith(".flac") || lowerName.endsWith(".aac")
        val mimeOk = mime == "audio/mpeg" || mime == "audio/flac" || mime == "audio/aac" || mime?.startsWith("audio/") == true
        return extOk || mimeOk
    }

    private fun generateLocalId(uri: android.net.Uri): Long {
        val hash = uri.toString().hashCode()
        val abs = kotlin.math.abs(hash)
        return -abs.toLong()
    }
}
