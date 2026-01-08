# Keep rules for Media3, Room, and Compose (minimal safe set)
-keep class androidx.media3.** { *; }
-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keep class androidx.compose.runtime.** { *; }

# If release build crashes, add keep rules for the reported classes here.
