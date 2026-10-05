package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Song
import com.example.ui.components.MusicVisualizer
import com.example.ui.theme.AppleDarkSurface
import com.example.ui.theme.AppleMusicRed
import com.example.util.PerformanceMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    downloadedIds: Set<Int>,
    favorites: Set<Int>,
    recentlyPlayed: List<Song>,
    performanceMode: PerformanceMode,
    onSongClick: (Song) -> Unit,
    onPlayFirst: () -> Unit,
    onShuffleAll: () -> Unit,
    onSetPerformanceMode: (PerformanceMode) -> Unit,
    onOpenSettings: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDateStr = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
    var showPerfMenu by remember { mutableStateOf(false) }

    val hindiPunjabiSongs = remember(songs) {
        songs.filter { it.language.equals("Hindi", ignoreCase = true) || it.language.equals("Punjabi", ignoreCase = true) || it.genre.contains("Bollywood", ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Hero Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
            ) {
                // Background image & gradient
                AsyncImage(
                    model = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/54/50/75/54507577-9e1d-c2c0-79dd-81e65aa7fb9d/197342550925_cover.jpg/1000x1000bb.jpg",
                    contentDescription = "Header artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color(0xFFFA2D48).copy(alpha = 0.55f),
                                    Color(0xFF000000)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: Apple Music logo, Performance Mode, Settings Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppleMusicRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "♫",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apple Music",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Performance Mode Selector
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { showPerfMenu = true }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = if (performanceMode == PerformanceMode.HIGH) AppleMusicRed else Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (performanceMode) {
                                            PerformanceMode.HIGH -> "120 FPS"
                                            PerformanceMode.BALANCED -> "60 FPS"
                                            PerformanceMode.BATTERY_SAVER -> "Power Save"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = showPerfMenu,
                                    onDismissRequest = { showPerfMenu = false },
                                    modifier = Modifier.background(Color(0xFF222226))
                                ) {
                                    PerformanceMode.values().forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = mode.displayName,
                                                        fontWeight = if (mode == performanceMode) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (mode == performanceMode) AppleMusicRed else Color.White
                                                    )
                                                    Text(
                                                        text = mode.description,
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.5f)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                onSetPerformanceMode(mode)
                                                showPerfMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Settings Button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable(onClick = onOpenSettings),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Billboard title & Play/Shuffle buttons
                    Column {
                        Text(
                            text = "Today's Hits & Discovery",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Updated $currentDateStr • Powered by Apple Music & Gemini AI",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Button(
                                onClick = onPlayFirst,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.22f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("home_header_play")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Play",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Button(
                                onClick = onShuffleAll,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.22f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("home_header_shuffle")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Shuffle",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Jump Back In (Recently Played)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Jump Back In",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)
                ) {
                    items(recentlyPlayed.take(8), key = { "rp_${it.id}" }) { song ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { onSongClick(song) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = song.artwork,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = song.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Top Indian & Bollywood Hits Shelf
        if (hindiPunjabiSongs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Top Bollywood & Punjabi Hits",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)
                ) {
                    items(hindiPunjabiSongs, key = { "desi_${it.id}" }) { song ->
                        Column(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { onSongClick(song) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(135.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = song.artwork,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = song.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Top Songs Header
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Songs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${songs.size} tracks",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Song Rows with complete action menus
        itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
            val isCurrent = song.id == currentSong?.id
            val isDownloaded = downloadedIds.contains(song.id)
            val isFav = favorites.contains(song.id)
            var showSongMenu by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song) }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .testTag("song_item_${song.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Song rank number
                Text(
                    text = "${index + 1}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrent) AppleMusicRed else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.width(28.dp)
                )

                // Artwork + visualizer
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppleDarkSurface)
                ) {
                    AsyncImage(
                        model = song.artwork,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isCurrent) {
                        MusicVisualizer(isPlaying = isPlaying)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            fontSize = 15.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) AppleMusicRed else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isDownloaded) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Offline ready",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} • ${song.album}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { showSongMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Options",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSongMenu,
                        onDismissRequest = { showSongMenu = false },
                        modifier = Modifier.background(Color(0xFF222226))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play Next", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White) },
                            onClick = {
                                onPlayNext(song)
                                showSongMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Queue", color = Color.White) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = Color.White) },
                            onClick = {
                                onAddToQueue(song)
                                showSongMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                            onClick = {
                                onAddToPlaylist(song)
                                showSongMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isFav) "Remove Favorite" else "Favorite", color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = AppleMusicRed
                                )
                            },
                            onClick = {
                                onToggleFavorite(song)
                                showSongMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isDownloaded) "Downloaded Offline" else "Download Offline", color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (isDownloaded) Color(0xFF22C55E) else AppleMusicRed
                                )
                            },
                            onClick = {
                                if (!isDownloaded) onDownloadSong(song)
                                showSongMenu = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}
