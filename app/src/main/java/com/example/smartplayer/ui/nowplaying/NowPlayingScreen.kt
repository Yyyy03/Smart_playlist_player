package com.example.smartplayer.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smartplayer.data.db.TrackEntity
import com.example.smartplayer.ui.home.MainViewModel
import kotlin.math.max

@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.currentPositionMs.collectAsState()
    val duration by viewModel.durationMs.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    var seeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0L) }

    val displayPosition = if (seeking) seekPosition else position
    val maxDuration = max(duration, 0L)
    val sliderValue = if (maxDuration == 0L) 0f else displayPosition.toFloat() / maxDuration

    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) {
                Text(text = "Back")
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = currentTrack?.title ?: "No Track",
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = trackSubtitle(currentTrack),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = sliderValue,
            onValueChange = { value ->
                seeking = true
                seekPosition = (value * maxDuration).toLong()
            },
            onValueChangeFinished = {
                val target = seekPosition
                seeking = false
                viewModel.seekTo(target)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(displayPosition))
            Text(text = formatTime(maxDuration))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.skipToPrevious() }) {
                Text(text = "Prev")
            }
            Spacer(modifier = Modifier.height(0.dp))
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Text(text = if (isPlaying) "Pause" else "Play")
            }
            IconButton(onClick = { viewModel.skipToNext() }) {
                Text(text = "Next")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Up Next",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        val nextQueue = queue.drop(currentIndex + 1).take(10)
        if (nextQueue.isEmpty()) {
            Text(text = "No upcoming tracks.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(nextQueue) { index, track ->
                    val targetIndex = currentIndex + 1 + index
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playFromQueue(targetIndex) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun trackSubtitle(track: TrackEntity?): String {
    if (track == null) return "Artist / Album"
    val artist = track.artist ?: "Unknown Artist"
    val album = track.album ?: "Unknown Album"
    return "$artist / $album"
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
