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
