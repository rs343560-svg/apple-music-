package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.Playlist
import com.example.model.Song
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppleDarkSurface
import com.example.ui.theme.AppleMusicRed
import com.example.util.PerformanceMode

data class LibraryMenuItem(
    val title: String,
    val icon: ImageVector,
    val count: Int? = null
)

@Composable
fun LibraryScreen(
    songs: List<Song>,
    downloadedIds: Set<Int>,
    playlists: List<Playlist>,
    favorites: Set<Int>,
    recentlyPlayed: List<Song>,
    performanceMode: PerformanceMode,
    onSongClick: (Song) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onDeleteDownload: (Int) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveSongFromPlaylist: (String, Int) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var activePlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val downloadedSongs = remember(songs, downloadedIds) {
        songs.filter { downloadedIds.contains(it.id) }
    }

    val favoriteSongs = remember(songs, favorites) {
        songs.filter { favorites.contains(it.id) }
    }

    val activePlaylist = remember(playlists, activePlaylistId) {
        playlists.firstOrNull { it.id == activePlaylistId }
    }

    val playlistSongs = remember(activePlaylist, songs) {
        if (activePlaylist == null) emptyList()
        else {
            val songMap = songs.associateBy { it.id }
            activePlaylist.songIds.mapNotNull { songMap[it] }
        }
    }

    val displayedSongs = when {
        activePlaylistId != null -> playlistSongs
        selectedCategory == "Downloaded" -> downloadedSongs
        selectedCategory == "Favorites" -> favoriteSongs
        selectedCategory == "Recently Played" -> recentlyPlayed
        else -> songs
    }

    val menuItems = listOf(
        LibraryMenuItem("Playlists", Icons.AutoMirrored.Filled.QueueMusic, playlists.size),
        LibraryMenuItem("Favorites", Icons.Default.Star, favoriteSongs.size),
        LibraryMenuItem("Downloaded", Icons.Default.DownloadDone, downloadedSongs.size),
        LibraryMenuItem("Songs", Icons.Default.MusicNote, songs.size),
        LibraryMenuItem("Artists", Icons.Default.Person, songs.map { it.artist }.distinct().size),
        LibraryMenuItem("Albums", Icons.Default.Album, songs.map { it.album }.filter { it.isNotBlank() }.distinct().size)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Library Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (activePlaylistId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { activePlaylistId = null }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = activePlaylist?.name ?: "Playlist",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${playlistSongs.size} tracks",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Library",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        IconButton(onClick = { showCreatePlaylistDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Playlist",
                                tint = AppleMusicRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (selectedCategory != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtered by $selectedCategory (${displayedSongs.size})",
                                fontSize = 14.sp,
                                color = AppleMusicRed,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Show All",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clickable { selectedCategory = null }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Category Cards (when not drilling down into a playlist)
        if (activePlaylistId == null && selectedCategory == null) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = Color(0xFF18181C),
                    performanceMode = performanceMode
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        menuItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategory = item.title }
                                    .padding(vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = AppleMusicRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = item.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White,
                                        modifier = Modifier.padding(start = 14.dp)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.count != null) {
                                        Text(
                                            text = "${item.count}",
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            if (index < menuItems.lastIndex) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // Playlists category view
        if (selectedCategory == "Playlists" && activePlaylistId == null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Playlists",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "+ Create",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleMusicRed,
                        modifier = Modifier
                            .clickable { showCreatePlaylistDialog = true }
                            .padding(4.dp)
                    )
                }
            }

            items(playlists, key = { it.id }) { pl ->
                var showPlMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activePlaylistId = pl.id }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = AppleMusicRed,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = pl.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "${pl.songIds.size} songs",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showPlMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White.copy(alpha = 0.6f))
                        }
                        DropdownMenu(
                            expanded = showPlMenu,
                            onDismissRequest = { showPlMenu = false },
                            modifier = Modifier.background(Color(0xFF222226))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Playlist", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    onDeletePlaylist(pl.id)
                                    showPlMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        activePlaylistId != null -> "Songs"
                        selectedCategory == "Downloaded" -> "Offline Downloads"
                        selectedCategory == "Favorites" -> "Starred Favorites"
                        selectedCategory != null -> selectedCategory!!
                        else -> "Recently Added"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (selectedCategory == "Downloaded" && displayedSongs.isNotEmpty()) {
                    Text(
                        text = "${displayedSongs.size} tracks ready offline",
                        fontSize = 12.sp,
                        color = Color(0xFF22C55E)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Empty state
        if (displayedSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (selectedCategory) {
                                "Downloaded" -> Icons.Default.DownloadDone
                                "Favorites" -> Icons.Default.Star
                                else -> Icons.Default.MusicNote
                            },
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (selectedCategory) {
                                "Downloaded" -> "No Offline Downloads Yet"
                                "Favorites" -> "No Starred Favorites Yet"
                                else -> "No Songs in this Section"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = when (selectedCategory) {
                                "Downloaded" -> "Tap the download icon on any song to save it for offline listening"
                                "Favorites" -> "Star any song while listening to add it to your favorites"
                                else -> "Explore songs in Search or Home"
                            },
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }

        // Grid of songs
        val chunkedSongs = displayedSongs.chunked(2)
        items(chunkedSongs.size) { chunkIndex ->
            val pair = chunkedSongs[chunkIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pair.forEach { song ->
                    val isSongDownloaded = downloadedIds.contains(song.id)
                    val isFav = favorites.contains(song.id)
                    var showSongMenu by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSongClick(song) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = song.artwork,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )

                            // Top corner badges
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                if (isFav) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Favorite",
                                            tint = AppleMusicRed,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(24.dp))
                                }

                                if (isSongDownloaded) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Downloaded",
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showSongMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
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
                                    if (activePlaylistId != null) {
                                        DropdownMenuItem(
                                            text = { Text("Remove from Playlist", color = Color(0xFFEF4444)) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                            onClick = {
                                                onRemoveSongFromPlaylist(activePlaylistId!!, song.id)
                                                showSongMenu = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(if (isFav) "Unstar" else "Star Favorite", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = AppleMusicRed) },
                                        onClick = {
                                            onToggleFavorite(song)
                                            showSongMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isSongDownloaded) "Delete Offline" else "Download Offline", color = Color.White) },
                                        leadingIcon = {
                                            Icon(
                                                if (isSongDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                                contentDescription = null,
                                                tint = if (isSongDownloaded) Color(0xFF22C55E) else AppleMusicRed
                                            )
                                        },
                                        onClick = {
                                            if (isSongDownloaded) onDeleteDownload(song.id)
                                            else onDownloadSong(song)
                                            showSongMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(150.dp))
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = onCreatePlaylist
        )
    }
}
