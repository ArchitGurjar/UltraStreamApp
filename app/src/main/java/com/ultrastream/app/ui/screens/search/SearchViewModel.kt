package com.ultrastream.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import com.ultrastream.app.network.StremioApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val metaRepository: MetaRepository,
    private val stremioApi: StremioApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(query: String, filter: String = "all", sort: String = "popular") {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = mutableListOf<MetaItem>()

            try {
                // Get all enabled addons
                val addons = addonRepository.getEnabledAddons()
                // For each addon, check if it supports search for the given type(s)
                // We'll iterate over types based on filter
                val types = when (filter) {
                    "all" -> listOf("movie", "series", "anime", "tv")
                    else -> listOf(filter)
                }

                for (addon in addons) {
                    // Get base URL
                    val baseUrl = addon.url.replace("/manifest.json", "")
                    // For each type, try to search
                    for (type in types) {
                        // Check if addon supports search for this type
                        val catalogList = addon.catalogs // JSON
                        // Parse catalogs to check if any catalog of this type has search support
                        // We'll use Moshi to parse
                        val moshi = com.squareup.moshi.Moshi.Builder()
                            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                            .build()
                        val catalogType = com.ultrastream.app.data.models.Catalog::class.java
                        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, catalogType)
                        val adapter = moshi.adapter<List<com.ultrastream.app.data.models.Catalog>>(listType)
                        val catalogs = adapter.fromJson(addon.catalogs) ?: emptyList()

                        // Find a catalog with this type that supports search
                        val searchableCatalog = catalogs.firstOrNull { cat ->
                            cat.type == type && (cat.extraSupported?.contains("search") == true ||
                                cat.extra?.any { it.name == "search" } == true)
                        }

                        if (searchableCatalog != null) {
                            val searchUrl = "$baseUrl/catalog/${type}/${searchableCatalog.id}/search=${query}.json"
                            try {
                                val response = stremioApi.getCatalog(type, searchableCatalog.id, query)
                                response.metas?.forEach { meta ->
                                    // Convert to MetaItem
                                    val metaItem = MetaItem(
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
                                            com.ultrastream.app.data.models.Video(
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
                                    results.add(metaItem)
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                }

                // Remove duplicates by id
                val unique = results.distinctBy { it.id }

                // Apply sorting
                val sorted = when (sort) {
                    "rating" -> unique.sortedByDescending { it.imdbRating?.toDoubleOrNull() ?: 0.0 }
                    "year" -> unique.sortedByDescending { it.year?.toIntOrNull() ?: 0 }
                    else -> unique // popular (keep as is)
                }

                _uiState.value = _uiState.value.copy(
                    results = sorted,
                    isSearching = false
                )
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
