package com.ultrastream.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrastream.app.data.dao.LibraryDao
import com.ultrastream.app.data.dao.WatchlistDao
import com.ultrastream.app.data.dao.HistoryDao
import com.ultrastream.app.data.dao.SmartPlaylistDao
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
    private val historyDao: HistoryDao,
    private val smartPlaylistDao: SmartPlaylistDao
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
            val smartPlaylists = smartPlaylistDao.getAll()
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

    fun exportPlaylistM3U(playlist: SmartPlaylist) {
        // Build M3U content from episodesJson and share
        viewModelScope.launch {
            try {
                val episodes = episodeAdapter.fromJson(playlist.episodesJson) ?: emptyList()
                val workingStreams = episodes.mapNotNull { it.stream }.filter { it.url != null }
                if (workingStreams.isEmpty()) {
                    Toast.makeText(context, "No valid streams to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val exporter = M3UExporter(context)
                val file = exporter.exportToM3U(workingStreams, playlist.metaName, "playlist_${playlist.id}.m3u")
                if (file != null) {
                    exporter.shareM3U(file)
                } else {
                    Toast.makeText(context, "Failed to create M3U", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error exporting playlist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun playAll(playlist: SmartPlaylist) {
        // Play the first available stream
        viewModelScope.launch {
            val episodes = episodeAdapter.fromJson(playlist.episodesJson) ?: emptyList()
            val firstStream = episodes.firstOrNull { it.stream != null }?.stream
            if (firstStream != null) {
                // Navigate to player with this stream
                // This needs to be handled via callback; we'll emit a flow.
                _playStreamEvent.value = firstStream to playlist.metaName
            } else {
                Toast.makeText(context, "No playable streams", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
