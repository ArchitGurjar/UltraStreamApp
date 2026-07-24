package com.ultrastream.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.dao.*
import com.ultrastream.app.data.models.*
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import com.ultrastream.app.data.repository.StreamRepository
import com.ultrastream.app.network.StremioApi
import com.ultrastream.app.utils.LinkVerifier
import com.ultrastream.app.utils.buildAddonBaseUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
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
    private val watchlistDao: WatchlistDao,
    private val smartPlaylistDao: SmartPlaylistDao,
    private val stremioApi: StremioApi,
    private val linkVerifier: LinkVerifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    private val _selectedSubtitle = MutableStateFlow<Subtitle?>(null)
    val selectedSubtitle: StateFlow<Subtitle?> = _selectedSubtitle.asStateFlow()
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var currentSeason: Int? = null
    private var currentEpisode: Int? = null

    private val _filteredEpisodes = MutableStateFlow<List<Video>>(emptyList())
    val filteredEpisodes: StateFlow<List<Video>> = _filteredEpisodes.asStateFlow()

    private val _availableSeasons = MutableStateFlow<List<Int>>(emptyList())
    val availableSeasons: StateFlow<List<Int>> = _availableSeasons.asStateFlow()

    private val _selectedSeason = MutableStateFlow<Int?>(null)
    val selectedSeason: StateFlow<Int?> = _selectedSeason.asStateFlow()

    private val _isAllSeasons = MutableStateFlow(false)
    val isAllSeasons: StateFlow<Boolean> = _isAllSeasons.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val episodeListType = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
    private val episodeAdapter = moshi.adapter<List<PlaylistEpisode>>(episodeListType)

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
                filterAndSortEpisodes(meta.videos)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Meta not found"
                )
            }
        }
    }

    private fun filterAndSortEpisodes(episodes: List<Video>?) {
        if (episodes == null) {
            _filteredEpisodes.value = emptyList()
            _availableSeasons.value = emptyList()
            return
        }
        val seen = mutableSetOf<String>()
        val seasonMap = mutableMapOf<Int, MutableList<Video>>()

        episodes.forEach { ep ->
            if (ep.season == null || ep.episode == null) return@forEach
            if (ep.season == 0 || ep.episode == 0) return@forEach
            val name = ep.name ?: ep.title ?: ""
            if (listOf(480, 720, 1080, 2160, 264, 265).contains(ep.episode) && name.isBlank()) return@forEach
            val key = "S${ep.season}E${ep.episode}"
            if (seen.contains(key)) return@forEach
            seen.add(key)
            seasonMap.getOrPut(ep.season) { mutableListOf() }.add(ep)
        }

        seasonMap.values.forEach { list -> list.sortBy { it.episode ?: 0 } }
        val all = seasonMap.keys.sorted().flatMap { seasonMap[it] ?: emptyList() }
        val seasons = all.mapNotNull { it.season }.distinct().sorted()
        _availableSeasons.value = seasons
        _filteredEpisodes.value = all
        if (seasons.isNotEmpty() && _selectedSeason.value == null && !_isAllSeasons.value) {
            _selectedSeason.value = seasons.first()
        }
        applySeasonFilter()
    }

    fun toggleAllSeasons() {
        _isAllSeasons.value = !_isAllSeasons.value
        if (_isAllSeasons.value) {
            _selectedSeason.value = null
        } else {
            val seasons = _availableSeasons.value
            if (seasons.isNotEmpty()) _selectedSeason.value = seasons.first()
        }
        applySeasonFilter()
    }

    fun selectSeason(season: Int?) {
        _selectedSeason.value = season
        if (season != null) _isAllSeasons.value = false
        applySeasonFilter()
    }

    private fun applySeasonFilter() {
        val all = _filteredEpisodes.value
        if (all.isEmpty()) return
        val result = if (_isAllSeasons.value || _selectedSeason.value == null) {
            all
        } else {
            all.filter { it.season == _selectedSeason.value }
        }
        _filteredEpisodes.value = result
    }

    fun selectSeasonAndLoad(season: Int?) {
        selectSeason(season)
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

    // ============================================================
    // SMART PLAYLIST CREATION (Background fetch with verification)
    // ============================================================
    
suspend fun createSmartPlaylist(meta: MetaItem, season: Int): Boolean {
    return try {
        val episodes = meta.videos?.filter { it.season == season } ?: emptyList()
        if (episodes.isEmpty()) return false

        // Create initial playlist with all episodes marked as missing
        val playlistEpisodes = episodes.map { ep ->
            PlaylistEpisode(
                epNum = ep.episode ?: 0,
                epName = ep.name ?: "Episode \${ep.episode}",
                title = "\${meta.name} - S\${season}E\${ep.episode}",
                stream = null,
                isMissing = true
            )
        }

        val episodesJson = episodeAdapter.toJson(playlistEpisodes)
        val playlistId = "\${meta.id}_S\${season}_\${System.currentTimeMillis()}"
        val playlist = SmartPlaylist(
            id = playlistId,
            metaId = meta.id,
            metaName = meta.name,
            poster = meta.poster,
            season = season,
            addon = "SmartPlaylist",
            total = episodes.size,
            fetched = 0,
            status = "Fetching...",
            episodesJson = episodesJson
        )

        smartPlaylistDao.insert(playlist)

        // Launch background fetch
        viewModelScope.launch {
            try {
                val addons = addonRepository.getEnabledAddons()
                val addonUrls = addons.map { it.url }
                val hindiPriority = preferencesManager.getHindiPriority().first()
                val debridKey = preferencesManager.getDebridKey().first()
                var fetchedCount = 0

                // Process episodes one by one (could be parallel, but let's do sequential for simplicity)
                val updatedEpisodes = playlistEpisodes.toMutableList()
                for ((index, episode) in playlistEpisodes.withIndex()) {
                    val epNum = episode.epNum
                    // Fetch streams for this episode
                    val streams = streamRepository.getStreams(
                        metaId = meta.id,
                        metaType = meta.type,
                        season = season,
                        episode = epNum,
                        addonUrls = addonUrls,
                        hindiPriority = hindiPriority,
                        debridKey = if (debridKey.isNotBlank()) debridKey else null
                    )
                    // Find the best stream (first after sorting)
                    val bestStream = streams.firstOrNull()
                    // Verify link if possible
                    var isValid = false
                    if (bestStream != null) {
                        val url = bestStream.url ?: bestStream.streamUrl ?: bestStream.externalUrl
                        if (!url.isNullOrBlank()) {
                            // Verify only if it's not a magnet
                            if (!url.startsWith("magnet:")) {
                                isValid = linkVerifier.verifyLink(url)
                            } else {
                                // For magnets, we assume they are valid (debrid will handle)
                                isValid = true
                            }
                        }
                    }
                    if (bestStream != null && isValid) {
                        updatedEpisodes[index] = episode.copy(stream = bestStream, isMissing = false)
                        fetchedCount++
                    } else {
                        // Keep as missing
                        updatedEpisodes[index] = episode.copy(isMissing = true)
                    }
                    // Update playlist after each episode
                    val updatedJson = episodeAdapter.toJson(updatedEpisodes)
                    smartPlaylistDao.updatePlaylist(
                        id = playlistId,
                        fetched = fetchedCount,
                        status = if (fetchedCount == updatedEpisodes.size) "Complete" else "Fetching... \${fetchedCount}/\${updatedEpisodes.size}",
                        episodesJson = updatedJson
                    )
                }
                // Final update
                val finalJson = episodeAdapter.toJson(updatedEpisodes)
                smartPlaylistDao.updatePlaylist(
                    id = playlistId,
                    fetched = fetchedCount,
                    status = if (fetchedCount == updatedEpisodes.size) "Complete" else "Partial",
                    episodesJson = finalJson
                )
            } catch (e: Exception) {
                // Mark as failed
                smartPlaylistDao.updateStatus(playlistId, "Failed: \${e.message}")
            }
        }

        true
    } catch (e: Exception) {
        false
    }
}
 ?: emptyList()
            if (episodes.isEmpty()) return false

            val playlistId = "${meta.id}_S${season}_${System.currentTimeMillis()}"
            val initialPlaylist = SmartPlaylist(
                id = playlistId,
                metaId = meta.id,
                metaName = meta.name,
                poster = meta.poster,
                season = season,
                addon = "SmartPlaylist",
                total = episodes.size,
                fetched = 0,
                status = "Fetching...",
                episodesJson = "[]"
            )
            smartPlaylistDao.insert(initialPlaylist)

            viewModelScope.launch {
                val addons = addonRepository.getEnabledAddons().map { it.url }
                val hindiPriority = preferencesManager.getHindiPriority().first()
                val debridKey = preferencesManager.getDebridKey().first()
                val fetchedEpisodes = mutableListOf<PlaylistEpisode>()

                episodes.forEachIndexed { index, ep ->
                    val epNum = ep.episode ?: 0
                    val streams = streamRepository.getStreams(
                        meta.id, meta.type, season, epNum, addons, hindiPriority, debridKey.takeIf { it.isNotBlank() }
                    )
                    var bestWorkingStream: StreamItem? = null
                    for (stream in streams) {
                        val sUrl = stream.url ?: stream.streamUrl ?: stream.externalUrl
                        if (sUrl != null && !sUrl.startsWith("magnet:")) {
                            if (linkVerifier.verifyLinkStatus(sUrl)) {
                                bestWorkingStream = stream
                                break
                            }
                        }
                    }

                    fetchedEpisodes.add(
                        PlaylistEpisode(
                            epNum = epNum,
                            epName = ep.name ?: "Episode $epNum",
                            title = "${meta.name} - S${season}E$epNum",
                            stream = bestWorkingStream,
                            isMissing = bestWorkingStream == null
                        )
                    )

                    val updatedPlaylist = initialPlaylist.copy(
                        fetched = index + 1,
                        status = if (index + 1 == episodes.size) "Ready" else "Fetching...",
                        episodesJson = episodeAdapter.toJson(fetchedEpisodes)
                    )
                    smartPlaylistDao.insert(updatedPlaylist)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ============================================================
    // SUBTITLE FETCHING FROM ADDONS
    // ============================================================
    suspend fun fetchSubtitles(metaId: String, type: String, season: Int, episode: Int): List<Subtitle> {
        val allSubtitles = mutableListOf<Subtitle>()
        val addons = addonRepository.getEnabledAddons()
        val idWithExtra = "$metaId:$season:$episode"

        for (addon in addons) {
            val baseUrl = buildAddonBaseUrl(addon.url)
            val url = "$baseUrl/subtitles/$type/$idWithExtra.json"
            try {
                val response = stremioApi.getSubtitles(url)
                response.subtitles?.let { subs ->
                    subs.forEach { netSub ->
                        allSubtitles.add(
                            Subtitle(
                                url = netSub.url,
                                file = netSub.file,
                                lang = netSub.lang,
                                name = netSub.name
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // skip this addon
            }
        }
        return allSubtitles.distinctBy { it.url ?: it.file }
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

    fun setSelectedSubtitle(subtitle: Subtitle) {
        _selectedSubtitle.value = subtitle
    }

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
