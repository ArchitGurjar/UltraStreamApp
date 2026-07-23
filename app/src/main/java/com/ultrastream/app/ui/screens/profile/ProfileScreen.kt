package com.ultrastream.app.ui.screens.profile

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ultrastream.app.data.database.AppDatabase
import com.ultrastream.app.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Launcher for importing backup
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

    // Permission launcher (for Android 10+ we use MediaStore, for older we need WRITE_EXTERNAL_STORAGE)
    // We'll handle export via MediaStore for Android 10+, else via FileProvider.

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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val database: AppDatabase
) : androidx.lifecycle.ViewModel() {

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
        return withContext(Dispatchers.IO) {
            try {
                // Gather all data
                val dataMap = mutableMapOf<String, Any>()
                // Addons
                dataMap["addons"] = database.addonDao().getAll()
                dataMap["library"] = database.libraryDao().getAll()
                dataMap["watchlist"] = database.watchlistDao().getAll()
                dataMap["history"] = database.historyDao().getAll()
                dataMap["watchedEpisodes"] = database.watchedEpisodeDao().getAll()
                dataMap["watchProgress"] = database.watchProgressDao().getAll()
                dataMap["smartPlaylists"] = database.smartPlaylistDao().getAll()
                dataMap["profiles"] = database.profileDao().getAll()
                dataMap["cachedMeta"] = database.cachedMetaDao().getAll()

                // Preferences
                val prefs = mutableMapOf<String, Any>()
                prefs["theme"] = runBlocking { preferencesManager.getTheme().collect { it } }
                prefs["debridKey"] = runBlocking { preferencesManager.getDebridKey().collect { it } }
                prefs["currentProfile"] = runBlocking { preferencesManager.getCurrentProfile().collect { it } }
                prefs["hindiPriority"] = runBlocking { preferencesManager.getHindiPriority().collect { it } }
                prefs["autoPlayNext"] = runBlocking { preferencesManager.getAutoPlayNext().collect { it } }
                prefs["parentalControl"] = runBlocking { preferencesManager.getParentalControl().collect { it } }
                dataMap["preferences"] = prefs

                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.toJson(dataMap)

                // Save to external storage via MediaStore (Android 10+)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "ultrastream_backup.json")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        "Documents/UltraStream"
                    } else {
                        null
                    })
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                        return@withContext true
                    }
                }
                // Fallback: write to cache and share via FileProvider
                val cacheFile = File(context.cacheDir, "ultrastream_backup.json")
                cacheFile.writeText(json)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        cacheFile
                    ))
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun importData(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val inputStream = resolver.openInputStream(uri) ?: return@withContext false
                val reader = InputStreamReader(inputStream)
                val gson = Gson()
                val dataMap = gson.fromJson(reader, Map::class.java) as Map<String, Any>

                // Restore addons
                dataMap["addons"]?.let { list ->
                    val addons = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.Addon>::class.java).toList()
                    database.addonDao().insertAll(addons)
                }
                // Restore library
                dataMap["library"]?.let { list ->
                    val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.LibraryItem>::class.java).toList()
                    database.libraryDao().insertAll(items)
                }
                // Restore watchlist
                dataMap["watchlist"]?.let { list ->
                    val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.WatchlistItem>::class.java).toList()
                    database.watchlistDao().insertAll(items)
                }
                // Restore history
                dataMap["history"]?.let { list ->
                    val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.HistoryItem>::class.java).toList()
                    database.historyDao().insertAll(items)
                }
                // Restore watched episodes
                dataMap["watchedEpisodes"]?.let { list ->
                    val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.WatchedEpisode>::class.java).toList()
                    database.watchedEpisodeDao().insertAll(items)
                }
                // Restore watch progress
                dataMap["watchProgress"]?.let { list ->
                    val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.WatchProgress>::class.java).toList()
                    database.watchProgressDao().insertAll(items)
                }
                // Restore smart playlists
                dataMap["smartPlaylists"]?.let { list ->
                    val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.SmartPlaylist>::class.java).toList()
                    database.smartPlaylistDao().insertAll(items)
                }
                // Restore profiles
                dataMap["profiles"]?.let { list ->
                    val items = gson.fromJson(gson.toJson(list), Array<com.ultrastream.app.data.models.Profile>::class.java).toList()
                    database.profileDao().insertAll(items)
                }
                // Restore cached meta
                dataMap["cachedMeta"]?.let { list ->
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
    }

    suspend fun factoryReset(context: Context) {
        withContext(Dispatchers.IO) {
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
    }

    data class ProfileUiState(
        val theme: String = "dark",
        val hindiPriority: Boolean = true,
        val autoPlayNext: Boolean = false,
        val parentalControl: Boolean = false,
        val currentProfile: String = "default"
    )
}

// Add missing deleteAll methods in DAOs if not present. We'll add them in the script.
