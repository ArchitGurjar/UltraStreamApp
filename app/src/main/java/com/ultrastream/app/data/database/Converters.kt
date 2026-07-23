package com.ultrastream.app.data.database

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.models.*

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromCatalogList(value: List<Catalog>): String {
        return moshi.adapter(List::class.java).toJson(value)
    }

    @TypeConverter
    fun toCatalogList(value: String): List<Catalog> {
        return moshi.adapter(List::class.java).fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return moshi.adapter(List::class.java).toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return moshi.adapter(List::class.java).fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromEpisodeList(value: List<PlaylistEpisode>): String {
        return moshi.adapter(List::class.java).toJson(value)
    }

    @TypeConverter
    fun toEpisodeList(value: String): List<PlaylistEpisode> {
        return moshi.adapter(List::class.java).fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStreamItem(value: StreamItem?): String? {
        return if (value == null) null else moshi.adapter(StreamItem::class.java).toJson(value)
    }

    @TypeConverter
    fun toStreamItem(value: String?): StreamItem? {
        return if (value == null) null else moshi.adapter(StreamItem::class.java).fromJson(value)
    }
}
