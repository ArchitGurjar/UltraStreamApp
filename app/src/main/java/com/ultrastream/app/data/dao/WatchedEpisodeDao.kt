package com.ultrastream.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultrastream.app.data.models.WatchedEpisode

@Dao
interface WatchedEpisodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(episode: WatchedEpisode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<WatchedEpisode>)

    @Query("SELECT * FROM watched_episodes WHERE episodeKey = :key")
    suspend fun getByKey(key: String): WatchedEpisode?

    @Query("SELECT * FROM watched_episodes")
    suspend fun getAll(): List<WatchedEpisode>

    @Delete
    suspend fun delete(episode: WatchedEpisode)

    @Query("DELETE FROM watched_episodes")
    suspend fun deleteAll()
}
