package com.example.smartplayer.smart

import com.example.smartplayer.data.db.TrackEntity
import kotlin.math.min
import kotlin.random.Random

object RuleEngine {
    private const val MAX_QUEUE_SIZE = 30
    private const val FAVORITE_WEIGHT = 50
    private const val PLAY_COUNT_WEIGHT = 5
    private const val PLAY_COUNT_CAP = 40
    private const val PLAYED_24H_WEIGHT = 10
    private const val SKIP_WEIGHT = 20

    fun generateQueue(
        allTracks: List<TrackEntity>,
        playCounts7d: Map<Long, Int>,
        skipCounts7d: Map<Long, Int>,
        playedIn24h: Set<Long>,
        scene: Scene,
        nowMillis: Long
    ): List<TrackEntity> {
        if (allTracks.isEmpty()) return emptyList()

        val scored = allTracks.map { track ->
            val baseScore = scoreTrack(track, playCounts7d, skipCounts7d, playedIn24h)
            track to baseScore
        }

        val sorted = scored.sortedByDescending { it.second }.map { it.first }.toMutableList()
        val queue = sorted.take(MAX_QUEUE_SIZE).toMutableList()

        if (queue.size < MAX_QUEUE_SIZE) {
            val remaining = sorted.drop(queue.size).toMutableList()
            if (remaining.isNotEmpty()) {
                remaining.shuffle(Random(nowMillis))
                val needed = MAX_QUEUE_SIZE - queue.size
                queue.addAll(remaining.take(needed))
            }
        }

        return queue.distinctBy { it.id }.take(MAX_QUEUE_SIZE)
    }

    private fun scoreTrack(
        track: TrackEntity,
        playCounts7d: Map<Long, Int>,
        skipCounts7d: Map<Long, Int>,
        playedIn24h: Set<Long>
    ): Int {
        val favoriteScore = if (track.isFavorite) FAVORITE_WEIGHT else 0
        val playCount = playCounts7d[track.id] ?: 0
        val playScore = min(playCount * PLAY_COUNT_WEIGHT, PLAY_COUNT_CAP)
        val played24hScore = if (playedIn24h.contains(track.id)) PLAYED_24H_WEIGHT else 0
        val skipCount = skipCounts7d[track.id] ?: 0
        val skipScore = skipCount * SKIP_WEIGHT
        return favoriteScore + playScore + played24hScore - skipScore
    }
}
