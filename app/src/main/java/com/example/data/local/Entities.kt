package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val streamUrl: String,
    val name: String,
    val group: String,
    val logoUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_channels")
data class RecentEntity(
    @PrimaryKey
    val streamUrl: String,
    val name: String,
    val group: String,
    val logoUrl: String,
    val lastWatchedAt: Long = System.currentTimeMillis()
)
