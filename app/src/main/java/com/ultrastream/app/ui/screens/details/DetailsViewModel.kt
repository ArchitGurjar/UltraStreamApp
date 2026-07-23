package com.ultrastream.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.models.LibraryItem
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.data.models.WatchlistItem
import com.ultrastream.app.data.models.WatchProgress
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import com.ultrastream.app.data.repository.StreamRepository
import com.ultrastream.app.data.dao.LibraryDao
import com.ultrastream.app.data.dao.WatchProgressDao
import com.ultrastream.app.data.dao.WatchedEpisodeDao
import com.ultrastream.app.data.dao.WatchlistDao
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

    fun loadMeta(id: String, type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val meta = metaRepository.getMeta(id, type)
            if (meta != null) {
                // Check if in library/watchlist
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

    fun loadStreams(id: String, type: String, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(streamsLoading = true, streams = emptyList())

            try {
                // Fetch installed addon URLs (only enabled ones)
                val addons = addonRepository.getEnabledAddons()
                val addonUrls = addons.map { it.url }

                // Get preferences
                val hindiPriority = preferencesManager.getHindiPriority().first()
                val debridKey = preferencesManager.getDebridKey().first()

                // Fetch streams
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

    data class DetailsUiState(
        val isLoading: Boolean = false,
        val meta: MetaItem? = null,
        val inLibrary: Boolean = false,
        val inWatchlist: Boolean = false,
        val watchProgress: WatchProgress? = null,
        val error: String? = null,
        val streams: List<StreamItem> = emptyList(),
        val streamsLoading: Boolean = false
    )
}

// Extension functions to convert MetaItem to LibraryItem/WatchlistItem
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
