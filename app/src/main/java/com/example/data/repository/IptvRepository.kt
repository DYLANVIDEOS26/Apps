package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.FavoriteEntity
import com.example.data.local.IptvDao
import com.example.data.local.RecentEntity
import com.example.data.model.Channel
import com.example.data.model.ChannelEpgInfo
import com.example.data.model.EpgProgram
import com.example.data.parser.M3uParser
import com.example.data.parser.XmlTvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

class IptvRepository(
    private val context: Context,
    private val iptvDao: IptvDao
) {
    companion object {
        const val DEFAULT_M3U_URL =
            "https://raw.githubusercontent.com/DYLANVIDEOS26/mis-listas-m3u/refs/heads/main/latinplayer_pro(epg_v1.0).m3u8"
        const val DEFAULT_EPG_URL =
            "https://epg.programadorx.cl/mdiaz/gratis.xml.gz"

        private const val CACHED_PLAYLIST_FILE = "cached_latinplayer.m3u"
        private const val CACHED_EPG_FILE = "cached_epg.xml.gz"
        private const val TAG = "IptvRepository"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _rawChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isEpgLoading = MutableStateFlow(false)
    val isEpgLoading: StateFlow<Boolean> = _isEpgLoading.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _lastEpgSyncTime = MutableStateFlow<Long?>(null)
    val lastEpgSyncTime: StateFlow<Long?> = _lastEpgSyncTime.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _epgMap = MutableStateFlow<Map<String, List<EpgProgram>>>(emptyMap())
    val epgMap: StateFlow<Map<String, List<EpgProgram>>> = _epgMap.asStateFlow()

    val favoritesFlow: Flow<List<FavoriteEntity>> = iptvDao.getAllFavorites()
    val recentsFlow: Flow<List<RecentEntity>> = iptvDao.getRecentChannels()

    // Channels with real-time favorite status applied
    val channelsFlow: Flow<List<Channel>> = combine(_rawChannels, favoritesFlow) { channels, favorites ->
        val favoriteUrls = favorites.map { it.streamUrl }.toSet()
        channels.map { ch ->
            ch.copy(isFavorite = favoriteUrls.contains(ch.streamUrl))
        }
    }

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            // First load from cached file if present, else fallback to bundled assets
            val cacheFile = File(context.cacheDir, CACHED_PLAYLIST_FILE)
            if (cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    val parsed = cacheFile.inputStream().use { M3uParser.parse(it) }
                    if (parsed.isNotEmpty()) {
                        _rawChannels.value = parsed
                        _lastSyncTime.value = cacheFile.lastModified()
                        Log.d(TAG, "Loaded ${parsed.size} channels from local cache")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading cached playlist", e)
                }
            }

            // If empty, load bundled asset
            if (_rawChannels.value.isEmpty()) {
                loadBundledAsset()
            }

            // Load cached EPG if available
            val epgCacheFile = File(context.cacheDir, CACHED_EPG_FILE)
            if (epgCacheFile.exists() && epgCacheFile.length() > 0) {
                try {
                    val parsedEpg = GZIPInputStream(FileInputStream(epgCacheFile)).use {
                        XmlTvParser.parse(it)
                    }
                    if (parsedEpg.isNotEmpty()) {
                        _epgMap.value = parsedEpg
                        _lastEpgSyncTime.value = epgCacheFile.lastModified()
                        Log.d(TAG, "Loaded EPG for ${parsedEpg.size} channels from local cache")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error loading cached EPG", e)
                }
            }

            // Fetch updates from network
            refreshFromNetwork(DEFAULT_M3U_URL)
            refreshEpg(DEFAULT_EPG_URL)
        }
    }

    private fun loadBundledAsset() {
        try {
            context.assets.open("default_channels.m3u").use { input ->
                val parsed = M3uParser.parse(input)
                if (parsed.isNotEmpty()) {
                    _rawChannels.value = parsed
                    Log.d(TAG, "Loaded ${parsed.size} channels from bundled assets")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bundled asset", e)
        }
    }

    suspend fun refreshFromNetwork(url: String = DEFAULT_M3U_URL) {
        withContext(Dispatchers.IO) {
            _isLoading.value = true
            _syncError.value = null
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; LatinPlayer IPTV)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBytes = response.body?.bytes()
                    if (responseBytes != null && responseBytes.isNotEmpty()) {
                        val parsed = M3uParser.parse(ByteArrayInputStream(responseBytes))
                        if (parsed.isNotEmpty()) {
                            _rawChannels.value = parsed
                            val now = System.currentTimeMillis()
                            _lastSyncTime.value = now

                            // Save to local cache
                            try {
                                val cacheFile = File(context.cacheDir, CACHED_PLAYLIST_FILE)
                                cacheFile.writeBytes(responseBytes)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to cache playlist", e)
                            }
                            Log.d(TAG, "Synced ${parsed.size} channels successfully from network")
                        }
                    }
                } else {
                    _syncError.value = "Error al descargar lista (${response.code})"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network sync failed, keeping local channels: ${e.message}")
                _syncError.value = e.localizedMessage ?: "Fallo de conexión"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun refreshEpg(url: String = DEFAULT_EPG_URL) {
        withContext(Dispatchers.IO) {
            _isEpgLoading.value = true
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; LatinPlayer IPTV EPG)")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        // Save cache
                        try {
                            val cacheFile = File(context.cacheDir, CACHED_EPG_FILE)
                            cacheFile.writeBytes(bytes)
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not save EPG cache", e)
                        }

                        val parsedEpg = GZIPInputStream(ByteArrayInputStream(bytes)).use {
                            XmlTvParser.parse(it)
                        }
                        if (parsedEpg.isNotEmpty()) {
                            _epgMap.value = parsedEpg
                            _lastEpgSyncTime.value = System.currentTimeMillis()
                            Log.d(TAG, "EPG synced successfully: ${parsedEpg.size} channels loaded")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "EPG network sync failed: ${e.message}")
            } finally {
                _isEpgLoading.value = false
            }
        }
    }

    fun getEpgForChannel(channel: Channel, timeZone: java.util.TimeZone): ChannelEpgInfo {
        val now = System.currentTimeMillis()
        val byId = if (channel.tvgId.isNotBlank()) _epgMap.value[channel.tvgId] else null
        val programs = byId
            ?: _epgMap.value.entries.firstOrNull { it.key.contains(channel.cleanName, ignoreCase = true) }?.value
            ?: emptyList()

        if (programs.isEmpty()) {
            return ChannelEpgInfo.generate24HourFallback(channel, timeZone)
        }

        val current = programs.firstOrNull { it.isCurrent(now) }
        val next = programs.firstOrNull { it.isUpcoming(now) }

        return ChannelEpgInfo(
            channelId = channel.tvgId.ifEmpty { channel.id },
            currentProgram = current,
            nextProgram = next,
            allPrograms = programs,
            timeZone = timeZone
        )
    }

    suspend fun toggleFavorite(channel: Channel) {
        withContext(Dispatchers.IO) {
            val exists = iptvDao.isFavorite(channel.streamUrl)
            if (exists) {
                iptvDao.removeFavorite(channel.streamUrl)
            } else {
                iptvDao.insertFavorite(
                    FavoriteEntity(
                        streamUrl = channel.streamUrl,
                        name = channel.cleanName,
                        group = channel.group,
                        logoUrl = channel.logoUrl
                    )
                )
            }
        }
    }

    suspend fun recordRecent(channel: Channel) {
        withContext(Dispatchers.IO) {
            iptvDao.recordRecent(
                RecentEntity(
                    streamUrl = channel.streamUrl,
                    name = channel.cleanName,
                    group = channel.group,
                    logoUrl = channel.logoUrl,
                    lastWatchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            iptvDao.clearRecents()
        }
    }
}
