package com.ultrastream.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.models.HistoryItem
import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.MetaRepository
import com.ultrastream.app.data.repository.StreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val metaRepository: MetaRepository,
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Load addons
            val addons = addonRepository.getAllAddons()
            // For each addon, load its catalogs (we'll fetch in screen)
            // We'll also load continue watching from history
            // For now we just set addons
            _uiState.value = _uiState.value.copy(
                addons = addons,
                isLoading = false
            )
            // Actually we need to fetch catalogs later
        }
    }

    fun refresh() {
        loadHomeData()
    }

    data class HomeUiState(
        val isLoading: Boolean = false,
        val addons: List<com.ultrastream.app.data.models.Addon> = emptyList(),
        val continueWatching: List<HistoryItem> = emptyList(),
        val catalogRows: Map<String, List<MetaItem>> = emptyMap() // rowId -> list
    )
}
