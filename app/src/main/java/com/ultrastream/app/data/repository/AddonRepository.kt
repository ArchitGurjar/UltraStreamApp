package com.ultrastream.app.data.repository

import com.ultrastream.app.data.dao.AddonDao
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.models.Catalog
import com.ultrastream.app.network.StremioApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddonRepository @Inject constructor(
    private val addonDao: AddonDao,
    private val stremioApi: StremioApi
) {

    suspend fun installAddon(url: String): Addon? {
        val manifest = try {
            stremioApi.getManifest(url)
        } catch (e: Exception) {
            return null
        }
        if (manifest.id.isBlank()) return null

        val existing = addonDao.getById(manifest.id)
        if (existing != null) return existing

        val catalogsJson = convertCatalogsToJson(manifest.catalogs ?: emptyList())
        val addon = Addon(
            id = manifest.id,
            url = url,
            name = manifest.name ?: manifest.id,
            catalogs = catalogsJson,
            enabled = true,
            required = false
        )
        addonDao.insert(addon)
        return addon
    }

    suspend fun getAllAddons(): List<Addon> = addonDao.getAll()

    suspend fun getEnabledAddons(): List<Addon> {
        return addonDao.getAll().filter { it.enabled }
    }

    suspend fun toggleAddon(id: String, enabled: Boolean) {
        addonDao.updateEnabled(id, enabled)
    }

    suspend fun removeAddon(id: String) {
        addonDao.deleteById(id)
    }

    private fun convertCatalogsToJson(catalogs: List<Catalog>): String {
        // Use Moshi or Gson, but for simplicity we'll use a simple JSON string
        // Since we have Moshi in Converters, we can use that.
        // For now, we'll use a placeholder. In real implementation, use Moshi.
        // But to avoid dependency cycle, we'll use Moshi from network module.
        // We'll create a utility function in a separate class.
        return catalogs.toString() // Placeholder, will be properly serialized later
    }
}
