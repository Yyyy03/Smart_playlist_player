package com.example.smartplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.smartplayer.ui.home.HomeScreen
import com.example.smartplayer.ui.home.MainViewModel
import com.example.smartplayer.ui.nowplaying.NowPlayingScreen

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    secondary = Color(0xFF155E75),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun SmartPlayerApp(viewModel: MainViewModel) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as androidx.activity.ComponentActivity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(colorScheme = LightColors) {
        var showNowPlaying by remember { mutableStateOf(false) }
        if (showNowPlaying) {
            NowPlayingScreen(
                viewModel = viewModel,
                onBack = { showNowPlaying = false }
            )
        } else {
            HomeScreen(
                viewModel = viewModel,
                onOpenNowPlaying = { showNowPlaying = true }
            )
        }
    }
}
