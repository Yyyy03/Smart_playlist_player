# Smart Playlist Player

Smart Playlist Player is a local music player with an eye toward smart rule-based playlists.
It is built with Jetpack Compose, Media3 ExoPlayer, Room, and an MVVM + Repository architecture.

## Why it feels different
- Fast local scan with a clean, minimal home screen.
- Tap-to-play from the list, with instant pause/resume on the same track.
- Play events logged for future smart playlist rules.

## Features (Current)
- Local media scan via MediaStore.
- Tap-to-play list with current track display.
- Play/pause state synced from Player.Listener.
- Room database for tracks, playlists, cross refs, and play events.
- Smart queue generation from local play history.
- Favorites support for rule weighting.

## Tech Stack
- Kotlin
- Jetpack Compose (Material3)
- AndroidX Media3 ExoPlayer
- Room
- MVVM + Repository

## Permissions
- Android 13+: `READ_MEDIA_AUDIO`
- Android 12 and below: `READ_EXTERNAL_STORAGE`

## Quick Start
1. Open the project in Android Studio.
2. Let Gradle sync finish.
3. Run on a device or emulator (API 24+).
4. Tap "Scan Local Music" to import tracks.
5. Tap a track to play; tap again to pause/resume.

## Architecture
- `PlayerController` owns ExoPlayer and exposes `isPlaying` + `currentTrack` as `StateFlow`.
- `MainViewModel` coordinates scanning, playback, and logs play events.
- `MusicRepository` reads MediaStore and stores results into Room.
- `RuleEngine` scores tracks to build a smart queue.

## Roadmap
- Smart rule playlists (by artist, time, skip rate, etc.)
- Play queue and now-playing screen
- Background playback and notification controls

## Changelog
- 2026-01-08: Smart rules v1 with favorites, skip/complete events, and smart queue playback.
- 2026-01-08: Added click-to-play list, now playing display, scan state, and play action logging.
- 2026-01-08: Initial project skeleton and main playback chain.
