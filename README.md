# UltraStream Android

Native Android version of UltraStream – a Stremio‑like streaming client with addon support, real‑debrid integration, and a powerful ExoPlayer.

## Features
- **Addon System**: Install Stremio‑compatible addons (manifest.json)
- **Real‑Debrid**: Automatic integration with your API key
- **Smart Playlists**: Auto‑generate episode playlists with working links
- **ExoPlayer**: Supports HLS, DASH, MP4, MKV, MOV, WebM; subtitles (WebVTT, SRT, ASS)
- **Custom Controls**: Play/pause, seek, speed, volume/brightness gestures, fullscreen, PiP
- **Library & Watchlist**: Save your favorites, track progress
- **Search**: Across all installed addons with filter/sort
- **Profiles**: Multi‑user support
- **Backup/Restore**: Export/import your data (addons, library, watchlist, history, progress)

## Build Instructions
### Prerequisites
- Android SDK (API 24+)
- JDK 17
- Gradle 8.4

### Build from source
```bash
git clone https://github.com/YOUR_USERNAME/ultrastream-android.git
cd ultrastream-android
./gradlew assembleDebug
