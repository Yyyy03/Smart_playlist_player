package com.example.smartplayer.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.smartplayer.R
import com.example.smartplayer.data.db.TrackEntity
import com.example.smartplayer.smart.Scene

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenNowPlaying: () -> Unit
) {
    val context = LocalContext.current
    val tracks by viewModel.tracks.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedScene by viewModel.selectedScene.collectAsState()
    val smartQueue by viewModel.smartQueue.collectAsState()

    val permission = requiredAudioPermission()
    val notificationPermission = requiredNotificationPermission()
    var permissionDenied by remember { mutableStateOf(false) }
    var notificationDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) {
            viewModel.scanLocalMusic()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationDenied = !granted
    }

    fun requestNotificationThen(action: () -> Unit) {
        if (notificationPermission != null && !hasPermission(context, notificationPermission)) {
            notificationLauncher.launch(notificationPermission)
        }
        action()
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FAFC),
            Color(0xFFE2E8F0)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.Top) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = nowPlayingText(currentTrack),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenNowPlaying() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9E3F5))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.open_now_playing),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.open_now_playing_subtitle),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (hasPermission(context, permission)) {
                        viewModel.scanLocalMusic()
                    } else {
                        launcher.launch(permission)
                    }
                },
                enabled = !uiState.isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.isScanning) {
                        stringResource(R.string.scanning)
                    } else {
                        stringResource(R.string.scan_local_music)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { requestNotificationThen { viewModel.togglePlayPause() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.scene),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sceneButton(
                    label = stringResource(R.string.scene_morning),
                    scene = Scene.Morning,
                    selectedScene = selectedScene,
                    onClick = viewModel::onSceneSelected
                )
                sceneButton(
                    label = stringResource(R.string.scene_commute),
                    scene = Scene.Commute,
                    selectedScene = selectedScene,
                    onClick = viewModel::onSceneSelected
                )
                sceneButton(
                    label = stringResource(R.string.scene_night),
                    scene = Scene.Night,
                    selectedScene = selectedScene,
                    onClick = viewModel::onSceneSelected
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { requestNotificationThen { viewModel.playSmartQueue() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.generate_smart_queue))
            }

            if (permissionDenied) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.permission_denied_audio),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (notificationDenied) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.permission_denied_notification),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.error_prefix) + uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.smart_queue),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (smartQueue.isEmpty()) {
                Text(
                    text = stringResource(R.string.smart_queue_empty),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(smartQueue) { track ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.tracks),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (tracks.isEmpty()) {
                Text(
                    text = stringResource(R.string.tracks_empty),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks) { track ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { requestNotificationThen { viewModel.onTrackClicked(track) } },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (track.isFavorite) {
                                            stringResource(R.string.favorite)
                                        } else {
                                            stringResource(R.string.unfavorite)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .clickable { viewModel.toggleFavorite(track) }
                                    )
                                }
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
}

@Composable
private fun nowPlayingText(track: TrackEntity?): String {
    return if (track == null) {
        stringResource(R.string.now_playing_none)
    } else {
        val artist = track.artist ?: stringResource(R.string.unknown_artist)
        stringResource(
            R.string.now_playing_format,
            track.title,
            artist
        )
    }
}

@Composable
private fun sceneButton(
    label: String,
    scene: Scene,
    selectedScene: Scene,
    onClick: (Scene) -> Unit
) {
    if (scene == selectedScene) {
        Button(onClick = { onClick(scene) }) {
            Text(text = label)
        }
    } else {
        OutlinedButton(onClick = { onClick(scene) }) {
            Text(text = label)
        }
    }
}

private fun requiredAudioPermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun requiredNotificationPermission(): String? {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
