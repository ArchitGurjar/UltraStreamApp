# UltraStream Complete Project Export
Generated on: Thu Jul 23 22:28:08 IST 2026

### File: `./app/src/main/java/com/ultrastream/app/di/DatabaseModule.kt`
```
package com.ultrastream.app.di

import android.content.Context
import androidx.room.Room
import com.ultrastream.app.data.dao.*
import com.ultrastream.app.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "ultrastream.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideAddonDao(db: AppDatabase): AddonDao = db.addonDao()
    @Provides fun provideLibraryDao(db: AppDatabase): LibraryDao = db.libraryDao()
    @Provides fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()
    @Provides fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()
    @Provides fun provideCachedMetaDao(db: AppDatabase): CachedMetaDao = db.cachedMetaDao()
    @Provides fun provideSmartPlaylistDao(db: AppDatabase): SmartPlaylistDao = db.smartPlaylistDao()
    @Provides fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()
    @Provides fun provideWatchProgressDao(db: AppDatabase): WatchProgressDao = db.watchProgressDao()
    @Provides fun provideWatchedEpisodeDao(db: AppDatabase): WatchedEpisodeDao = db.watchedEpisodeDao()
}
```

### File: `./app/src/main/java/com/ultrastream/app/di/NetworkModule.kt`
```
package com.ultrastream.app.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.ultrastream.app.network.StremioApi
import com.ultrastream.app.network.RealDebridApi
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "UltraStream/1.0 (Android)")
                    .header("Referer", "https://ultrastream.app/")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://dummy.base.url/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideStremioApi(retrofit: Retrofit): StremioApi {
        return retrofit.create(StremioApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRealDebridApi(retrofit: Retrofit): RealDebridApi {
        return retrofit.newBuilder()
            .baseUrl("https://api.real-debrid.com/rest/1.0/")
            .build()
            .create(RealDebridApi::class.java)
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/library/LibraryViewModel.kt`
```
package com.ultrastream.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.dao.LibraryDao
import com.ultrastream.app.data.dao.WatchlistDao
import com.ultrastream.app.data.dao.HistoryDao
import com.ultrastream.app.data.models.LibraryItem
import com.ultrastream.app.data.models.WatchlistItem
import com.ultrastream.app.data.models.HistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val watchlistDao: WatchlistDao,
    private val historyDao: HistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibraryData()
    }

    fun loadLibraryData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val library = libraryDao.getAll()
            val watchlist = watchlistDao.getAll()
            val history = historyDao.getAll()
            _uiState.value = _uiState.value.copy(
                library = library,
                watchlist = watchlist,
                history = history,
                isLoading = false
            )
        }
    }

    fun refresh() {
        loadLibraryData()
    }

    data class LibraryUiState(
        val isLoading: Boolean = false,
        val library: List<LibraryItem> = emptyList(),
        val watchlist: List<WatchlistItem> = emptyList(),
        val history: List<HistoryItem> = emptyList()
    )
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/library/LibraryScreen.kt`
```
package com.ultrastream.app.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.ui.components.GridSection
import com.ultrastream.app.ui.components.SectionHeader
import com.ultrastream.app.data.models.MetaItem

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onItemClick: (id: String, type: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // Library
            item {
                SectionHeader(title = "Library")
                if (uiState.library.isEmpty()) {
                    Text("No saved items", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    val metaItems = uiState.library.map { lib ->
                        MetaItem(
                            id = lib.id,
                            type = lib.type,
                            name = lib.name,
                            poster = lib.poster,
                            background = lib.background,
                            imdbRating = lib.imdbRating,
                            year = lib.year,
                            releaseInfo = lib.releaseInfo,
                            released = lib.released,
                            description = lib.description,
                            genre = lib.genre?.split(","),
                            runtime = lib.runtime,
                            cast = lib.cast?.split(","),
                            imdbId = lib.imdbId,
                            videos = null
                        )
                    }
                    GridSection(items = metaItems, onItemClick = onItemClick)
                }
            }

            // Watchlist
            item {
                SectionHeader(title = "Watchlist")
                if (uiState.watchlist.isEmpty()) {
                    Text("No watchlist items", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    val metaItems = uiState.watchlist.map { wl ->
                        MetaItem(
                            id = wl.id,
                            type = wl.type,
                            name = wl.name,
                            poster = wl.poster,
                            background = wl.background,
                            imdbRating = wl.imdbRating,
                            year = wl.year,
                            releaseInfo = wl.releaseInfo,
                            released = wl.released,
                            description = wl.description,
                            genre = wl.genre?.split(","),
                            runtime = wl.runtime,
                            cast = wl.cast?.split(","),
                            imdbId = wl.imdbId,
                            videos = null
                        )
                    }
                    GridSection(items = metaItems, onItemClick = onItemClick)
                }
            }

            // History
            item {
                SectionHeader(title = "History")
                if (uiState.history.isEmpty()) {
                    Text("No history", modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    val metaItems = uiState.history.map { hist ->
                        MetaItem(
                            id = hist.id,
                            type = "movie", // default
                            name = hist.name,
                            poster = hist.poster,
                            background = null,
                            imdbRating = null,
                            year = null,
                            releaseInfo = null,
                            released = null,
                            description = null,
                            genre = null,
                            runtime = null,
                            cast = null,
                            imdbId = null,
                            videos = null
                        )
                    }
                    GridSection(items = metaItems, onItemClick = onItemClick)
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/player/PlayerViewModel.kt`
```
package com.ultrastream.app.ui.screens.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.ultrastream.app.data.models.StreamItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _brightness = MutableStateFlow(1.0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private var playerListener: Player.Listener? = null
    private var positionJob: Job? = null

    fun initializePlayer(context: Context, stream: StreamItem, title: String) {
        viewModelScope.launch {
            try {
                val url = stream.url ?: stream.streamUrl ?: stream.externalUrl
                if (url.isNullOrBlank()) {
                    _error.value = "No valid stream URL"
                    return@launch
                }

                val trackSelector = DefaultTrackSelector(context)
                val exoPlayer = ExoPlayer.Builder(context)
                    .setTrackSelector(trackSelector)
                    .build()

                val dataSourceFactory = createDataSourceFactory()
                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(url)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())

                // FIXED: Attach external subtitles safely using explicit MIME types and Uri.parse
                stream.subtitles?.let { subs ->
                    val configs = subs.mapNotNull { subtitle ->
                        val subUriStr = subtitle.url ?: return@mapNotNull null
                        val mimeType = when {
                            subUriStr.endsWith(".vtt", ignoreCase = true) -> "text/vtt"
                            subUriStr.endsWith(".srt", ignoreCase = true) -> "application/x-subrip"
                            else -> "text/vtt"
                        }
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUriStr))
                            .setMimeType(mimeType)
                            .setLanguage(subtitle.lang ?: "und")
                            .setLabel(subtitle.name ?: subtitle.lang ?: "Subtitle")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                    }
                    if (configs.isNotEmpty()) {
                        mediaItemBuilder.setSubtitleConfigurations(configs)
                    }
                }

                val mediaItem = mediaItemBuilder.build()
                val mediaSource = createMediaSource(mediaItem, dataSourceFactory)
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true

                _player.value = exoPlayer
                _title.value = title

                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _duration.value = exoPlayer.duration
                                _isPlaying.value = exoPlayer.isPlaying
                            }
                            Player.STATE_ENDED -> {
                                _isPlaying.value = false
                            }
                            else -> {}
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _error.value = error.message
                    }
                }
                exoPlayer.addListener(listener)
                playerListener = listener

                positionJob?.cancel()
                positionJob = viewModelScope.launch {
                    while (isActive) {
                        try {
                            _currentPosition.value = exoPlayer.currentPosition
                        } catch (e: IllegalStateException) {
                            break
                        }
                        delay(200)
                    }
                }

            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent("UltraStream/1.0 (Android)")
            .setDefaultRequestProperties(mapOf("Referer" to "https://ultrastream.app/"))
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(60_000)
    }

    private fun createMediaSource(mediaItem: MediaItem, dataSourceFactory: DataSource.Factory): MediaSource {
        val uri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY
        val url = uri.toString()
        return when {
            url.contains(".m3u8") -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            url.contains(".mpd") -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            else -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
    }

    fun playPause() {
        _player.value?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.play()
                _isPlaying.value = true
            }
        }
    }

    fun play() {
        _player.value?.play()
        _isPlaying.value = true
    }

    fun pause() {
        _player.value?.pause()
        _isPlaying.value = false
    }

    fun skipForward(seconds: Long = 10) {
        _player.value?.let { player ->
            val newPos = player.currentPosition + seconds * 1000
            player.seekTo(newPos.coerceAtMost(player.duration))
        }
    }

    fun skipBackward(seconds: Long = 10) {
        _player.value?.let { player ->
            val newPos = player.currentPosition - seconds * 1000
            player.seekTo(newPos.coerceAtLeast(0))
        }
    }

    fun seekTo(position: Long) {
        _player.value?.seekTo(position.coerceIn(0, _duration.value))
    }

    fun setSpeed(speed: Float) {
        _player.value?.setPlaybackSpeed(speed)
        _speed.value = speed
    }

    fun setVolume(volume: Float) {
        _player.value?.volume = volume.coerceIn(0f, 1f)
        _volume.value = volume
    }

    fun setBrightness(brightness: Float) {
        _brightness.value = brightness.coerceIn(0f, 1f)
    }

    fun releasePlayer() {
        positionJob?.cancel()
        positionJob = null
        playerListener?.let { listener ->
            _player.value?.removeListener(listener)
        }
        _player.value?.release()
        _player.value = null
        playerListener = null
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/player/PlayerScreen.kt`
```
package com.ultrastream.app.ui.screens.player

import android.app.Activity
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.ui.PlayerView
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Fullscreen
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    stream: StreamItem,
    title: String = "Now Playing",
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val customColors = LocalCustomColors.current

    // Immersive mode
    DisposableEffect(Unit) {
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }
        insetsController?.let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val player by viewModel.player.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val error by viewModel.error.collectAsState()
    val playerTitle by viewModel.title.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val volume by viewModel.volume.collectAsState()

    LaunchedEffect(stream) {
        viewModel.initializePlayer(context, stream, title)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    // Brightness
    LaunchedEffect(brightness) {
        activity?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness
            window.attributes = layoutParams
        }
    }

    // Volume
    LaunchedEffect(volume) {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVol = (volume * maxVol).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
    }

    // Lifecycle handling for PiP and background
    DisposableEffect(Unit) {
        val listener = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!(activity?.isInPictureInPictureMode ?: false)) {
                        viewModel.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.play()
                }
                else -> {}
            }
        }
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        lifecycle?.addObserver(listener)
        onDispose {
            lifecycle?.removeObserver(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
    ) {
        // PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.player = player
            }
        )

        // Custom controls overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (playerTitle.isNotEmpty()) playerTitle else title,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = { enterPip(activity) }) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = "Picture in Picture",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Progress
            val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatTime(duration),
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { viewModel.skipBackward() }) {
                    Icon(Icons.Default.Replay, contentDescription = "Back 10s", tint = androidx.compose.ui.graphics.Color.White)
                }
                IconButton(onClick = { viewModel.playPause() }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
                IconButton(onClick = { viewModel.skipForward() }) {
                    Icon(Icons.Default.Forward, contentDescription = "Forward 10s", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }

        // Gesture overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { /* could show indicators */ },
                        onDragEnd = { /* hide indicators */ },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val width = size.width
                            val deltaX = dragAmount.x / width
                            if (change.position.x < width / 2) {
                                val newBrightness = (brightness + deltaX).coerceIn(0f, 1f)
                                viewModel.setBrightness(newBrightness)
                            } else {
                                val newVolume = (volume + deltaX).coerceIn(0f, 1f)
                                viewModel.setVolume(newVolume)
                            }
                        }
                    )
                }
        )

        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private fun enterPip(activity: Activity?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        activity?.enterPictureInPictureMode()
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"
    val seconds = millis / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        "%d:%02d:%02d".format(hours, minutes % 60, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/profile/ProfileScreen.kt`
