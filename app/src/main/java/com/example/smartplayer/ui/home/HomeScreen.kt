package com.example.smartplayer.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    var pendingPlayAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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
        if (granted) {
            pendingPlayAction?.invoke()
        }
        pendingPlayAction = null
    }

    fun requestNotificationThen(action: () -> Unit) {
        if (notificationPermission != null && !hasPermission(context, notificationPermission)) {
            pendingPlayAction = action
            notificationLauncher.launch(notificationPermission)
        } else {
            action()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Smart Playlist Player",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = nowPlayingText(currentTrack),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenNowPlaying() }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Open Now Playing",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Full controls and queue preview",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
            Text(text = if (uiState.isScanning) "Scanning..." else "Scan Local Music")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { requestNotificationThen { viewModel.togglePlayPause() } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isPlaying) "Pause" else "Play")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scene",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sceneButton(
                scene = Scene.Morning,
                selectedScene = selectedScene,
                onClick = viewModel::onSceneSelected
            )
            sceneButton(
                scene = Scene.Commute,
                selectedScene = selectedScene,
                onClick = viewModel::onSceneSelected
            )
            sceneButton(
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
            Text(text = "Generate Smart Queue & Play")
        }

        if (permissionDenied) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Permission denied. Please grant access to scan local audio.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (notificationDenied) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Notification permission denied. Playback will continue without notification.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Error: ${uiState.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Smart Queue",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (smartQueue.isEmpty()) {
            Text(
                text = "No smart queue yet. Generate to start.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(smartQueue) { track ->
                    Card(modifier = Modifier.fillMaxWidth()) {
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

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Tracks",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (tracks.isEmpty()) {
            Text(
                text = "No tracks yet. Tap scan to import local music.",
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
                            .clickable { requestNotificationThen { viewModel.onTrackClicked(track) } }
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
                                    text = if (track.isFavorite) "Fav" else "Unfav",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clickable { viewModel.toggleFavorite(track) }
                                )
                            }
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

private fun nowPlayingText(track: TrackEntity?): String {
    return if (track == null) {
        "Now Playing: None"
    } else {
        val artist = track.artist ?: "Unknown Artist"
        "Now Playing: ${track.title} - $artist"
    }
}

@Composable
private fun sceneButton(
    scene: Scene,
    selectedScene: Scene,
    onClick: (Scene) -> Unit
) {
    if (scene == selectedScene) {
        Button(onClick = { onClick(scene) }) {
            Text(text = scene.name)
        }
    } else {
        OutlinedButton(onClick = { onClick(scene) }) {
            Text(text = scene.name)
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
