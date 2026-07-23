# ExoPlayer
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
# Keep HLS and DASH classes
-keep class androidx.media3.exoplayer.hls.** { *; }
-keep class androidx.media3.exoplayer.dash.** { *; }
# Keep DataSource factories
-keep class androidx.media3.datasource.** { *; }
# Keep native methods
-keepclassmembers class * {
    native <methods>;
}