```
package com.ultrastream.app.ui.screens.profile

import kotlinx.coroutines.launch

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val success = viewModel.importData(context, uri)
                if (success) {
                    Toast.makeText(context, "Data imported successfully", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Import failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Theme")
                Switch(
                    checked = uiState.theme == "light",
                    onCheckedChange = {
                        scope.launch { viewModel.toggleTheme() }
                    }
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hindi Priority")
                Switch(
                    checked = uiState.hindiPriority,
                    onCheckedChange = {
                        scope.launch { viewModel.toggleHindiPriority() }
                    }
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto-play Next")
                Switch(
                    checked = uiState.autoPlayNext,
                    onCheckedChange = {
                        scope.launch { viewModel.toggleAutoPlayNext() }
                    }
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Parental Control")
                Switch(
                    checked = uiState.parentalControl,
                    onCheckedChange = {
                        scope.launch { viewModel.toggleParentalControl() }
                    }
                )
            }
        }
        item {
            Text("Profile: ${uiState.currentProfile}", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        val success = viewModel.exportData(context)
                        if (success) {
                            Toast.makeText(context, "Data exported successfully", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Export failed", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Data")
            }
        }
        item {
            Button(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import Data")
            }
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        viewModel.factoryReset(context)
                        Toast.makeText(context, "Factory reset performed", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Factory Reset")
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/home/HomeScreen.kt`
```
package com.ultrastream.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.ui.components.ContinueWatchingCard
import com.ultrastream.app.ui.components.HScrollRow
import com.ultrastream.app.ui.components.PosterCard
import com.ultrastream.app.ui.components.SectionHeader

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onItemClick: (id: String, type: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Continue Watching
        item {
            SectionHeader(title = "Continue Watching")
            if (uiState.continueWatching.isEmpty()) {
                Text("No history yet", modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                HScrollRow {
                    uiState.continueWatching.forEach { (history, progress) ->
                        ContinueWatchingCard(
                            history = history,
                            progressPercent = progress,
                            onClick = { onItemClick(history.id, history.type) }
                        )
                    }
                }
            }
        }

        // Catalog rows
        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            uiState.catalogRows.forEach { (rowId, items) ->
                item {
                    // Generate a human-friendly name from rowId
                    val parts = rowId.split("_")
                    val displayName = when {
                        parts.size >= 3 -> {
                            val type = parts[1]
                            val name = parts.drop(2).joinToString(" ")
                            "$type $name".replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                        else -> "Catalog"
                    }
                    SectionHeader(title = displayName)
                    if (items.isEmpty()) {
                        Text("No items", modifier = Modifier.padding(horizontal = 16.dp))
                    } else {
                        HScrollRow {
                            items.forEach { meta ->
                                PosterCard(
                                    meta = meta,
                                    onClick = { onItemClick(meta.id, meta.type) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/home/HomeViewModel.kt`
```
package com.ultrastream.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.models.Catalog
import com.ultrastream.app.data.models.HistoryItem
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.Video
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import com.ultrastream.app.data.repository.StreamRepository
import com.ultrastream.app.data.dao.HistoryDao
import com.ultrastream.app.data.dao.WatchProgressDao
import com.ultrastream.app.network.StremioApi
import com.ultrastream.app.utils.buildAddonBaseUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val metaRepository: MetaRepository,
    private val streamRepository: StreamRepository,
    private val historyDao: HistoryDao,
    private val watchProgressDao: WatchProgressDao,
    private val stremioApi: StremioApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val catalogListType = Types.newParameterizedType(List::class.java, Catalog::class.java)
    private val catalogAdapter = moshi.adapter<List<Catalog>>(catalogListType)

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val historyItems = historyDao.getAll().take(10)
            val continueWatching = historyItems.mapNotNull { history ->
                val progress = watchProgressDao.getById(history.id)
                if (progress != null && progress.percent > 0) history to progress.percent else null
            }

            val addons = addonRepository.getEnabledAddons()
            val catalogRows = ConcurrentHashMap<String, List<MetaItem>>()

            val fetchJobs = mutableListOf<Deferred<Unit>>()
            for (addon in addons) {
                try {
                    val catalogs = catalogAdapter.fromJson(addon.catalogs) ?: emptyList()
                    val baseUrl = buildAddonBaseUrl(addon.url)

                    for (cat in catalogs) {
                        val rowId = "${addon.id}_${cat.type}_${cat.id}"
                        fetchJobs.add(
                            viewModelScope.async {
                                try {
                                    val url = "$baseUrl/catalog/${cat.type}/${cat.id}.json"
                                    val response = stremioApi.getCatalog(url)
                                    val items = response.metas?.mapNotNull { meta ->
                                        try {
                                            MetaItem(
                                                id = meta.id,
                                                type = meta.type,
                                                name = meta.name,
                                                poster = meta.poster,
                                                background = meta.background,
                                                imdbRating = meta.imdbRating,
                                                year = meta.year,
                                                releaseInfo = meta.releaseInfo,
                                                released = meta.released,
                                                description = meta.description,
                                                genre = meta.genre,
                                                runtime = meta.runtime,
                                                cast = meta.cast,
                                                imdbId = meta.imdb_id,
                                                videos = meta.videos?.map {
                                                    Video(
                                                        season = it.season,
                                                        episode = it.episode,
                                                        name = it.name,
                                                        title = it.title,
                                                        description = it.description,
                                                        thumbnail = it.thumbnail,
                                                        url = it.url
                                                    )
                                                }
                                            )
                                        } catch (e: Exception) { null }
                                    } ?: emptyList()
                                    catalogRows[rowId] = items.take(20)
                                } catch (e: Exception) {
                                    // skip this catalog
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    // skip addon
                }
            }
            fetchJobs.awaitAll()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                continueWatching = continueWatching,
                addons = addons,
                catalogRows = catalogRows.toSortedMap(compareBy { it })
            )
        }
    }

    fun refresh() = loadHomeData()

    data class HomeUiState(
        val isLoading: Boolean = false,
        val addons: List<Addon> = emptyList(),
        val continueWatching: List<Pair<HistoryItem, Int>> = emptyList(),
        val catalogRows: Map<String, List<MetaItem>> = emptyMap()
    )
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/search/SearchViewModel.kt`
```
package com.ultrastream.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.models.Catalog
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.Video
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import com.ultrastream.app.network.StremioApi
import com.ultrastream.app.utils.buildAddonBaseUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val metaRepository: MetaRepository,
    private val stremioApi: StremioApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val catalogListType = Types.newParameterizedType(List::class.java, Catalog::class.java)
    private val catalogAdapter = moshi.adapter<List<Catalog>>(catalogListType)

    fun search(query: String, filter: String = "all", sort: String = "popular") {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = mutableListOf<MetaItem>()

            try {
                val addons = addonRepository.getEnabledAddons()
                val types = when (filter) {
                    "all" -> listOf("movie", "series", "anime", "tv")
                    else -> listOf(filter)
                }

                for (addon in addons) {
                    val baseUrl = buildAddonBaseUrl(addon.url)
                    val catalogs = catalogAdapter.fromJson(addon.catalogs) ?: emptyList()

                    for (type in types) {
                        val searchableCatalog = catalogs.firstOrNull { cat ->
                            cat.type == type && (cat.extraSupported?.contains("search") == true ||
                                cat.extra?.any { it.name == "search" } == true)
                        } ?: continue

                        val encodedQuery = URLEncoder.encode(query, "UTF-8")
                        val searchUrl = "$baseUrl/catalog/$type/${searchableCatalog.id}/search=$encodedQuery.json"
                        try {
                            val response = stremioApi.getCatalog(searchUrl)
                            response.metas?.forEach { meta ->
                                results.add(
                                    MetaItem(
                                        id = meta.id,
                                        type = meta.type,
                                        name = meta.name,
                                        poster = meta.poster,
                                        background = meta.background,
                                        imdbRating = meta.imdbRating,
                                        year = meta.year,
                                        releaseInfo = meta.releaseInfo,
                                        released = meta.released,
                                        description = meta.description,
                                        genre = meta.genre,
                                        runtime = meta.runtime,
                                        cast = meta.cast,
                                        imdbId = meta.imdb_id,
                                        videos = meta.videos?.map {
                                            Video(
                                                season = it.season,
                                                episode = it.episode,
                                                name = it.name,
                                                title = it.title,
                                                description = it.description,
                                                thumbnail = it.thumbnail,
                                                url = it.url
                                            )
                                        }
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // ignore this addon/type combo
                        }
                    }
                }

                val unique = results.distinctBy { it.id }
                val sorted = when (sort) {
                    "rating" -> unique.sortedByDescending { it.imdbRating?.toDoubleOrNull() ?: 0.0 }
                    "year" -> unique.sortedByDescending { it.year?.toIntOrNull() ?: 0 }
                    else -> unique
                }

                _uiState.value = _uiState.value.copy(results = sorted, isSearching = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    results = emptyList(),
                    isSearching = false,
                    error = e.message
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
    }

    data class SearchUiState(
        val isSearching: Boolean = false,
        val results: List<MetaItem> = emptyList(),
        val error: String? = null
    )
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/search/SearchScreen.kt`
```
package com.ultrastream.app.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.ui.components.FilterChipGroup
import com.ultrastream.app.ui.components.PosterCard

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onItemClick: (id: String, type: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var sort by remember { mutableStateOf("popular") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it, filter, sort)
            },
            label = { Text("Search movies, series...") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FilterChipGroup(
            chips = listOf("All", "Movie", "Series", "Anime", "TV"),
            selected = filter,
            onSelect = {
                filter = it.lowercase()
                viewModel.search(query, filter, sort)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        FilterChipGroup(
            chips = listOf("Popular", "Rating", "Year"),
            selected = sort,
            onSelect = {
                sort = it.lowercase()
                viewModel.search(query, filter, sort)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.results.size) { index ->
                    val item = uiState.results[index]
                    PosterCard(
                        meta = item,
                        onClick = { onItemClick(item.id, item.type) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/details/DetailsViewModel.kt`
