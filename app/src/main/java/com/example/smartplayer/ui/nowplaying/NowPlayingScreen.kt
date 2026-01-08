package com.example.smartplayer.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smartplayer.R
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

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FAFC),
            Color(0xFFE2E8F0)
        )
    )

    Column(
        modifier = Modifier
            .background(gradient)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) {
                Text(text = stringResource(R.string.now_playing_back))
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = currentTrack?.title ?: stringResource(R.string.now_playing_title),
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
                if (maxDuration > 0L) {
                    seeking = true
                    seekPosition = (value * maxDuration).toLong()
                }
            },
            onValueChangeFinished = {
                if (maxDuration > 0L) {
                    val target = seekPosition
                    seeking = false
                    viewModel.seekTo(target)
                } else {
                    seeking = false
                }
            },
            enabled = maxDuration > 0L,
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
                Text(text = stringResource(R.string.prev_track))
            }
            Spacer(modifier = Modifier.height(0.dp))
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Text(text = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play))
            }
            IconButton(onClick = { viewModel.skipToNext() }) {
                Text(text = stringResource(R.string.next_track))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.now_playing_up_next),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        val nextQueue = queue.drop(currentIndex + 1).take(10)
        if (nextQueue.isEmpty()) {
            Text(text = stringResource(R.string.now_playing_no_up_next))
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
                            .clickable { viewModel.playFromQueue(targetIndex) },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist ?: stringResource(R.string.unknown_artist),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun trackSubtitle(track: TrackEntity?): String {
    if (track == null) return stringResource(R.string.artist_album_placeholder)
    val artist = track.artist ?: stringResource(R.string.unknown_artist)
    val album = track.album ?: stringResource(R.string.unknown_album)
    return "$artist / $album"
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
