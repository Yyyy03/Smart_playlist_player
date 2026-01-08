package com.example.smartplayer.smart

import java.time.Instant
import java.time.ZoneId

object SceneResolver {
    fun resolve(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Scene {
        val localTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalTime()
        val hour = localTime.hour
        return when {
            hour in 5..10 -> Scene.Morning
            hour in 11..17 -> Scene.Commute
            else -> Scene.Night
        }
    }
}