```
package com.ultrastream.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.dao.*
import com.ultrastream.app.data.models.*
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import com.ultrastream.app.data.repository.StreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val metaRepository: MetaRepository,
    private val streamRepository: StreamRepository,
    private val addonRepository: AddonRepository,
    private val preferencesManager: PreferencesManager,
    private val watchProgressDao: WatchProgressDao,
    private val watchedEpisodeDao: WatchedEpisodeDao,
    private val libraryDao: LibraryDao,
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var currentSeason: Int? = null
    private var currentEpisode: Int? = null

    fun loadMeta(id: String, type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val meta = metaRepository.getMeta(id, type)
            if (meta != null) {
                val inLibrary = libraryDao.getById(id) != null
                val inWatchlist = watchlistDao.getById(id) != null
                val progress = watchProgressDao.getById(id)
                _uiState.value = _uiState.value.copy(
                    meta = meta,
                    inLibrary = inLibrary,
                    inWatchlist = inWatchlist,
                    watchProgress = progress,
                    isLoading = false,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Meta not found"
                )
            }
        }
    }

    fun selectSeason(season: Int) {
        currentSeason = season
        currentEpisode = null
        _uiState.value = _uiState.value.copy(selectedSeason = season, selectedEpisode = null)
        loadStreamsForCurrentSelection()
    }

    fun selectEpisode(episode: Int) {
        currentEpisode = episode
        _uiState.value = _uiState.value.copy(selectedEpisode = episode)
        loadStreamsForCurrentSelection()
    }

    private fun loadStreamsForCurrentSelection() {
        val meta = _uiState.value.meta ?: return
        loadStreams(meta.id, meta.type, currentSeason, currentEpisode)
    }

    fun loadStreams(id: String, type: String, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(streamsLoading = true, streams = emptyList())
            try {
                val addons = addonRepository.getEnabledAddons()
                val addonUrls = addons.map { it.url }
                val hindiPriority = preferencesManager.getHindiPriority().first()
                val debridKey = preferencesManager.getDebridKey().first()

                val streams = streamRepository.getStreams(
                    metaId = id,
                    metaType = type,
                    season = season,
                    episode = episode,
                    addonUrls = addonUrls,
                    hindiPriority = hindiPriority,
                    debridKey = if (debridKey.isNotBlank()) debridKey else null
                )

                _uiState.value = _uiState.value.copy(
                    streams = streams,
                    streamsLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    streams = emptyList(),
                    streamsLoading = false,
                    error = e.message ?: "Failed to load streams"
                )
            }
        }
    }

    fun toggleLibrary(meta: MetaItem) {
        viewModelScope.launch {
            val current = _uiState.value.inLibrary
            if (current) {
                libraryDao.delete(libraryDao.getById(meta.id) ?: return@launch)
                _uiState.value = _uiState.value.copy(inLibrary = false)
            } else {
                libraryDao.insert(meta.toLibraryItem())
                _uiState.value = _uiState.value.copy(inLibrary = true)
            }
        }
    }

    fun toggleWatchlist(meta: MetaItem) {
        viewModelScope.launch {
            val current = _uiState.value.inWatchlist
            if (current) {
                watchlistDao.delete(watchlistDao.getById(meta.id) ?: return@launch)
                _uiState.value = _uiState.value.copy(inWatchlist = false)
            } else {
                watchlistDao.insert(meta.toWatchlistItem())
                _uiState.value = _uiState.value.copy(inWatchlist = true)
            }
        }
    }

    fun playStream(stream: StreamItem, title: String, onResolved: (StreamItem, String) -> Unit) {
        viewModelScope.launch {
            val debridKey = preferencesManager.getDebridKey().first()
            val resolved = streamRepository.resolveStream(stream, debridKey.takeIf { it.isNotBlank() })
            onResolved(resolved, title)
        }
    }

    data class DetailsUiState(
        val isLoading: Boolean = false,
        val meta: MetaItem? = null,
        val inLibrary: Boolean = false,
        val inWatchlist: Boolean = false,
        val watchProgress: WatchProgress? = null,
        val error: String? = null,
        val streams: List<StreamItem> = emptyList(),
        val streamsLoading: Boolean = false,
        val selectedSeason: Int? = null,
        val selectedEpisode: Int? = null
    )
}

fun MetaItem.toLibraryItem() = LibraryItem(
    id = id,
    type = type,
    name = name,
    poster = poster,
    background = background,
    imdbRating = imdbRating,
    year = year,
    releaseInfo = releaseInfo,
    released = released,
    description = description,
    genre = genre?.joinToString(","),
    runtime = runtime,
    cast = cast?.joinToString(","),
    imdbId = imdbId,
    timestamp = System.currentTimeMillis()
)

fun MetaItem.toWatchlistItem() = WatchlistItem(
    id = id,
    type = type,
    name = name,
    poster = poster,
    background = background,
    imdbRating = imdbRating,
    year = year,
    releaseInfo = releaseInfo,
    released = released,
    description = description,
    genre = genre?.joinToString(","),
    runtime = runtime,
    cast = cast?.joinToString(","),
    imdbId = imdbId,
    timestamp = System.currentTimeMillis()
)
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/addons/AddonsViewModel.kt`
```
package com.ultrastream.app.ui.screens.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.data.repository.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddonsViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddonsUiState())
    val uiState: StateFlow<AddonsUiState> = _uiState.asStateFlow()

    init {
        loadAddons()
        observeDebridKey()
    }

    fun loadAddons() {
        viewModelScope.launch {
            val addons = addonRepository.getAllAddons()
            _uiState.value = _uiState.value.copy(addons = addons)
        }
    }

    private fun observeDebridKey() {
        viewModelScope.launch {
            preferencesManager.getDebridKey().collect { key ->
                _uiState.value = _uiState.value.copy(debridKey = key)
            }
        }
    }

    suspend fun installAddon(url: String): Boolean {
        val addon = addonRepository.installAddon(url)
        if (addon != null) {
            loadAddons()
            return true
        }
        return false
    }

    suspend fun toggleAddon(id: String, enabled: Boolean) {
        addonRepository.toggleAddon(id, enabled)
        loadAddons()
    }

    suspend fun removeAddon(id: String) {
        addonRepository.removeAddon(id)
        loadAddons()
    }

    suspend fun saveDebridKey(key: String) {
        preferencesManager.setDebridKey(key)
        _uiState.value = _uiState.value.copy(debridKey = key)
    }

    data class AddonsUiState(
        val addons: List<Addon> = emptyList(),
        val debridKey: String = ""
    )
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/screens/addons/AddonsScreen.kt`
```
package com.ultrastream.app.ui.screens.addons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun AddonsScreen(
    viewModel: AddonsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // FIXED: Added proper context for UI Feedback
    var addonUrl by remember { mutableStateOf("") }
    var debridKey by remember { mutableStateOf(uiState.debridKey) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Addons", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            OutlinedTextField(
                value = addonUrl,
                onValueChange = { addonUrl = it },
                label = { Text("Manifest URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        // FIXED: Added proper UI Feedback without deleting any logic
                        val success = viewModel.installAddon(addonUrl)
                        if (success) {
                            addonUrl = ""
                            Toast.makeText(context, "✅ Addon Installed Successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "❌ Install Failed: Invalid URL or Parsing Error.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Install Addon")
            }
        }
        item {
            Text("Real-Debrid Key", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = debridKey,
                onValueChange = { debridKey = it },
                label = { Text("Debrid API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveDebridKey(debridKey)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Debrid Key")
            }
        }
        item {
            Text("Installed Addons", style = MaterialTheme.typography.titleMedium)
        }
        items(uiState.addons.size) { index ->
            val addon = uiState.addons[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(addon.name, style = MaterialTheme.typography.titleSmall)
                        Text(addon.url, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        Switch(
                            checked = addon.enabled,
                            onCheckedChange = {
                                scope.launch {
                                    viewModel.toggleAddon(addon.id, it)
                                }
                            }
                        )
                        if (!addon.required) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.removeAddon(addon.id)
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/navigation/NavRoutes.kt`
```
package com.ultrastream.app.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Search : Screen("search")
    object Addons : Screen("addons")
    object Profile : Screen("profile")
    object Details : Screen("details/{id}/{type}") {
        fun pass(id: String, type: String) =
            "details/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(type, "UTF-8")}"
    }
    object Player : Screen("player/{url}") {
        fun pass(url: String) = "player/${URLEncoder.encode(url, "UTF-8")}"
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/theme/Theme.kt`
```
package com.ultrastream.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextMainDark,
    secondary = AccentPurple,
    onSecondary = TextMainDark,
    tertiary = AccentGold,
    onTertiary = TextMainDark,
    background = BackgroundDark,
    onBackground = TextMainDark,
    surface = SurfaceDark,
    onSurface = TextMainDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextMutedDark,
    error = AccentRed,
    onError = TextMainDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = TextMainLight,
    secondary = AccentPurple,
    onSecondary = TextMainLight,
    tertiary = AccentGold,
    onTertiary = TextMainLight,
    background = BackgroundLight,
    onBackground = TextMainLight,
    surface = SurfaceLight,
    onSurface = TextMainLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextMutedLight,
    error = AccentRed,
    onError = TextMainLight
)

// Local composition for custom colors if needed
val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

data class CustomColors(
    val accentBlue: androidx.compose.ui.graphics.Color = AccentBlue,
    val accentGold: androidx.compose.ui.graphics.Color = AccentGold,
    val accentRed: androidx.compose.ui.graphics.Color = AccentRed,
    val accentGreen: androidx.compose.ui.graphics.Color = AccentGreen,
    val accentPurple: androidx.compose.ui.graphics.Color = AccentPurple,
    val accentPink: androidx.compose.ui.graphics.Color = AccentPink,
    val accentOrange: androidx.compose.ui.graphics.Color = AccentOrange,
    val textMuted: androidx.compose.ui.graphics.Color = TextMutedDark
)

@Composable
fun UltraStreamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) {
        CustomColors(
            textMuted = TextMutedDark
        )
    } else {
        CustomColors(
            textMuted = TextMutedLight
        )
    }

    CompositionLocalProvider(
        LocalCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/theme/Color.kt`
