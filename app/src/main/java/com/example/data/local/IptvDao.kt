package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE streamUrl = :streamUrl")
    suspend fun removeFavorite(streamUrl: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE streamUrl = :streamUrl)")
    suspend fun isFavorite(streamUrl: String): Boolean

    @Query("SELECT * FROM recent_channels ORDER BY lastWatchedAt DESC LIMIT 20")
    fun getRecentChannels(): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordRecent(recent: RecentEntity)

    @Query("DELETE FROM recent_channels")
    suspend fun clearRecents()
}
