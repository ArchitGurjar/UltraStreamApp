package com.ultrastream.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val metaRepository: MetaRepository
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
            // For now we'll just fetch from Cinemeta (dummy)
            // In real implementation, we'd search all addons
            // Placeholder: fetch from a known catalog
            val results = mutableListOf<MetaItem>()
            // We'll simulate a search with a single addon
            try {
                // Use Cinemeta addon (hardcoded for demo)
                val addons = addonRepository.getAllAddons()
                val cinemeta = addons.find { it.id == "com.stremio.cinemeta" }
                if (cinemeta != null) {
                    // We'll need a real search call - for now we just set dummy results
                    // In real implementation we'd call streamRepository or metaRepository
                }
                // Dummy results
                results.add(
                    MetaItem(
                        id = "tt1234567",
                        type = "movie",
                        name = "Dummy Movie 1",
                        poster = null,
                        background = null,
                        imdbRating = "8.5",
                        year = "2023",
                        releaseInfo = null,
                        released = null,
                        description = "A dummy movie for testing",
                        genre = listOf("Action", "Drama"),
                        runtime = "120 min",
                        cast = listOf("Actor A", "Actor B"),
                        imdbId = "tt1234567",
                        videos = null
                    )
                )
            } catch (e: Exception) {
                // ignore
            }
            _uiState.value = _uiState.value.copy(results = results, isSearching = false)
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
    }

    data class SearchUiState(
        val isSearching: Boolean = false,
        val results: List<MetaItem> = emptyList()
    )
}