```
package com.ultrastream.app.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme colors (matching web CSS variables)
val BackgroundDark = Color(0xFF060606)
val SurfaceDark = Color(0xFF121212)
val CardDark = Color(0xFF1A1A1A)
val TextMainDark = Color(0xFFFFFFFF)
val TextMutedDark = Color(0xFFA3A3A3)
val AccentBlue = Color(0xFF38BDF8)
val AccentGold = Color(0xFFFBBF24)
val AccentRed = Color(0xFFEF4444)
val AccentGreen = Color(0xFF4CAF50)
val AccentPurple = Color(0xFFA78BFA)
val AccentPink = Color(0xFFF472B6)
val AccentOrange = Color(0xFFFB923C)

// Light theme colors
val BackgroundLight = Color(0xFFF3F4F6)
val SurfaceLight = Color(0xFFFFFFFF)
val CardLight = Color(0xFFFFFFFF)
val TextMainLight = Color(0xFF111827)
val TextMutedLight = Color(0xFF6B7280)

// Default Material colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

### File: `./app/src/main/java/com/ultrastream/app/ui/theme/Type.kt`
```
package com.ultrastream.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Use system fonts for now (Nunito can be added later)
val AppFontFamily = FontFamily.Default

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)
```

### File: `./app/src/main/java/com/ultrastream/app/ui/theme/Shape.kt`
```
package com.ultrastream.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
```

### File: `./app/src/main/java/com/ultrastream/app/ui/components/FilterChipGroup.kt`
```
package com.ultrastream.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilterChipGroup(
    chips: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { chip ->
            val isSelected = chip.lowercase() == selected.lowercase()
            AssistChip(
                onClick = { onSelect(chip) },
                label = { Text(chip) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/components/bottomsheets/SubtitlesSheet.kt`
```
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components.bottomsheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultrastream.app.data.models.Subtitle

@Composable
fun SubtitlesSheet(
    subtitles: List<Subtitle>,
    onDismiss: () -> Unit,
    onSubtitleSelected: (Subtitle) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Subtitles", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(subtitles.size) { index ->
                    val sub = subtitles[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onSubtitleSelected(sub) }
                    ) {
                        Text(
                            text = sub.lang ?: "Unknown",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/components/bottomsheets/StreamsSheet.kt`
```
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components.bottomsheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultrastream.app.data.models.StreamItem

@Composable
fun StreamsSheet(
    streams: List<StreamItem>,
    onDismiss: () -> Unit,
    onStreamClick: (StreamItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Available Streams", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(streams.size) { index ->
                    val stream = streams[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onStreamClick(stream) }
                    ) {
                        Text(
                            text = stream.title ?: stream.name ?: "Stream",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/components/bottomsheets/SeasonsSheet.kt`
```
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components.bottomsheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SeasonsSheet(
    seasons: List<Int>,
    currentSeason: Int,
    onDismiss: () -> Unit,
    onSeasonSelected: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Select Season", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(seasons.size) { index ->
                    val season = seasons[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onSeasonSelected(season) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (season == currentSeason) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = "Season $season",
                            modifier = Modifier.padding(16.dp),
                            color = if (season == currentSeason) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/components/PosterCard.kt`
```
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ultrastream.app.data.models.MetaItem

@Composable
fun PosterCard(
    meta: MetaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    progressPercent: Int = 0
) {
    val posterUrl = meta.poster ?: meta.background ?: ""
    Card(
        modifier = modifier
            .width(130.dp)
            .height(195.dp)
            .clip(RoundedCornerShape(12.dp)),
        onClick = onClick
    ) {
        Box {
            AsyncImage(
                model = posterUrl,
                contentDescription = meta.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Gradient overlay for title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                Text(
                    text = meta.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (showProgress && progressPercent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Gray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercent / 100f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/components/SectionHeader.kt`
```
package com.ultrastream.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
            )
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/components/ContinueWatchingCard.kt`
```
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ultrastream.app.data.models.HistoryItem

@Composable
fun ContinueWatchingCard(
    history: HistoryItem,
    progressPercent: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp)),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = history.poster,
                contentDescription = history.name,
                modifier = Modifier
                    .width(75.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = history.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = history.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (progressPercent > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 4.dp)
                            .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressPercent / 100f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/components/GridSection.kt`
```
package com.ultrastream.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultrastream.app.data.models.MetaItem

@Composable
fun GridSection(
    items: List<MetaItem>,
    onItemClick: (id: String, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val chunkedItems = items.chunked(3)
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    PosterCard(
                        meta = item,
                        onClick = { onItemClick(item.id, item.type) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty spaces to keep consistent grid width
                val emptySpaces = 3 - rowItems.size
                repeat(emptySpaces) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/ui/components/HScrollRow.kt`
```
package com.ultrastream.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HScrollRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/network/RealDebridApi.kt`
```
package com.ultrastream.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface RealDebridApi {
    @GET("torrents/instantAvailability")
    suspend fun checkInstantAvailability(
        @Header("Authorization") auth: String,
        @Query("hash") hash: String
    ): Map<String, Map<String, List<String>>>

    @GET("torrents/addMagnet")
    suspend fun addMagnet(
        @Header("Authorization") auth: String,
        @Query("magnet") magnet: String
    ): AddTorrentResponse

    @GET("torrents/selectFiles")
    suspend fun selectFiles(
        @Header("Authorization") auth: String,
        @Query("id") torrentId: String,
        @Query("files") files: String = "all"
    ): String

    @GET("torrents/status")
    suspend fun getTorrentStatus(
        @Header("Authorization") auth: String,
        @Query("id") torrentId: String
    ): TorrentStatus

    @GET("torrents/unrestrictLink")
    suspend fun unrestrictLink(
        @Header("Authorization") auth: String,
        @Query("link") link: String
    ): UnrestrictedLink
}

data class AddTorrentResponse(val id: String, val uri: String)
data class TorrentStatus(val id: String, val status: String, val links: List<String>)
data class UnrestrictedLink(val link: String)
```

### File: `./app/src/main/java/com/ultrastream/app/network/StremioApi.kt`
```
package com.ultrastream.app.network

import retrofit2.http.GET
import retrofit2.http.Url

interface StremioApi {
    @GET
    suspend fun getManifest(@Url url: String): ManifestResponse

    @GET
    suspend fun getCatalog(@Url url: String): CatalogResponse

    @GET
    suspend fun getMeta(@Url url: String): MetaResponse

    @GET
    suspend fun getStreams(@Url url: String): StreamResponse
}

data class ManifestResponse(
    val id: String,
    val name: String,
    val catalogs: List<Catalog>?,
    val resources: List<String>?,
    val types: List<String>?,
    val version: String?
)

data class Catalog(
    val type: String,
    val id: String,
    val name: String,
    val extraSupported: List<String>? = null,
    val extra: List<Extra>? = null
)

data class Extra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String>? = null
)

data class CatalogResponse(
    val metas: List<Meta>? = emptyList()
)

data class MetaResponse(
    val meta: Meta?
)

data class Meta(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: List<String>?,
    val runtime: String?,
    val cast: List<String>?,
    val imdb_id: String?,
    val videos: List<Video>? = null
)

data class Video(
    val season: Int?,
    val episode: Int?,
    val name: String?,
    val title: String?,
    val description: String?,
    val thumbnail: String?,
    val url: String?
)

data class StreamResponse(
    val streams: List<Stream>? = emptyList()
)

data class Stream(
    val url: String?,
    val streamUrl: String?,
    val externalUrl: String?,
    val title: String?,
    val name: String?,
    val description: String?,
    val infoHash: String?,
    val subtitles: List<StreamSubtitle>?
)

data class StreamSubtitle(
    val url: String?,
    val file: String?,
    val lang: String?,
    val name: String?
)
```

### File: `./app/src/main/java/com/ultrastream/app/data/database/AppDatabase.kt`
```
package com.ultrastream.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ultrastream.app.data.dao.*
import com.ultrastream.app.data.models.*

@Database(
    entities = [
        Addon::class,
        LibraryItem::class,
        WatchlistItem::class,
        HistoryItem::class,
        CachedMeta::class,
        SmartPlaylist::class,
        Profile::class,
        WatchProgress::class,
        WatchedEpisode::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun addonDao(): AddonDao
    abstract fun libraryDao(): LibraryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun historyDao(): HistoryDao
    abstract fun cachedMetaDao(): CachedMetaDao
    abstract fun smartPlaylistDao(): SmartPlaylistDao
    abstract fun profileDao(): ProfileDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/database/Converters.kt`
```
package com.ultrastream.app.data.database

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.models.*

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromCatalogList(value: List<Catalog>): String {
        val type = Types.newParameterizedType(List::class.java, Catalog::class.java)
        val adapter = moshi.adapter<List<Catalog>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toCatalogList(value: String): List<Catalog> {
        val type = Types.newParameterizedType(List::class.java, Catalog::class.java)
        val adapter = moshi.adapter<List<Catalog>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromEpisodeList(value: List<PlaylistEpisode>): String {
        val type = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
        val adapter = moshi.adapter<List<PlaylistEpisode>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toEpisodeList(value: String): List<PlaylistEpisode> {
        val type = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
        val adapter = moshi.adapter<List<PlaylistEpisode>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStreamItem(value: StreamItem?): String? {
        if (value == null) return null
        val adapter = moshi.adapter(StreamItem::class.java)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStreamItem(value: String?): StreamItem? {
        if (value == null) return null
        val adapter = moshi.adapter(StreamItem::class.java)
        return adapter.fromJson(value)
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/dao/SmartPlaylistDao.kt`
```
package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.SmartPlaylist

@Dao
interface SmartPlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: SmartPlaylist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playlists: List<SmartPlaylist>)

    @Query("SELECT * FROM smart_playlists")
    suspend fun getAll(): List<SmartPlaylist>

    @Query("SELECT * FROM smart_playlists WHERE id = :id")
    suspend fun getById(id: String): SmartPlaylist?

    @Delete
    suspend fun delete(playlist: SmartPlaylist)

    @Query("DELETE FROM smart_playlists")
    suspend fun deleteAll()
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/dao/LibraryDao.kt`
```
package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.LibraryItem

@Dao
interface LibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LibraryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LibraryItem>)

    @Query("SELECT * FROM library")
    suspend fun getAll(): List<LibraryItem>

    @Query("SELECT * FROM library WHERE id = :id")
    suspend fun getById(id: String): LibraryItem?

    @Delete
    suspend fun delete(item: LibraryItem)

    @Query("DELETE FROM library")
    suspend fun deleteAll()
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/dao/WatchProgressDao.kt`
```
package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultrastream.app.data.models.WatchProgress
import com.ultrastream.app.data.models.WatchedEpisode

@Dao
interface WatchProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: WatchProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progressList: List<WatchProgress>)

    @Query("SELECT * FROM watch_progress WHERE id = :id")
    suspend fun getById(id: String): WatchProgress?

    @Query("SELECT * FROM watch_progress")
    suspend fun getAll(): List<WatchProgress>

    @Delete
    suspend fun delete(progress: WatchProgress)

    @Query("DELETE FROM watch_progress")
    suspend fun deleteAll()
}

@Dao
interface WatchedEpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ep: WatchedEpisode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eps: List<WatchedEpisode>)

    @Query("SELECT * FROM watched_episodes WHERE episodeKey = :key")
    suspend fun getByKey(key: String): WatchedEpisode?

    @Query("SELECT * FROM watched_episodes")
    suspend fun getAll(): List<WatchedEpisode>

    @Delete
    suspend fun delete(ep: WatchedEpisode)

    @Query("DELETE FROM watched_episodes")
    suspend fun deleteAll()
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/dao/AddonDao.kt`
```
package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultrastream.app.data.models.Addon

@Dao
interface AddonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(addon: Addon)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addons: List<Addon>)

    @Query("SELECT * FROM addons")
    suspend fun getAll(): List<Addon>

    @Query("SELECT * FROM addons WHERE id = :id")
    suspend fun getById(id: String): Addon?

    @Query("DELETE FROM addons WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE addons SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM addons")
    suspend fun deleteAll()
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/dao/ProfileDao.kt`
```
package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.Profile

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<Profile>)

    @Query("SELECT * FROM profiles")
    suspend fun getAll(): List<Profile>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): Profile?

    @Delete
    suspend fun delete(profile: Profile)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/dao/WatchlistDao.kt`
```
package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.WatchlistItem

@Dao
interface WatchlistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WatchlistItem>)

    @Query("SELECT * FROM watchlist")
    suspend fun getAll(): List<WatchlistItem>

    @Query("SELECT * FROM watchlist WHERE id = :id")
    suspend fun getById(id: String): WatchlistItem?

    @Delete
    suspend fun delete(item: WatchlistItem)

    @Query("DELETE FROM watchlist")
    suspend fun deleteAll()
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/dao/HistoryDao.kt`
```
package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.ultrastream.app.data.models.HistoryItem

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HistoryItem>)

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryItem>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getById(id: String): HistoryItem?

    @Delete
    suspend fun delete(item: HistoryItem)

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/dao/CachedMetaDao.kt`
```
package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultrastream.app.data.models.CachedMeta

@Dao
interface CachedMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meta: CachedMeta)

    @Query("SELECT * FROM cached_meta WHERE cacheKey = :key")
    suspend fun getByKey(key: String): CachedMeta?

    @Query("DELETE FROM cached_meta")
    suspend fun deleteAll()

    @androidx.room.Query("SELECT * FROM cached_meta")
    suspend fun getAll(): List<CachedMeta>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(metas: List<CachedMeta>)
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/models/WatchProgress.kt`
```
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgress(
    @PrimaryKey val id: String, // can be metaId or episode key
    val percent: Int,
    val lastUpdate: Long = System.currentTimeMillis()
)

@Entity(tableName = "watched_episodes")
data class WatchedEpisode(
    @PrimaryKey val episodeKey: String, // "metaId_sX_eY"
    val watched: Boolean = true
)
```

### File: `./app/src/main/java/com/ultrastream/app/data/models/Addon.kt`
```
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addons")
data class Addon(
    @PrimaryKey val id: String,
    val url: String,
    val name: String,
    val catalogs: String, // JSON string of List<Catalog>
    val enabled: Boolean = true,
    val required: Boolean = false
)

data class Catalog(
    val type: String, // movie, series, anime, tv
    val id: String,
    val name: String,
    val extraSupported: List<String>? = null,
    val extra: List<Extra>? = null
)

data class Extra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String>? = null
)
```

### File: `./app/src/main/java/com/ultrastream/app/data/models/Episode.kt`
```
package com.ultrastream.app.data.models

data class Episode(
    val season: Int,
    val episode: Int,
    val name: String?,
    val title: String?,
    val description: String?,
    val thumbnail: String?
)
```

### File: `./app/src/main/java/com/ultrastream/app/data/models/SmartPlaylist.kt`
```
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_playlists")
data class SmartPlaylist(
    @PrimaryKey val id: String,
    val metaId: String,
    val metaName: String,
    val poster: String?,
    val season: Int,
    val addon: String,
    val total: Int,
    val fetched: Int,
    val status: String,
    val episodesJson: String // JSON string of List<PlaylistEpisode>
)

data class PlaylistEpisode(
    val epNum: Int,
    val epName: String,
    val title: String,
    val stream: StreamItem?,
    val isMissing: Boolean = false
)
```

### File: `./app/src/main/java/com/ultrastream/app/data/models/MetaItem.kt`
```
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library")
data class LibraryItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: String?,
    val runtime: String?,
    val cast: String?, // JSON string of List<String>
    val imdbId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: String?,
    val runtime: String?,
    val cast: String?,
    val imdbId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_meta")
data class CachedMeta(
    @PrimaryKey val cacheKey: String, // e.g., "id_type"
    val json: String // full meta JSON
)

data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val imdbRating: String?,
    val year: String?,
    val releaseInfo: String?,
    val released: String?,
    val description: String?,
    val genre: List<String>?,
    val runtime: String?,
    val cast: List<String>?,
    val imdbId: String?,
    val videos: List<Video>? = null
)

data class Video(
    val season: Int?,
    val episode: Int?,
    val name: String?,
    val title: String?,
    val description: String?,
    val thumbnail: String?,
    val url: String? // trailer url etc.
)
```

### File: `./app/src/main/java/com/ultrastream/app/data/models/StreamItem.kt`
```
package com.ultrastream.app.data.models

data class StreamItem(
    val url: String?,
    val streamUrl: String?,
    val externalUrl: String?,
    val title: String?,
    val name: String?,
    val description: String?,
    val infoHash: String?,
    val addonName: String?,
    val subtitles: List<Subtitle>?,
    val isLive: Boolean = false
)

data class Subtitle(
    val url: String?,
    val file: String?,
    val lang: String?,
    val name: String?
)

data class StreamResponse(
    val streams: List<StreamItem>
)
```

### File: `./app/src/main/java/com/ultrastream/app/data/models/Profile.kt`
```
package com.ultrastream.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String
)
```

### File: `./app/src/main/java/com/ultrastream/app/data/preferences/PreferencesManager.kt`
```
package com.ultrastream.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val DEBRID_KEY = stringPreferencesKey("debrid_key")
        val CURRENT_PROFILE_KEY = stringPreferencesKey("current_profile")
        val HINDI_PRIORITY_KEY = booleanPreferencesKey("hindi_priority")
        val AUTO_PLAY_NEXT_KEY = booleanPreferencesKey("auto_play_next")
        val PARENTAL_CONTROL_KEY = booleanPreferencesKey("parental_control")
        val PARENTAL_RATING_KEY = stringPreferencesKey("parental_rating")
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    fun getTheme(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[THEME_KEY] ?: "dark"
        }
    }

    suspend fun setDebridKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[DEBRID_KEY] = key
        }
    }

    fun getDebridKey(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[DEBRID_KEY] ?: ""
        }
    }

    suspend fun setCurrentProfile(profileId: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_PROFILE_KEY] = profileId
        }
    }

    fun getCurrentProfile(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CURRENT_PROFILE_KEY] ?: "default"
        }
    }

    suspend fun setHindiPriority(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HINDI_PRIORITY_KEY] = enabled
        }
    }

    fun getHindiPriority(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[HINDI_PRIORITY_KEY] ?: true
        }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_NEXT_KEY] = enabled
        }
    }

    fun getAutoPlayNext(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_PLAY_NEXT_KEY] ?: false
        }
    }

    suspend fun setParentalControl(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PARENTAL_CONTROL_KEY] = enabled
        }
    }

    fun getParentalControl(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PARENTAL_CONTROL_KEY] ?: false
        }
    }

    suspend fun setParentalRating(rating: String) {
        context.dataStore.edit { preferences ->
            preferences[PARENTAL_RATING_KEY] = rating
        }
    }

    fun getParentalRating(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PARENTAL_RATING_KEY] ?: "PG-13"
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/repository/MetaRepository.kt`
```
package com.ultrastream.app.data.repository

