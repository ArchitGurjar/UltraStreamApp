package com.ultrastream.app.data.repository

import com.ultrastream.app.data.dao.AddonDao
import com.ultrastream.app.data.models.Addon
import com.ultrastream.app.data.models.Catalog
import com.ultrastream.app.network.StremioApi
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

        val netCatalogs = manifest.catalogs ?: emptyList()
        val mappedCatalogs = netCatalogs.map { netCat ->
            Catalog(
                type = netCat.type,
                id = netCat.id,
                name = netCat.name,
                extraSupported = netCat.extraSupported,
                extra = netCat.extra?.map { 
                    com.ultrastream.app.data.models.Extra(
                        name = it.name,
                        isRequired = it.isRequired,
                        options = it.options
                    )
                }
            )
        }

        val catalogsJson = mappedCatalogs.toString()
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
}
