#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "🚀 Fixing PreferencesManager.kt and AddonsViewModel.kt..."

# Ensure we're in the project root (adjust if needed)
cd /sdcard/ultrabuild/MyNewApp

# ============================================================
# 1. OVERWRITE PreferencesManager.kt
# ============================================================
cat > app/src/main/java/com/ultrastream/app/data/preferences/PreferencesManager.kt << 'EOF'
package com.ultrastream.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val DEBRID_KEY = stringPreferencesKey("debrid_key")
        private val DEBRID_PROVIDER_KEY = stringPreferencesKey("debrid_provider")
        private val CURRENT_PROFILE_KEY = stringPreferencesKey("current_profile")
        private val HINDI_PRIORITY_KEY = booleanPreferencesKey("hindi_priority")
        private val AUTO_PLAY_NEXT_KEY = booleanPreferencesKey("auto_play_next")
        private val PARENTAL_CONTROL_KEY = booleanPreferencesKey("parental_control")
        private val PARENTAL_RATING_KEY = stringPreferencesKey("parental_rating")
        private val SUBTITLE_LANGUAGE_KEY = stringPreferencesKey("subtitle_language")
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    fun getTheme(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "dark"
    }

    suspend fun setDebridKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[DEBRID_KEY] = key
        }
    }

    fun getDebridKey(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEBRID_KEY] ?: ""
    }

    suspend fun setDebridProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[DEBRID_PROVIDER_KEY] = provider
        }
    }

    fun getDebridProvider(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEBRID_PROVIDER_KEY] ?: "realdebrid"
    }

    suspend fun setCurrentProfile(profileId: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_PROFILE_KEY] = profileId
        }
    }

    fun getCurrentProfile(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_PROFILE_KEY] ?: "default"
    }

    suspend fun setHindiPriority(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HINDI_PRIORITY_KEY] = enabled
        }
    }

    fun getHindiPriority(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HINDI_PRIORITY_KEY] ?: true
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_NEXT_KEY] = enabled
        }
    }

    fun getAutoPlayNext(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_PLAY_NEXT_KEY] ?: false
    }

    suspend fun setParentalControl(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PARENTAL_CONTROL_KEY] = enabled
        }
    }

    fun getParentalControl(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PARENTAL_CONTROL_KEY] ?: false
    }

    suspend fun setParentalRating(rating: String) {
        context.dataStore.edit { preferences ->
            preferences[PARENTAL_RATING_KEY] = rating
        }
    }

    fun getParentalRating(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PARENTAL_RATING_KEY] ?: "PG-13"
    }

    suspend fun setSubtitleLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_LANGUAGE_KEY] = language
        }
    }

    fun getSubtitleLanguage(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SUBTITLE_LANGUAGE_KEY] ?: "English"
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
EOF

echo "✅ PreferencesManager.kt written."

# ============================================================
# 2. OVERWRITE AddonsViewModel.kt
# ============================================================
cat > app/src/main/java/com/ultrastream/app/ui/screens/addons/AddonsViewModel.kt << 'EOF'
package com.ultrastream.app.ui.screens.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.models.Catalog
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.data.repository.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddonsViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddonsUiState())
    val uiState: StateFlow<AddonsUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    init {
        loadAddons()
        observeDebridKey()
        observeDebridProvider()
    }

    private fun observeDebridKey() {
        viewModelScope.launch {
            preferencesManager.getDebridKey().collect { key ->
                _uiState.value = _uiState.value.copy(debridKey = key)
            }
        }
    }

    private fun observeDebridProvider() {
        viewModelScope.launch {
            preferencesManager.getDebridProvider().collect { provider ->
                _uiState.value = _uiState.value.copy(debridProvider = provider)
            }
        }
    }

    fun loadAddons() {
        viewModelScope.launch {
            val addons = addonRepository.getAllAddons()
            _uiState.value = _uiState.value.copy(addons = addons)
        }
    }

    suspend fun installAddon(rawUrl: String): Boolean {
        var safeUrl = rawUrl.trim()
        if (safeUrl.startsWith("stremio://")) {
            safeUrl = safeUrl.replace("stremio://", "https://")
        } else if (!safeUrl.startsWith("http")) {
            safeUrl = "https://$safeUrl"
        }
        if (safeUrl.endsWith("/")) {
            safeUrl = safeUrl.dropLast(1)
        }
        val addon = addonRepository.installAddon(safeUrl)
        if (addon != null) {
            loadAddons()
            return true
        }
        return false
    }

    suspend fun toggleAddon(id: String, enabled: Boolean) {
        addonRepository.toggleAddon(id, enabled)
        loadAddons()
    }

    suspend fun removeAddon(id: String) {
        addonRepository.removeAddon(id)
        loadAddons()
    }

    suspend fun saveDebridKey(key: String) {
        preferencesManager.setDebridKey(key)
        _uiState.value = _uiState.value.copy(debridKey = key)
    }

    suspend fun saveDebridProvider(provider: String) {
        preferencesManager.setDebridProvider(provider)
        _uiState.value = _uiState.value.copy(debridProvider = provider)
    }

    fun exportAddonsJson(): String {
        return try {
            val addons = _uiState.value.addons
            val exportList = addons.map {
                val catAdapter = moshi.adapter<List<Catalog>>(
                    Types.newParameterizedType(List::class.java, Catalog::class.java)
                )
                val parsedCatalogs = try { catAdapter.fromJson(it.catalogs) } catch (e: Exception) { emptyList() }
                StremioAddonExport(it.id, it.url, it.name, parsedCatalogs, it.enabled, it.required)
            }
            val type = Types.newParameterizedType(List::class.java, StremioAddonExport::class.java)
            moshi.adapter<List<StremioAddonExport>>(type).toJson(exportList)
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun importAddonsJson(json: String): Boolean {
        return try {
            val type = Types.newParameterizedType(List::class.java, StremioAddonExport::class.java)
            val importList = moshi.adapter<List<StremioAddonExport>>(type).fromJson(json) ?: return false

            val newAddons = importList.map {
                val catAdapter = moshi.adapter<List<Catalog>>(
                    Types.newParameterizedType(List::class.java, Catalog::class.java)
                )
                val catsJson = catAdapter.toJson(it.catalogs ?: emptyList())
                Addon(it.id, it.url, it.name, catsJson, it.enabled, it.required)
            }
            addonRepository.insertRawAddons(newAddons)
            loadAddons()
            true
        } catch (e: Exception) {
            false
        }
    }

    data class AddonsUiState(
        val addons: List<Addon> = emptyList(),
        val debridKey: String = "",
        val debridProvider: String = "realdebrid"
    )
}

data class StremioAddonExport(
    val id: String,
    val url: String,
    val name: String,
    val catalogs: List<Catalog>? = emptyList(),
    val enabled: Boolean = true,
    val required: Boolean = false
)
EOF

echo "✅ AddonsViewModel.kt written."

# ============================================================
# 3. Git commit and push
# ============================================================
git add app/src/main/java/com/ultrastream/app/data/preferences/PreferencesManager.kt
git add app/src/main/java/com/ultrastream/app/ui/screens/addons/AddonsViewModel.kt
git commit -m "Fix: Corrected PreferencesManager and AddonsViewModel syntax errors"
git push origin main

echo "🎉 Both files fixed and pushed successfully!"