import com.ultrastream.app.data.dao.CachedMetaDao
import com.ultrastream.app.data.models.CachedMeta
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.Video
import com.ultrastream.app.network.Meta
import com.ultrastream.app.network.StremioApi
import com.ultrastream.app.utils.buildAddonBaseUrl
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetaRepository @Inject constructor(
    private val cachedMetaDao: CachedMetaDao,
    private val addonRepository: AddonRepository,
    private val stremioApi: StremioApi,
    private val moshi: Moshi
) {

    suspend fun getMeta(id: String, type: String): MetaItem? {
        val cacheKey = "$id:$type"
        val cached = cachedMetaDao.getByKey(cacheKey)
        if (cached != null) {
            return try {
                moshi.adapter(MetaItem::class.java).fromJson(cached.json)
            } catch (e: Exception) {
                null
            }
        }

        val addons = addonRepository.getEnabledAddons()
        var meta: Meta? = null
        for (addon in addons) {
            val base = buildAddonBaseUrl(addon.url)
            val fullUrl = "$base/meta/$type/$id.json"
            meta = try {
                stremioApi.getMeta(fullUrl).meta
            } catch (e: Exception) {
                null
            }
            if (meta != null) break
        }
        if (meta == null) return null

        val metaItem = convertToMetaItem(meta)
        val json = moshi.adapter(MetaItem::class.java).toJson(metaItem)
        cachedMetaDao.insert(CachedMeta(cacheKey, json))
        return metaItem
    }

    private fun convertToMetaItem(meta: Meta): MetaItem {
        return MetaItem(
            id = meta.id,
            type = meta.type,
            name = meta.name,
            poster = meta.poster,
            background = meta.background,
            imdbRating = meta.imdbRating,
            year = meta.year,
            releaseInfo = meta.releaseInfo,
            released = meta.released,
            description = meta.description,
            genre = meta.genre,
            runtime = meta.runtime,
            cast = meta.cast,
            imdbId = meta.imdb_id,
            videos = meta.videos?.map {
                Video(
                    season = it.season,
                    episode = it.episode,
                    name = it.name,
                    title = it.title,
                    description = it.description,
                    thumbnail = it.thumbnail,
                    url = it.url
                )
            }
        )
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/repository/AddonRepository.kt`
```
package com.ultrastream.app.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.ultrastream.app.data.dao.AddonDao
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.models.Catalog
import com.ultrastream.app.data.models.Extra
import com.ultrastream.app.network.StremioApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddonRepository @Inject constructor(
    private val addonDao: AddonDao,
    private val stremioApi: StremioApi,
    private val moshi: Moshi
) {

    private val catalogListType = Types.newParameterizedType(List::class.java, Catalog::class.java)
    private val catalogAdapter = moshi.adapter<List<Catalog>>(catalogListType)

    suspend fun installAddon(url: String): Addon? {
        val manifest = try {
            stremioApi.getManifest(url)
        } catch (e: Exception) {
            return null
        }
        if (manifest.id.isBlank()) return null

        val existing = addonDao.getById(manifest.id)
        if (existing != null) return existing

        val netCatalogs = manifest.catalogs ?: emptyList()
        val mappedCatalogs = netCatalogs.map { netCat ->
            Catalog(
                type = netCat.type,
                id = netCat.id,
                name = netCat.name,
                extraSupported = netCat.extraSupported,
                extra = netCat.extra?.map {
                    Extra(
                        name = it.name,
                        isRequired = it.isRequired,
                        options = it.options
                    )
                }
            )
        }

        val catalogsJson = catalogAdapter.toJson(mappedCatalogs)
        val addon = Addon(
            id = manifest.id,
            url = url,
            name = manifest.name ?: manifest.id,
            catalogs = catalogsJson,
            enabled = true,
            required = false
        )
        addonDao.insert(addon)
        return addon
    }

    suspend fun getAllAddons(): List<Addon> = addonDao.getAll()

    suspend fun getEnabledAddons(): List<Addon> {
        return addonDao.getAll().filter { it.enabled }
    }

    suspend fun toggleAddon(id: String, enabled: Boolean) {
        addonDao.updateEnabled(id, enabled)
    }

    suspend fun removeAddon(id: String) {
        addonDao.deleteById(id)
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/data/repository/StreamRepository.kt`
```
package com.ultrastream.app.data.repository

import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.data.models.Subtitle
import com.ultrastream.app.network.StremioApi
import com.ultrastream.app.network.Stream
import com.ultrastream.app.utils.DebridHelper
import com.ultrastream.app.utils.StreamParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepository @Inject constructor(
    private val stremioApi: StremioApi,
    private val debridHelper: DebridHelper,
    private val streamParser: StreamParser
) {

    suspend fun getStreams(
        metaId: String,
        metaType: String,
        season: Int? = null,
        episode: Int? = null,
        addonUrls: List<String>,
        hindiPriority: Boolean,
        debridKey: String?
    ): List<StreamItem> {
        val idWithExtra = if (season != null && episode != null) {
            "$metaId:$season:$episode"
        } else {
            metaId
        }

        return coroutineScope {
            val deferred = addonUrls.map { url ->
                async {
                    try {
                        var baseUrl = url
                        if (baseUrl.endsWith("/manifest.json")) {
                            baseUrl = baseUrl.substring(0, baseUrl.length - "/manifest.json".length)
                        } else if (baseUrl.endsWith("manifest.json")) {
                            baseUrl = baseUrl.substring(0, baseUrl.length - "manifest.json".length)
                        }
                        if (baseUrl.endsWith("/")) {
                            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
                        }

                        val fullUrl = if (season != null && episode != null) {
                            "$baseUrl/stream/$metaType/$idWithExtra.json"
                        } else {
                            "$baseUrl/stream/$metaType/$metaId.json"
                        }
                        val finalUrl = debridHelper.applyDebridParams(fullUrl, debridKey ?: "")
                        val response = stremioApi.getStreams(finalUrl)
                        response.streams?.mapNotNull { stream ->
                            val addonName = extractAddonName(url)
                            val streamItem = convertStream(stream, addonName)
                            if (season != null && episode != null) {
                                if (!streamParser.isValidEpisode(streamItem, season, episode)) {
                                    return@mapNotNull null
                                }
                            }
                            streamItem
                        } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            val results = deferred.awaitAll()
            val all = results.flatten()
            streamParser.sortStreams(all, hindiPriority)
        }
    }

    suspend fun resolveStream(stream: StreamItem, debridKey: String?): StreamItem {
        val resolvedUrl = debridHelper.resolveStreamUrl(stream.url ?: "", debridKey)
        return stream.copy(url = resolvedUrl)
    }

    private fun convertStream(stream: Stream, addonName: String): StreamItem {
        return StreamItem(
            url = stream.url,
            streamUrl = stream.streamUrl,
            externalUrl = stream.externalUrl,
            title = stream.title,
            name = stream.name,
            description = stream.description,
            infoHash = stream.infoHash,
            addonName = addonName,
            subtitles = stream.subtitles?.map {
                Subtitle(
                    url = it.url,
                    file = it.file,
                    lang = it.lang,
                    name = it.name
                )
            },
            isLive = stream.isLive
        )
    }

    private fun extractAddonName(url: String): String {
        val parts = url.split("/")
        return parts.getOrElse(2) { "addon" }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/UltraStreamApplication.kt`
```
package com.ultrastream.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UltraStreamApplication : Application()
```

### File: `./app/src/main/java/com/ultrastream/app/MainActivity.kt`
```
package com.ultrastream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.ui.navigation.Screen
import com.ultrastream.app.ui.screens.addons.AddonsScreen
import com.ultrastream.app.ui.screens.details.DetailsScreen
import com.ultrastream.app.ui.screens.home.HomeScreen
import com.ultrastream.app.ui.screens.library.LibraryScreen
import com.ultrastream.app.ui.screens.player.PlayerScreen
import com.ultrastream.app.ui.screens.profile.ProfileScreen
import com.ultrastream.app.ui.screens.search.SearchScreen
import com.ultrastream.app.ui.theme.UltraStreamTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UltraStreamTheme {
                UltraStreamNavHost()
            }
        }
    }

    @Composable
    fun UltraStreamNavHost() {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val items = listOf(
                        Triple(Screen.Home, "Home", R.drawable.ic_home),
                        Triple(Screen.Library, "Library", R.drawable.ic_library),
                        Triple(Screen.Search, "Search", R.drawable.ic_search),
                        Triple(Screen.Addons, "Addons", R.drawable.ic_addon),
                        Triple(Screen.Profile, "Profile", R.drawable.ic_profile)
                    )
                    items.forEach { (screen, title, iconRes) ->
                        NavigationBarItem(
                            icon = { Icon(imageVector = ImageVector.vectorResource(id = iconRes), contentDescription = title) },
                            label = { Text(title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen { id, type ->
                        navController.navigate(Screen.Details.pass(id, type))
                    }
                }
                composable(Screen.Library.route) {
                    LibraryScreen { id, type ->
                        navController.navigate(Screen.Details.pass(id, type))
                    }
                }
                composable(Screen.Search.route) {
                    SearchScreen { id, type ->
                        navController.navigate(Screen.Details.pass(id, type))
                    }
                }
                composable(Screen.Addons.route) {
                    AddonsScreen()
                }
                composable(Screen.Profile.route) {
                    ProfileScreen()
                }
                composable(Screen.Details.route) { backStackEntry ->
                    val id = URLDecoder.decode(backStackEntry.arguments?.getString("id") ?: "", "UTF-8")
                    val type = URLDecoder.decode(backStackEntry.arguments?.getString("type") ?: "", "UTF-8")
                    DetailsScreen(
                        id = id,
                        type = type,
                        onBack = { navController.popBackStack() },
                        onPlay = { stream, title ->
                            val json = moshi.adapter(StreamItem::class.java).toJson(stream)
                            navController.navigate(Screen.Player.pass(json, title))
                        }
                    )
                }
                composable(Screen.Player.route) { backStackEntry ->
                    val json = URLDecoder.decode(backStackEntry.arguments?.getString("streamJson") ?: "", "UTF-8")
                    val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
                    val stream = try {
                        moshi.adapter(StreamItem::class.java).fromJson(json)
                    } catch (e: Exception) { null }
                    if (stream != null) {
                        PlayerScreen(
                            stream = stream,
                            title = title.ifBlank { "Now Playing" },
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/utils/StreamParser.kt`
```
package com.ultrastream.app.utils

import com.ultrastream.app.data.models.StreamItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamParser @Inject constructor() {

    fun isValidEpisode(stream: StreamItem, targetSeason: Int, targetEpisode: Int): Boolean {
        val text = buildString {
            append(stream.title ?: "")
            append(" ")
            append(stream.name ?: "")
            append(" ")
            append(stream.description ?: "")
        }.uppercase()

        // Check for explicit episode patterns
        var hasExplicit = false
        var matchFound = false

        // EP, E, EPISODE
        val epRegex = Regex("(?:^|[^A-Z])(?:E|EP|EPISODE)[-\\s_]*(\\d{1,4})(?:[^A-Z]|$)")
        epRegex.findAll(text).forEach {
            hasExplicit = true
            if (it.groupValues[1].toIntOrNull() == targetEpisode) {
                matchFound = true
            }
        }

        // S01E01 format
        val sxeRegex = Regex("S(\\d{1,2})[-\\s_]*E(\\d{1,4})")
        sxeRegex.findAll(text).forEach {
            hasExplicit = true
            val s = it.groupValues[1].toIntOrNull()
            val e = it.groupValues[2].toIntOrNull()
            if (s == targetSeason && e == targetEpisode) {
                matchFound = true
            }
        }

        // 1x01 format
        val axbRegex = Regex("(?:^|[^A-Z0-9])(\\d{1,2})x(\\d{1,4})(?:[^A-Z0-9]|$)")
        axbRegex.findAll(text).forEach {
            // avoid resolutions like 1920x1080
            if (it.groupValues[1].toIntOrNull()?.let { num -> num < 100 } == true) {
                hasExplicit = true
                val s = it.groupValues[1].toIntOrNull()
                val e = it.groupValues[2].toIntOrNull()
                if (s == targetSeason && e == targetEpisode) {
                    matchFound = true
                }
            }
        }

        // If explicit episode marker found but no match, reject
        if (hasExplicit && !matchFound) return false

        // Isolated numbers (fallback)
        if (!hasExplicit) {
            val isoRegex = Regex("(?:^|[\\s\\-_\\[\\]])(\\d{1,4})(?:[\\s\\-_\\[\\]]|$)")
            var foundAny = false
            var isoMatch = false
            isoRegex.findAll(text).forEach {
                val num = it.groupValues[1].toIntOrNull() ?: return@forEach
                // Ignore technical numbers
                if (num in listOf(720, 1080, 2160, 480, 264, 265, 10)) return@forEach
                if (num in 1900..2100) return@forEach // year
                foundAny = true
                if (num == targetEpisode) {
                    isoMatch = true
                }
            }
            if (foundAny && !isoMatch) return false
        }

        return true
    }

    fun sortStreams(streams: List<StreamItem>, hindiPriority: Boolean): List<StreamItem> {
        return streams.sortedWith { a, b ->
            val textA = (a.title ?: "") + (a.name ?: "") + (a.description ?: "")
            val textB = (b.title ?: "") + (b.name ?: "") + (b.description ?: "")
            val hindiRegex = Regex("hindi|hin|हिंदी|हिन्दी|dual audio.*hindi|multi audio.*hindi", RegexOption.IGNORE_CASE)
            val hasHindiA = hindiRegex.containsMatchIn(textA)
            val hasHindiB = hindiRegex.containsMatchIn(textB)

            if (hindiPriority) {
                if (hasHindiA && !hasHindiB) return@sortedWith -1
                if (!hasHindiA && hasHindiB) return@sortedWith 1
            }

            // Quality score
            val qualRegex = Regex("4k|2160p|1080p|720p|hdr|dolby", RegexOption.IGNORE_CASE)
            val qualA = qualRegex.findAll(textA).count()
            val qualB = qualRegex.findAll(textB).count()
            qualB.compareTo(qualA) // higher quality first
        }
    }

    fun parseMetadata(rawText: String): ParsedInfo {
        val sizeMatch = Regex("\\b(\\d+(?:\\.\\d+)?)\\s*(GB|MB)\\b", RegexOption.IGNORE_CASE).find(rawText)
        val size = sizeMatch?.value?.uppercase()
        val sizeValueBytes = sizeMatch?.let {
            val value = it.groupValues[1].toDoubleOrNull() ?: 0.0
            val unit = it.groupValues[2].uppercase()
            when (unit) {
                "GB" -> (value * 1024 * 1024 * 1024).toLong()
                "MB" -> (value * 1024 * 1024).toLong()
                else -> null
            }
        }
        val seedMatch = Regex("(?:seeders|seeds|s)[:\\s]*(\\d+)", RegexOption.IGNORE_CASE).find(rawText)
        val seeds = seedMatch?.groupValues?.get(1)
        val langMatch = Regex("hindi|english|tamil|telugu|malayalam|bengali|dual audio|multi audio|हिंदी|हिन्दी", RegexOption.IGNORE_CASE)
            .findAll(rawText)
            .map { it.value }
            .toSet()
        val langs = langMatch.map { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }.toList()
        val qualMatch = Regex("4K|2160p|1080p|720p|480p|HDR|DV|CAM|HDTS|HDTC", RegexOption.IGNORE_CASE)
            .findAll(rawText)
            .map { it.value.uppercase() }
            .toSet()
        val quals = qualMatch.toList()
        val isLive = Regex("live|iptv|stream", RegexOption.IGNORE_CASE).containsMatchIn(rawText) && size == null && seeds == null
        val hasHindi = langs.any { it.contains("hindi", ignoreCase = true) }

        val yearMatch = Regex("\\b(19\\d{2}|20[0-2]\\d)\\b").find(rawText)
        val parsedYear = yearMatch?.value

        var parsedSeason: Int? = null
        var parsedEpisode: Int? = null

        val sxeMatch = Regex("\\b(\\d{1,2})x(\\d{1,4})\\b", RegexOption.IGNORE_CASE).find(rawText)
        if (sxeMatch != null && sxeMatch.groupValues[1].toIntOrNull()?.let { it < 100 } == true) {
            parsedSeason = sxeMatch.groupValues[1].toIntOrNull()
            parsedEpisode = sxeMatch.groupValues[2].toIntOrNull()
        } else {
            val seasonMatch = Regex("(?:^|[^A-Z])(?:S|SEASON)[-\\s_]*(\\d{1,2})\\b", RegexOption.IGNORE_CASE).find(rawText)
            parsedSeason = seasonMatch?.groupValues?.get(1)?.toIntOrNull()
            val episodeMatch = Regex("(?:^|[^A-Z])(?:E|EP|EPISODE)[-\\s_]*(\\d{1,4})\\b", RegexOption.IGNORE_CASE).find(rawText)
            parsedEpisode = episodeMatch?.groupValues?.get(1)?.toIntOrNull()
        }

        val cleanText = rawText
            .replace(Regex("\\b(\\d+(?:\\.\\d+)?\\s*(?:GB|MB))\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?:seeders|seeds|s)[:\\s]*(\\d+)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(hindi|english|tamil|telugu|malayalam|bengali|dual audio|multi audio|हिंदी|हिन्दी)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(4K|2160p|1080p|720p|480p|HDR|DV|CAM|HDTS|HDTC)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[\\u{1F300}-\\u{1F9FF}]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[\\u{2600}-\\u{26FF}]", RegexOption.IGNORE_CASE), "")
            .trim()
        return ParsedInfo(
            size = size,
            sizeValueBytes = sizeValueBytes,
            seeds = seeds,
            langs = langs,
            quals = quals,
            isLive = isLive,
            hasHindi = hasHindi,
            cleanText = cleanText.ifEmpty { "Direct Video Stream" },
            parsedYear = parsedYear,
            parsedSeason = parsedSeason,
            parsedEpisode = parsedEpisode
        )
    }

    data class ParsedInfo(
        val size: String?,
        val sizeValueBytes: Long?,
        val seeds: String?,
        val langs: List<String>,
        val quals: List<String>,
        val isLive: Boolean,
        val hasHindi: Boolean,
        val cleanText: String,
        val parsedYear: String?,
        val parsedSeason: Int?,
        val parsedEpisode: Int?
    )
}
```

### File: `./app/src/main/java/com/ultrastream/app/utils/AddonUrl.kt`
```
package com.ultrastream.app.utils

fun buildAddonBaseUrl(addonUrl: String): String {
    var base = addonUrl
    if (base.endsWith("/manifest.json")) base = base.removeSuffix("/manifest.json")
    else if (base.endsWith("manifest.json")) base = base.removeSuffix("manifest.json")
    if (base.endsWith("/")) base = base.removeSuffix("/")
    return base
}
```

### File: `./app/src/main/java/com/ultrastream/app/utils/ShareHelper.kt`
```
package com.ultrastream.app.utils

import android.content.Context
import android.content.Intent

object ShareHelper {
    fun shareText(context: Context, text: String, subject: String = "UltraStream") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/utils/DebridHelper.kt`
```
package com.ultrastream.app.utils

import com.ultrastream.app.network.RealDebridApi
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebridHelper @Inject constructor(
    private val realDebridApi: RealDebridApi
) {

    suspend fun resolveStreamUrl(url: String, debridKey: String?): String {
        if (debridKey.isNullOrBlank()) return url

        val auth = "Bearer $debridKey"

        if (url.startsWith("magnet:")) {
            return resolveMagnet(url, auth)
        }

        if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            return resolveMagnet(magnet, auth)
        }

        return applyDebridParams(url, debridKey)
    }

    private suspend fun resolveMagnet(magnet: String, auth: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) return magnet

        val availability = realDebridApi.checkInstantAvailability(auth, hash)
        if (availability.isNotEmpty()) {
            val cached = availability.values.firstOrNull { it.isNotEmpty() }
            if (cached != null) {
                val addResponse = realDebridApi.addMagnet(auth, magnet)
                val torrentId = addResponse.id
                var status = realDebridApi.getTorrentStatus(auth, torrentId)
                var attempts = 0
                while (status.status != "downloaded" && status.status != "ready" && attempts < 30) {
                    delay(1000)
                    status = realDebridApi.getTorrentStatus(auth, torrentId)
                    attempts++
                }
                if (status.status == "downloaded" || status.status == "ready") {
                    val link = status.links.firstOrNull() ?: return magnet
                    val unrestricted = realDebridApi.unrestrictLink(auth, link)
                    return unrestricted.link
                }
            }
        }
        return magnet
    }

    private fun extractHash(magnet: String): String {
        val match = Regex("btih:([a-fA-F0-9]{40})").find(magnet)
        return match?.groupValues?.get(1) ?: ""
    }

    // FIXED: Made public so StreamRepository can access it without visibility errors
    fun applyDebridParams(url: String, debridKey: String): String {
        if (debridKey.isBlank()) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}realdebrid=$debridKey"
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/utils/UrlClassifier.kt`
```
package com.ultrastream.app.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlClassifier @Inject constructor() {

    fun classify(url: String): UrlType {
        if (url.isBlank()) return UrlType.INVALID
        val lower = url.lowercase()
        return when {
            lower.startsWith("magnet:") -> UrlType.MAGNET
            lower.contains(".m3u8") || lower.contains(".m3u") -> UrlType.HLS
            lower.contains(".mpd") -> UrlType.DASH
            lower.contains("pengu.uk") || lower.contains("streamraiwind") || lower.contains("cdn") || lower.contains("proxy") -> UrlType.PROXY
            lower.matches(Regex(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$")) -> UrlType.DIRECT
            else -> UrlType.UNKNOWN
        }
    }

    enum class UrlType {
        HLS, DASH, PROXY, DIRECT, MAGNET, UNKNOWN, INVALID
    }
}
```

### File: `./app/src/main/java/com/ultrastream/app/utils/M3UParser.kt`
```
package com.ultrastream.app.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UParser @Inject constructor() {

    fun parseM3U(content: String): List<M3UItem> {
        val lines = content.lines()
        val items = mutableListOf<M3UItem>()
        var currentTitle: String? = null
        var currentGroup: String? = null
        var currentLogo: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:")) {
                val titleMatch = Regex(",([^,]*)$").find(trimmed)
                currentTitle = titleMatch?.groupValues?.get(1)?.trim()
                val groupMatch = Regex("group-title=\"([^\"]*)\"").find(trimmed)
                currentGroup = groupMatch?.groupValues?.get(1)
                val logoMatch = Regex("tvg-logo=\"([^\"]*)\"").find(trimmed)
                currentLogo = logoMatch?.groupValues?.get(1)
            } else if (!trimmed.startsWith("#")) {
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("magnet:") || trimmed.startsWith("/")) {
                    items.add(M3UItem(
                        url = trimmed,
                        title = currentTitle ?: "Unknown",
                        group = currentGroup,
                        logo = currentLogo
                    ))
                }
                currentTitle = null
                currentGroup = null
                currentLogo = null
            }
        }
        return items
    }

    data class M3UItem(
        val url: String,
        val title: String,
        val group: String?,
        val logo: String?
    )
}
```

### File: `./app/src/main/AndroidManifest.xml`
```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.ultrastream.app">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:name=".UltraStreamApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.UltraStream"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".MainActivity"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:exported="true"
            android:supportsPictureInPicture="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>
</manifest>
```

### File: `./app/src/main/res/values/themes.xml`
```
<resources>
    <style name="Theme.UltraStream" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

### File: `./app/src/main/res/values/strings.xml`
```
<resources>
    <string name="app_name">UltraStream</string>
</resources>
```

### File: `./app/src/main/res/drawable/ic_profile.xml`
```
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#000000" android:pathData="M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"/>
</vector>
```

### File: `./app/src/main/res/drawable/ic_addon.xml`
```
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#000000" android:pathData="M19,5h-2V3h-2v2h-2v2h2v2h2V7h2V5zM13,3h-2v2h2V3zM9,7V5h2V3H7v2H5v2h2v2h2V7zM9,9H7v2h2V9zM11,11h2V9h-2V11zM15,11h2V9h-2V11zM19,9h-2v2h2V9zM19,11v2h2v-2H19zM17,15h-2v-2h-2v2h2v2h2V15zM13,15h-2v-2H9v2h2v2h2V15zM15,19v-2h-2v2h2zM19,15h-2v2h2V15zM13,19h-2v-2h2V19zM9,19h2v-2H9V19zM5,13h2v-2H5V13zM5,17h2v-2H5V17zM5,19v-2H3v2H5zM13,11h2V9h-2V11z"/>
</vector>
```

### File: `./app/src/main/res/drawable/ic_launcher_foreground_round.xml`
```
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
  <path
      android:fillColor="#38BDF8"
      android:pathData="M54,18 L54,90 L18,54 Z" />
  <path
      android:fillColor="#38BDF8"
      android:pathData="M54,18 L54,90 L90,54 Z" />
</vector>
```

### File: `./app/src/main/res/drawable/ic_home.xml`
```
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#000000" android:pathData="M12,3L2,12h3v8h6v-6h2v6h6v-8h3L12,3z"/>
</vector>
```

### File: `./app/src/main/res/drawable/ic_library.xml`
```
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#000000" android:pathData="M4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6z"/>
    <path android:fillColor="#000000" android:pathData="M20,2H8c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2z"/>
</vector>
```

### File: `./app/src/main/res/drawable/ic_search.xml`
```
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#000000" android:pathData="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"/>
</vector>
```

### File: `./app/src/main/res/drawable/ic_launcher_foreground.xml`
```
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
  <path
      android:fillColor="#38BDF8"
      android:pathData="M54,18 L54,90 L18,54 Z" />
  <path
      android:fillColor="#38BDF8"
      android:pathData="M54,18 L54,90 L90,54 Z" />
</vector>
```

### File: `./app/src/main/res/xml/file_paths.xml`
```
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="cache" path="/" />
    <files-path name="files" path="/" />
    <external-files-path name="external_files" path="/" />
</paths>
```

### File: `./app/proguard-rules.pro`
```
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class androidx.media3.exoplayer.hls.** { *; }
-keep class androidx.media3.exoplayer.dash.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keepclassmembers class * {
    native <methods>;
}
```

