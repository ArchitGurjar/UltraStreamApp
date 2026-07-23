package com.ultrastream.app.ui.screens.profile

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ultrastream.app.data.database.AppDatabase
import com.ultrastream.app.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.getTheme().collect { theme ->
                _uiState.value = _uiState.value.copy(theme = theme)
            }
        }
        viewModelScope.launch {
            preferencesManager.getHindiPriority().collect { priority ->
                _uiState.value = _uiState.value.copy(hindiPriority = priority)
            }
        }
        viewModelScope.launch {
            preferencesManager.getAutoPlayNext().collect { auto ->
                _uiState.value = _uiState.value.copy(autoPlayNext = auto)
            }
        }
        viewModelScope.launch {
            preferencesManager.getParentalControl().collect { pc ->
                _uiState.value = _uiState.value.copy(parentalControl = pc)
            }
        }
        viewModelScope.launch {
            preferencesManager.getCurrentProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(currentProfile = profile)
            }
        }
    }

    suspend fun toggleTheme() {
        val current = uiState.value.theme
        val newTheme = if (current == "dark") "light" else "dark"
        preferencesManager.setTheme(newTheme)
    }

    suspend fun toggleHindiPriority() {
        val current = uiState.value.hindiPriority
        preferencesManager.setHindiPriority(!current)
    }

    suspend fun toggleAutoPlayNext() {
        val current = uiState.value.autoPlayNext
        preferencesManager.setAutoPlayNext(!current)
    }

    suspend fun toggleParentalControl() {
        val current = uiState.value.parentalControl
        preferencesManager.setParentalControl(!current)
    }

    suspend fun exportData(context: Context): Boolean {
        return try {
            val dataMap = mutableMapOf<String, Any>()
            dataMap["addons"] = database.addonDao().getAll()
            dataMap["library"] = database.libraryDao().getAll()
            dataMap["watchlist"] = database.watchlistDao().getAll()
            dataMap["history"] = database.historyDao().getAll()
            dataMap["watchedEpisodes"] = database.watchedEpisodeDao().getAll()
            dataMap["watchProgress"] = database.watchProgressDao().getAll()
            dataMap["smartPlaylists"] = database.smartPlaylistDao().getAll()
            dataMap["profiles"] = database.profileDao().getAll()
            dataMap["cachedMeta"] = database.cachedMetaDao().getAll()

            val prefs = mutableMapOf<String, Any>()
            prefs["theme"] = preferencesManager.getTheme().first()
            prefs["debridKey"] = preferencesManager.getDebridKey().first()
            prefs["currentProfile"] = preferencesManager.getCurrentProfile().first()
            prefs["hindiPriority"] = preferencesManager.getHindiPriority().first()
            prefs["autoPlayNext"] = preferencesManager.getAutoPlayNext().first()
            prefs["parentalControl"] = preferencesManager.getParentalControl().first()
            dataMap["preferences"] = prefs

            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(dataMap)

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "ultrastream_backup.json")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/UltraStream")
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importData(context: Context, uri: Uri): Boolean {
        return try {
            val resolver = context.contentResolver
            val inputStream = resolver.openInputStream(uri) ?: return false
            val gson = Gson()
            val dataMap = gson.fromJson(inputStream.reader(), Map::class.java) as Map<String, Any>

            // Restore addons
            (dataMap["addons"] as? List<*>)?.let { list ->
                val addons = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.Addon>::class.java).toList()
                database.addonDao().insertAll(addons)
            }
            // Restore library
            (dataMap["library"] as? List<*>)?.let { list ->
                val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.LibraryItem>::class.java).toList()
                database.libraryDao().insertAll(items)
            }
            // Restore watchlist
            (dataMap["watchlist"] as? List<*>)?.let { list ->
                val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.WatchlistItem>::class.java).toList()
                database.watchlistDao().insertAll(items)
            }
            // Restore history
            (dataMap["history"] as? List<*>)?.let { list ->
                val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.HistoryItem>::class.java).toList()
                database.historyDao().insertAll(items)
            }
            // Restore watched episodes
            (dataMap["watchedEpisodes"] as? List<*>)?.let { list ->
                val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.WatchedEpisode>::class.java).toList()
                database.watchedEpisodeDao().insertAll(items)
            }
            // Restore watch progress
            (dataMap["watchProgress"] as? List<*>)?.let { list ->
                val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.WatchProgress>::class.java).toList()
                database.watchProgressDao().insertAll(items)
            }
            // Restore smart playlists
            (dataMap["smartPlaylists"] as? List<*>)?.let { list ->
                val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.SmartPlaylist>::class.java).toList()
                database.smartPlaylistDao().insertAll(items)
            }
            // Restore profiles
            (dataMap["profiles"] as? List<*>)?.let { list ->
                val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.Profile>::class.java).toList()
                database.profileDao().insertAll(items)
            }
            // Restore cached meta
            (dataMap["cachedMeta"] as? List<*>)?.let { list ->
                val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.CachedMeta>::class.java).toList()
                database.cachedMetaDao().insertAll(items)
            }

            // Restore preferences
            (dataMap["preferences"] as? Map<String, Any>)?.let { prefs ->
                prefs["theme"]?.let { preferencesManager.setTheme(it.toString()) }
                prefs["debridKey"]?.let { preferencesManager.setDebridKey(it.toString()) }
                prefs["currentProfile"]?.let { preferencesManager.setCurrentProfile(it.toString()) }
                prefs["hindiPriority"]?.let { preferencesManager.setHindiPriority(it.toString().toBoolean()) }
                prefs["autoPlayNext"]?.let { preferencesManager.setAutoPlayNext(it.toString().toBoolean()) }
                prefs["parentalControl"]?.let { preferencesManager.setParentalControl(it.toString().toBoolean()) }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun factoryReset(context: Context) {
        // Clear all tables
        database.addonDao().deleteAll()
        database.libraryDao().deleteAll()
        database.watchlistDao().deleteAll()
        database.historyDao().deleteAll()
        database.watchedEpisodeDao().deleteAll()
        database.watchProgressDao().deleteAll()
        database.smartPlaylistDao().deleteAll()
        database.profileDao().deleteAll()
        database.cachedMetaDao().deleteAll()

        // Clear DataStore
        preferencesManager.clearAll()
    }

    data class ProfileUiState(
        val theme: String = "dark",
        val hindiPriority: Boolean = true,
        val autoPlayNext: Boolean = false,
        val parentalControl: Boolean = false,
        val currentProfile: String = "default"
    )
}
