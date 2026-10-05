package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.AiQueryAnalysis
import com.example.data.local.Playlist
import com.example.data.model.ActiveMusicFilters
import com.example.data.model.ContentType
import com.example.model.Song
import com.example.ui.components.AdvancedFilterSheet
import com.example.ui.theme.AppleDarkSurface
import com.example.ui.theme.AppleMusicRed
import com.example.util.PerformanceMode

data class SearchCategory(
    val title: String,
    val colors: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    searchResults: List<Song>,
    matchingPlaylists: List<Playlist>,
    isSearching: Boolean,
    isAiRanking: Boolean,
    searchError: String?,
    aiAnalysis: AiQueryAnalysis?,
    isAiActive: Boolean,
    isOffline: Boolean,
    activeFilters: ActiveMusicFilters,
    recentSearches: List<String>,
    searchSuggestions: List<String>,
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    downloadedIds: Set<Int>,
    downloadProgress: Map<Int, Float>,
    favorites: Set<Int>,
    performanceMode: PerformanceMode,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onApplyFilters: (ActiveMusicFilters) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onLoadMore: () -> Unit,
    onDownloadSong: (Song) -> Unit,
    onDeleteDownload: (Int) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    // Detect when user scrolls near the end to trigger infinite scroll / pagination
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && canLoadMore && !isLoadingMore && !isSearching) {
                onLoadMore()
            }
        }
    }

    val categories = remember {
        listOf(
            SearchCategory("Bollywood & Hindi", listOf(Color(0xFFE11D48), Color(0xFFBE123C))),
            SearchCategory("Punjabi Hits", listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
            SearchCategory("Spatial Audio", listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))),
            SearchCategory("Pop", listOf(Color(0xFFEC4899), Color(0xFFDB2777))),
            SearchCategory("Hip-Hop", listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
            SearchCategory("Dance & EDM", listOf(Color(0xFF06B6D4), Color(0xFF0891B2))),
            SearchCategory("Rock & Metal", listOf(Color(0xFFEF4444), Color(0xFFDC2626))),
            SearchCategory("R&B / Soul", listOf(Color(0xFF10B981), Color(0xFF059669))),
            SearchCategory("Chill & Relax", listOf(Color(0xFF6366F1), Color(0xFF4F46E5))),
            SearchCategory("Workout Energy", listOf(Color(0xFFF97316), Color(0xFFEA580C)))
        )
    }

    val smartPrompts = listOf(
        "sad songs",
        "arijit singh romantic",
        "latest hindi songs",
        "90s bollywood",
        "gym music",
        "barsaat banjaare",
        "best punjabi playlist",
        "songs like this"
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Search Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    // AI indicator / settings trigger
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isAiActive) Color(0xFF6366F1).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = if (isAiActive) Color(0xFF818CF8) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAiActive) "Gemini Active" else "AI Settings",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAiActive) Color(0xFFC7D2FE) else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = "Artists, Songs, Lyrics, Hindi, Hinglish...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 4.dp),
                                    strokeWidth = 2.dp,
                                    color = AppleMusicRed
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E),
                        focusedBorderColor = AppleMusicRed,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        onSubmitSearch(searchQuery)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_field")
                )

                // Instant Auto-Complete Suggestions as user types
                if (searchSuggestions.isNotEmpty() && searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    ) {
                        searchSuggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        focusManager.clearFocus()
                                        onQueryChange(suggestion)
                                        onSubmitSearch(suggestion)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = AppleMusicRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Pills Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeCount = activeFilters.activeCount
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeCount > 0) AppleMusicRed else Color.White.copy(alpha = 0.1f))
                            .clickable { showFilterSheet = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filters",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeCount > 0) "Filters ($activeCount)" else "Filters",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Content Type pills: All, Songs, Artists, Albums
                    val types = listOf(ContentType.ALL, ContentType.SONGS, ContentType.ARTISTS, ContentType.ALBUMS)
                    types.forEach { type ->
                        val isSel = activeFilters.contentType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) AppleMusicRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isSel) AppleMusicRed else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .clickable {
                                    onApplyFilters(activeFilters.copy(contentType = type))
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = type.label,
                                fontSize = 12.sp,
                                color = if (isSel) AppleMusicRed else Color.White.copy(alpha = 0.8f),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Offline Banner (if applicable)
        if (isOffline) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Offline Mode Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCD34D)
                        )
                        Text(
                            text = "Searching downloaded tracks & local library songs.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // AI Ranking in progress pulse
        if (isAiRanking) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFF818CF8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini AI is analyzing intent and ranking results...",
                        fontSize = 11.sp,
                        color = Color(0xFFC7D2FE)
                    )
                }
            }
        }

        // AI Query Understanding & Intelligence Card
        if (aiAnalysis != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4F46E5).copy(alpha = 0.22f),
                                    Color(0xFF7C3AED).copy(alpha = 0.14f)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gemini AI Intelligence",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC7D2FE)
                                )
                            }
                            if (aiAnalysis.mood != null || aiAnalysis.genre != null) {
                                Text(
                                    text = listOfNotNull(aiAnalysis.genre, aiAnalysis.mood, aiAnalysis.era).joinToString(" • "),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiAnalysis.explanation,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )

                        // Smart recommendations / keyword chips
                        if (aiAnalysis.searchKeywords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                aiAnalysis.searchKeywords.forEach { kw ->
                                    if (!kw.equals(searchQuery, ignoreCase = true)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.1f))
                                                .clickable {
                                                    onQueryChange(kw)
                                                    onSubmitSearch(kw)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "+ $kw",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Matching Playlists section (if present)
        if (matchingPlaylists.isNotEmpty() && searchQuery.isNotBlank()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Matching Playlists (${matchingPlaylists.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            items(matchingPlaylists, key = { "pl_${it.id}" }) { pl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = AppleMusicRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = pl.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "${pl.songIds.size} songs • ${pl.description.ifEmpty { "Playlist" }}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Search Results OR Empty State & Browse Categories
        if (searchQuery.isNotBlank()) {
            if (searchResults.isEmpty() && !isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No results found for \"$searchQuery\"",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (!activeFilters.isDefault) "Try resetting active filters or checking spelling." else "Try searching with artist, album, or Hindi keywords.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppleMusicRed.copy(alpha = 0.2f))
                                    .clickable { onSubmitSearch(searchQuery) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = AppleMusicRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Retry Search",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleMusicRed
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (aiAnalysis != null) "Top Matches (AI Ranked)" else "Top Songs (${searchResults.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                items(searchResults, key = { it.id }) { song ->
                    val isDownloaded = downloadedIds.contains(song.id)
                    val isFav = favorites.contains(song.id)
                    val progress = downloadProgress[song.id]

                    var showMenu by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song) }
                            .padding(horizontal = 20.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppleDarkSurface)
                        ) {
                            AsyncImage(
                                model = song.artwork,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = song.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (song.isExplicit) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.White.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "E",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${song.artist} • ${song.genre} • ${song.releaseYear}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Download state / Action
                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = { progress },
                                strokeWidth = 2.dp,
                                color = AppleMusicRed,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp)
                            )
                        } else if (isDownloaded) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Overflow menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color(0xFF222226))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Play Next", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        onPlayNext(song)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Queue", color = Color.White) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        onAddToQueue(song)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        onAddToPlaylist(song)
                                        showMenu = false
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
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isDownloaded) "Delete Offline" else "Download Offline", color = Color.White) },
                                    leadingIcon = {
                                        Icon(
                                            if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                            contentDescription = null,
                                            tint = if (isDownloaded) Color(0xFF22C55E) else AppleMusicRed
                                        )
                                    },
                                    onClick = {
                                        if (isDownloaded) onDeleteDownload(song.id)
                                        else onDownloadSong(song)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Loading more indicator at end of list
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = AppleMusicRed
                            )
                        }
                    }
                }
            }
        } else {
            // When query is empty: Recent Searches
            if (recentSearches.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recent Searches",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Clear",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppleMusicRed,
                                modifier = Modifier
                                    .clickable(onClick = onClearRecentSearches)
                                    .padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentSearches.forEach { query ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E1E24))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            focusManager.clearFocus()
                                            onQueryChange(query)
                                            onSubmitSearch(query)
                                        }
                                        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = query,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = { onRemoveRecentSearch(query) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Smart AI Discovery Prompts
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isAiActive) Color(0xFF818CF8) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Smart AI Searches (Try asking in plain words)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        smartPrompts.forEach { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E1E24))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        focusManager.clearFocus()
                                        onQueryChange(prompt)
                                        onSubmitSearch(prompt)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Browse Categories Header
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Browse Categories",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Categories 2x2 grid cards
            val chunkedCategories = categories.chunked(2)
            items(chunkedCategories.size) { chunkIndex ->
                val pair = chunkedCategories[chunkIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    pair.forEach { cat ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(cat.colors))
                                .clickable {
                                    onQueryChange(cat.title)
                                    onSubmitSearch(cat.title)
                                }
                                .padding(14.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = cat.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(160.dp))
        }
    }

    if (showFilterSheet) {
        AdvancedFilterSheet(
            initialFilters = activeFilters,
            sheetState = filterSheetState,
            onApply = onApplyFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}
