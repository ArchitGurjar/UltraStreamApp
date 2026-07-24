# UltraStream Targeted Export

## File: `app/src/main/java/com/ultrastream/app/data/repository/MetaRepository.kt`
```kotlin
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


## File: `app/src/main/java/com/ultrastream/app/data/repository/StreamRepository.kt`
```kotlin
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


## File: `app/src/main/java/com/ultrastream/app/ui/screens/details/DetailsViewModel.kt`
```kotlin
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
import com.ultrastream.app.utils.buildAddonBaseUrl
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
    private val watchlistDao: WatchlistDao,
    private val smartPlaylistDao: SmartPlaylistDao,
    private val stremioApi: StremioApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var currentSeason: Int? = null
    private var currentEpisode: Int? = null

    // Filtered episodes & seasons
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
        val filtered = mutableListOf<Video>()
        val seen = mutableSetOf<String>()
        val seasonMap = mutableMapOf<Int, MutableList<Video>>()
        val junkPattern = Regex(
            "opening|ending|creditless|ncop|nced|trailer|promo|teaser|ova|oav|special",
            RegexOption.IGNORE_CASE
        )

        episodes.forEach { ep ->
            if (ep.season == null || ep.episode == null) return@forEach
            if (ep.season == 0 || ep.episode == 0) return@forEach
            val name = ep.name ?: ep.title ?: ""
            if (junkPattern.containsMatchIn(name)) return@forEach
            if (listOf(480, 720, 1080, 2160, 264, 265).contains(ep.episode) && name.isBlank()) return@forEach
            val key = "S${ep.season}E${ep.episode}"
            if (seen.contains(key)) return@forEach
            seen.add(key)
            seasonMap.getOrPut(ep.season) { mutableListOf() }.add(ep)
        }

        seasonMap.values.forEach { list -> list.sortBy { it.episode ?: 0 } }
        // Remove outliers (gaps > 20)
        seasonMap.values.forEach { seasonEpisodes ->
            if (seasonEpisodes.size > 1) {
                var prev = seasonEpisodes[0].episode ?: 0
                val toRemove = mutableListOf<Video>()
                for (i in 1 until seasonEpisodes.size) {
                    val current = seasonEpisodes[i].episode ?: 0
                    if (current > prev + 20) {
                        toRemove.add(seasonEpisodes[i])
                    }
                    prev = current
                }
                seasonEpisodes.removeAll(toRemove)
            }
        }

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
    // SMART PLAYLIST CREATION (Room insertion)
    // ============================================================
    suspend fun createSmartPlaylist(meta: MetaItem, season: Int): Boolean {
        return try {
            val episodes = meta.videos?.filter { it.season == season } ?: emptyList()
            if (episodes.isEmpty()) return false

            val playlistEpisodes = episodes.map { ep ->
                PlaylistEpisode(
                    epNum = ep.episode ?: 0,
                    epName = ep.name ?: "Episode ${ep.episode}",
                    title = "${meta.name} - S${season}E${ep.episode}",
                    stream = null,
                    isMissing = true
                )
            }

            val episodesJson = episodeAdapter.toJson(playlistEpisodes)
            val playlist = SmartPlaylist(
                id = "${meta.id}_S${season}_${System.currentTimeMillis()}",
                metaId = meta.id,
                metaName = meta.name,
                poster = meta.poster,
                season = season,
                addon = "SmartPlaylist",
                total = episodes.size,
                fetched = 0,
                status = "Pending",
                episodesJson = episodesJson
            )

            smartPlaylistDao.insert(playlist)
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


## File: `app/src/main/java/com/ultrastream/app/ui/screens/search/SearchViewModel.kt`
```kotlin
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


## File: `app/src/main/java/com/ultrastream/app/ui/components/StreamCard.kt`
```kotlin
@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.ui.theme.*

@Composable
fun StreamCard(
    stream: StreamItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stream.addonName ?: "Addon",
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "4KHDHub 4K",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stream.title ?: stream.name ?: "Stream",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Tag(text = "2024", icon = Icons.Default.CalendarToday, color = AccentGold)
                Tag(text = "Hindi", icon = Icons.Default.Translate, color = AccentOrange)
                Tag(text = "18.74 GB", icon = Icons.Default.Storage, color = AccentOrange)
                Tag(text = "2160P", icon = Icons.Default.Monitor, color = Color.White)
                
                OutlinedTag(text = "HDR", color = TextMuted)
                OutlinedTag(text = "DV", color = TextMuted)
                OutlinedTag(text = "English", color = AccentBlue)
            }
        }
    }
}

@Composable
private fun Tag(text: String, icon: ImageVector, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OutlinedTag(text: String, color: Color) {
    Surface(
        color = Color.Transparent,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

```

