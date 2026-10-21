package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryChipsBar
import com.example.ui.components.ChannelCard
import com.example.ui.components.ChannelListItem
import com.example.ui.player.VideoPlayerView
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.IptvViewModel
import com.example.ui.viewmodel.ViewDisplayMode

@Composable
fun HomeScreen(
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val currentChannel by viewModel.currentChannel.collectAsState()
    val currentChannelEpg by viewModel.currentChannelEpg.collectAsState()
    val selectedEpgChannel by viewModel.selectedEpgChannel.collectAsState()
    val epgMap by viewModel.epgMap.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val categories by viewModel.categoryListState.collectAsState()
    val categoryCounts by viewModel.categoryCountsState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val lastEpgSyncTime by viewModel.lastEpgSyncTime.collectAsState()
    val selectedTimezone by viewModel.selectedTimezone.collectAsState()

    var showPlaylistInfoSheet by remember { mutableStateOf(false) }
    var showTimezoneDialog by remember { mutableStateOf(false) }

    // Rotate animation for refresh icon when syncing
    val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // BackHandler for exiting fullscreen
    if (isFullscreen) {
        BackHandler {
            viewModel.setFullscreen(false)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayerView(
                channel = currentChannel,
                isFullscreen = true,
                epgInfo = currentChannelEpg,
                onOpenEpg = { currentChannel?.let { viewModel.openChannelEpg(it) } },
                onToggleFullscreen = { viewModel.setFullscreen(false) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onPreviousChannel = { viewModel.playPreviousChannel() },
                onNextChannel = { viewModel.playNextChannel() },
                onBack = { viewModel.setFullscreen(false) },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Standard Portrait Mode
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // TOP APP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LatinPlayer",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanAccent,
                                contentColor = BackgroundDark
                            ) {
                                Text(
                                    text = "IPTV",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${channels.size} canales en vivo",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Timezone Selector Button
                    Surface(
                        onClick = { showTimezoneDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariantDark,
                        contentColor = CyanAccent,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("timezone_selector_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Zona Horaria",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedTimezone.shortLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // View mode toggle (Grid / List)
                    IconButton(
                        onClick = { viewModel.toggleViewMode() },
                        modifier = Modifier.testTag("view_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (viewMode == ViewDisplayMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Cambiar vista",
                            tint = TextSecondary
                        )
                    }

                    // Sync button
                    IconButton(
                        onClick = { viewModel.refreshPlaylist() },
                        enabled = !isLoading,
                        modifier = Modifier.testTag("refresh_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar canales",
                            tint = if (isLoading) CyanAccent else TextSecondary,
                            modifier = if (isLoading) Modifier.rotate(rotationAngle) else Modifier
                        )
                    }

                    // Playlist info button
                    IconButton(
                        onClick = { showPlaylistInfoSheet = true },
                        modifier = Modifier.testTag("info_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info de lista",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // PINNED VIDEO PLAYER AT TOP (16:9 ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .testTag("top_player_container")
            ) {
                VideoPlayerView(
                    channel = currentChannel,
                    isFullscreen = false,
                    epgInfo = currentChannelEpg,
                    onOpenEpg = { currentChannel?.let { viewModel.openChannelEpg(it) } },
                    onToggleFullscreen = { viewModel.setFullscreen(true) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPreviousChannel = { viewModel.playPreviousChannel() },
                    onNextChannel = { viewModel.playNextChannel() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_text_field"),
                placeholder = {
                    Text(
                        text = "Buscar canal, país o temática...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = if (searchQuery.isNotEmpty()) CyanAccent else TextMuted
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // CATEGORY CHIPS BAR
            CategoryChipsBar(
                categories = categories,
                selectedCategory = selectedCategory,
                categoryCounts = categoryCounts,
                onSelectCategory = { viewModel.selectCategory(it) }
            )

            // CHANNEL RESULTS HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "Resultados de búsqueda" else selectedCategory,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${filteredChannels.size} canales",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // CHANNELS LIST OR GRID
            if (filteredChannels.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedCategory.contains("Favoritos")) {
                                "No tienes canales favoritos guardados"
                            } else {
                                "No se encontraron canales"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedCategory.contains("Favoritos")) {
                                "Toca el ícono de estrella en cualquier canal para agregarlo a tus favoritos."
                            } else {
                                "Intenta con otro término de búsqueda o selecciona otra categoría."
                            },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.clearSearch()
                                viewModel.selectCategory("Todos")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceElevated,
                                contentColor = CyanAccent
                            )
                        ) {
                            Text("Ver todos los canales")
                        }
                    }
                }
            } else {
                when (viewMode) {
                    ViewDisplayMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("channel_lazy_column"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredChannels, key = { it.id }) { channel ->
                                val channelEpg = remember(channel.tvgId, epgMap) {
                                    viewModel.getChannelEpg(channel)
                                }
                                ChannelListItem(
                                    channel = channel,
                                    isSelected = currentChannel?.streamUrl == channel.streamUrl,
                                    currentProgram = channelEpg.currentProgram,
                                    timeZone = channelEpg.timeZone,
                                    onSelect = { viewModel.playChannel(channel) },
                                    onToggleFavorite = { viewModel.toggleFavorite(channel) },
                                    onOpenEpg = { viewModel.openChannelEpg(channel) }
                                )
                            }
                        }
                    }

                    ViewDisplayMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("channel_lazy_grid"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredChannels, key = { it.id }) { channel ->
                                val channelEpg = remember(channel.tvgId, epgMap, selectedTimezone) {
                                    viewModel.getChannelEpg(channel)
                                }
                                ChannelCard(
                                    channel = channel,
                                    isSelected = currentChannel?.streamUrl == channel.streamUrl,
                                    currentProgram = channelEpg.currentProgram,
                                    timeZone = channelEpg.timeZone,
                                    onSelect = { viewModel.playChannel(channel) },
                                    onToggleFavorite = { viewModel.toggleFavorite(channel) },
                                    onOpenEpg = { viewModel.openChannelEpg(channel) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Channel EPG Schedule Sheet
    selectedEpgChannel?.let { targetChannel ->
        val epgInfo = remember(targetChannel.tvgId, epgMap, selectedTimezone) {
            viewModel.getChannelEpg(targetChannel)
        }
        ChannelEpgScheduleSheet(
            channel = targetChannel,
            epgInfo = epgInfo,
            selectedTimezone = selectedTimezone,
            onSelectTimezone = { viewModel.setTimezone(it) },
            onDismiss = { viewModel.closeChannelEpg() }
        )
    }

    // Timezone Selector Dialog
    if (showTimezoneDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimezoneDialog = false },
            title = {
                Text(
                    text = "Ajuste de Zona Horaria (EPG)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Selecciona la hora de tu país para ajustar los horarios de toda la programación:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    com.example.data.model.EpgTimeZone.entries.forEach { tz ->
                        val isSelected = tz == selectedTimezone
                        Surface(
                            onClick = {
                                viewModel.setTimezone(tz)
                                showTimezoneDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyanAccent else SurfaceVariantDark,
                            contentColor = if (isSelected) BackgroundDark else TextPrimary,
                            border = if (isSelected) null else BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = tz.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tz.countries,
                                    fontSize = 11.sp,
                                    color = if (isSelected) BackgroundDark.copy(alpha = 0.8f) else TextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showTimezoneDialog = false }) {
                    Text("Cerrar", color = CyanAccent)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Playlist Info Bottom Sheet
    if (showPlaylistInfoSheet) {
        PlaylistInfoSheet(
            channelCount = channels.size,
            lastSyncTime = lastSyncTime,
            lastEpgSyncTime = lastEpgSyncTime,
            isLoading = isLoading,
            syncError = syncError,
            onRefresh = { viewModel.refreshPlaylist() },
            onDismiss = { showPlaylistInfoSheet = false }
        )
    }
}
