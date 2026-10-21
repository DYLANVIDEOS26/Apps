package com.example.data.model

data class Channel(
    val id: String,
    val name: String,
    val cleanName: String,
    val number: String,
    val group: String,
    val logoUrl: String,
    val streamUrl: String,
    val tvgId: String = "",
    val isFavorite: Boolean = false,
    val isOnline: Boolean = true
)
