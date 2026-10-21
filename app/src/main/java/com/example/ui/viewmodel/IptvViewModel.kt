package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.IptvDatabase
import com.example.data.model.Channel
import com.example.data.model.ChannelEpgInfo
import com.example.data.model.EpgTimeZone
import com.example.data.repository.IptvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ViewDisplayMode {
    GRID,
    LIST
}

class IptvViewModel(application: Application) : AndroidViewModel(application) {

    private val database = IptvDatabase.getDatabase(application)
    private val repository = IptvRepository(application, database.iptvDao())
    private val prefs = application.getSharedPreferences("latinplayer_iptv_prefs", Context.MODE_PRIVATE)

    val isLoading = repository.isLoading
    val isEpgLoading = repository.isEpgLoading
    val syncError = repository.syncError
    val lastSyncTime = repository.lastSyncTime
    val lastEpgSyncTime = repository.lastEpgSyncTime
    val epgMap = repository.epgMap

    // Persistent timezone selection (defaults to GMT-5 for Perú, Colombia, Ecuador)
    private val _selectedTimezone = MutableStateFlow(loadInitialTimezone())
    val selectedTimezone: StateFlow<EpgTimeZone> = _selectedTimezone.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewDisplayMode.LIST)
    val viewMode: StateFlow<ViewDisplayMode> = _viewMode.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _selectedEpgChannel = MutableStateFlow<Channel?>(null)
    val selectedEpgChannel: StateFlow<Channel?> = _selectedEpgChannel.asStateFlow()

    val favorites = repository.favoritesFlow
    val recents = repository.recentsFlow

    val channels: StateFlow<List<Channel>> = repository.channelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time EPG for the currently playing channel with timezone applied
    val currentChannelEpg: StateFlow<ChannelEpgInfo?> = combine(
        _currentChannel,
        epgMap,
        _selectedTimezone
    ) { channel, _, tz ->
        if (channel != null) {
            val resolvedTz = tz.resolveTimeZone(channel.group)
            repository.getEpgForChannel(channel, resolvedTz)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Category lists and counts
    val categoryListState: StateFlow<List<String>> = channels.combine(favorites) { channelList, favList ->
        val groups = channelList.map { it.group }.distinct().sorted()
        val fixed = mutableListOf("Todos")
        if (favList.isNotEmpty()) {
            fixed.add("⭐ Favoritos")
        }
        fixed.add("🕒 Recientes")

        // Prioritize local groups like Perú, Deportes, Cine
        val priority = listOf("Perú", "Deportes Perú", "Deportes Int.", "Cine y Series", "Noticias", "Kids")
        val reordered = mutableListOf<String>()
        reordered.addAll(fixed)
        priority.forEach { p ->
            if (groups.contains(p) && !reordered.contains(p)) {
                reordered.add(p)
            }
        }
        groups.forEach { g ->
            if (!reordered.contains(g)) {
                reordered.add(g)
            }
        }
        reordered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Todos", "🕒 Recientes"))

    val categoryCountsState: StateFlow<Map<String, Int>> = combine(channels, favorites, recents) { chList, favList, recList ->
        val counts = mutableMapOf<String, Int>()
        counts["Todos"] = chList.size
        counts["⭐ Favoritos"] = favList.size
        counts["🕒 Recientes"] = recList.size
        chList.groupBy { it.group }.forEach { (grp, list) ->
            counts[grp] = list.size
        }
        counts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered channels based on search and category
    val filteredChannels: StateFlow<List<Channel>> = combine(
        channels,
        _searchQuery,
        _selectedCategory,
        recents
    ) { chList, query, category, recList ->
        var list = chList

        when (category) {
            "Todos" -> { /* No category filter */ }
            "⭐ Favoritos" -> {
                list = list.filter { it.isFavorite }
            }
            "🕒 Recientes" -> {
                val recentUrls = recList.map { it.streamUrl }
                val channelMap = chList.associateBy { it.streamUrl }
                list = recentUrls.mapNotNull { channelMap[it] }
            }
            else -> {
                list = list.filter { it.group.equals(category, ignoreCase = true) }
            }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { ch ->
                ch.cleanName.lowercase().contains(q) ||
                ch.number.lowercase().contains(q) ||
                ch.group.lowercase().contains(q) ||
                ch.name.lowercase().contains(q)
            }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initialize()
            channels.collect { chList ->
                if (_currentChannel.value == null && chList.isNotEmpty()) {
                    val initial = chList.firstOrNull { it.group == "Perú" } ?: chList.first()
                    _currentChannel.value = initial
                    repository.recordRecent(initial)
                }
            }
        }
    }

    private fun loadInitialTimezone(): EpgTimeZone {
        return try {
            val savedName = prefs.getString("selected_timezone", EpgTimeZone.GMT_MINUS_5.name)
            EpgTimeZone.valueOf(savedName ?: EpgTimeZone.GMT_MINUS_5.name)
        } catch (e: Exception) {
            EpgTimeZone.GMT_MINUS_5
        }
    }

    fun setTimezone(timezone: EpgTimeZone) {
        _selectedTimezone.value = timezone
        prefs.edit().putString("selected_timezone", timezone.name).apply()
    }

    fun getChannelEpg(channel: Channel): ChannelEpgInfo {
        val tz = _selectedTimezone.value.resolveTimeZone(channel.group)
        return repository.getEpgForChannel(channel, tz)
    }

    fun openChannelEpg(channel: Channel) {
        _selectedEpgChannel.value = channel
    }

    fun closeChannelEpg() {
        _selectedEpgChannel.value = null
    }

    fun playChannel(channel: Channel) {
        _currentChannel.value = channel
        viewModelScope.launch {
            repository.recordRecent(channel)
        }
    }

    fun playNextChannel() {
        val currentList = filteredChannels.value.ifEmpty { channels.value }
        val current = _currentChannel.value ?: return
        val index = currentList.indexOfFirst { it.streamUrl == current.streamUrl }
        if (index != -1 && currentList.isNotEmpty()) {
            val nextIndex = (index + 1) % currentList.size
            playChannel(currentList[nextIndex])
        }
    }

    fun playPreviousChannel() {
        val currentList = filteredChannels.value.ifEmpty { channels.value }
        val current = _currentChannel.value ?: return
        val index = currentList.indexOfFirst { it.streamUrl == current.streamUrl }
        if (index != -1 && currentList.isNotEmpty()) {
            val prevIndex = if (index - 1 < 0) currentList.size - 1 else index - 1
            playChannel(currentList[prevIndex])
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewDisplayMode.LIST) ViewDisplayMode.GRID else ViewDisplayMode.LIST
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun refreshPlaylist() {
        viewModelScope.launch {
            repository.refreshFromNetwork()
            repository.refreshEpg()
        }
    }
}
