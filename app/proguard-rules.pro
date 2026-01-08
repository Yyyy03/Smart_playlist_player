# Minimal keep rules for release stability

# Keep app playback service and its inner classes referenced from manifest/notification
-keep class com.example.smartplayer.playback.PlaybackService { *; }
-keep class com.example.smartplayer.playback.PlaybackService$* { *; }

# Keep Room annotations on members to avoid stripping generated access paths
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# If release build crashes, add targeted keep rules for the reported classes here.
