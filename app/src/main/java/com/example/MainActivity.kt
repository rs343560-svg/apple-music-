package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MusicCatalog
import com.example.player.MusicPlayerViewModel
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.AppleMusicBottomBar
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.NavigationTab
import com.example.ui.components.SettingsDialog
import com.example.ui.player.AppleMusicSheetLayout
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NewScreen
import com.example.ui.screens.RadioScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                AppleMusicApp()
            }
        }
    }
}

@Composable
fun AppleMusicApp(
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val playerState by viewModel.state.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var showCreatePlaylistFromAdd by remember { mutableStateOf(false) }

    // Combine static catalog with downloaded and recently played songs
    val allSongs = remember(playerState.downloadedSongIds, playerState.recentlyPlayed) {
        val set = mutableSetOf<Int>()
        val list = mutableListOf<com.example.model.Song>()
        (MusicCatalog.songs + playerState.recentlyPlayed).forEach {
            if (set.add(it.id)) list.add(it)
        }
        list
    }

    // Android back handler to return to Home tab
    BackHandler(enabled = currentTab != NavigationTab.HOME) {
        currentTab = NavigationTab.HOME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AppleMusicSheetLayout(
            playerState = playerState,
            viewModel = viewModel,
            bottomBar = {
                AppleMusicBottomBar(
                    currentTab = currentTab,
                    performanceMode = playerState.performanceMode,
                    onTabSelected = { currentTab = it }
                )
            },
            backgroundContent = {
                Crossfade(targetState = currentTab, label = "tabCrossfade") { tab ->
                    when (tab) {
                        NavigationTab.HOME -> {
                            HomeScreen(
                                songs = allSongs,
                                currentSong = playerState.currentSong,
                                isPlaying = playerState.isPlaying,
                                downloadedIds = playerState.downloadedSongIds,
                                favorites = playerState.favorites,
                                recentlyPlayed = playerState.recentlyPlayed,
                                performanceMode = playerState.performanceMode,
                                onSongClick = { viewModel.playSong(it) },
                                onPlayFirst = {
                                    allSongs.firstOrNull()?.let { viewModel.playSong(it) }
                                },
                                onShuffleAll = { viewModel.shuffleAll() },
                                onSetPerformanceMode = { viewModel.setPerformanceMode(it) },
                                onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                                onPlayNext = { viewModel.playNextInQueue(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylist = { viewModel.setShowAddToPlaylistDialog(it) },
                                onDownloadSong = { viewModel.downloadSong(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                        NavigationTab.NEW -> {
                            NewScreen(
                                songs = allSongs,
                                onSongClick = { viewModel.playSong(it) }
                            )
                        }
                        NavigationTab.RADIO -> {
                            RadioScreen(
                                songs = allSongs,
                                onTuneIn = { viewModel.playSong(it) }
                            )
                        }
                        NavigationTab.LIBRARY -> {
                            LibraryScreen(
                                songs = allSongs,
                                downloadedIds = playerState.downloadedSongIds,
                                playlists = playerState.customPlaylists,
                                favorites = playerState.favorites,
                                recentlyPlayed = playerState.recentlyPlayed,
                                performanceMode = playerState.performanceMode,
                                onSongClick = { viewModel.playSong(it) },
                                onDownloadSong = { viewModel.downloadSong(it) },
                                onDeleteDownload = { viewModel.deleteDownload(it) },
                                onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onRemoveSongFromPlaylist = { plId, sId -> viewModel.removeSongFromPlaylist(plId, sId) },
                                onAddToPlaylist = { viewModel.setShowAddToPlaylistDialog(it) },
                                onPlayNext = { viewModel.playNextInQueue(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                        NavigationTab.SEARCH -> {
                            SearchScreen(
                                searchQuery = playerState.searchQuery,
                                searchResults = if (playerState.searchQuery.isBlank()) allSongs else playerState.searchResults,
                                matchingPlaylists = playerState.matchingPlaylists,
                                isSearching = playerState.isSearching,
                                isAiRanking = playerState.isAiRanking,
                                searchError = playerState.searchError,
                                aiAnalysis = playerState.aiAnalysis,
                                isAiActive = playerState.isAiActive,
                                isOffline = playerState.isOfflineMode,
                                activeFilters = playerState.activeFilters,
                                recentSearches = playerState.recentSearches,
                                searchSuggestions = playerState.searchSuggestions,
                                canLoadMore = playerState.canLoadMore,
                                isLoadingMore = playerState.isLoadingMore,
                                downloadedIds = playerState.downloadedSongIds,
                                downloadProgress = playerState.downloadProgress,
                                favorites = playerState.favorites,
                                performanceMode = playerState.performanceMode,
                                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                                onSubmitSearch = { viewModel.submitSearch(it) },
                                onSongClick = { viewModel.playSong(it) },
                                onApplyFilters = { viewModel.applyFilters(it) },
                                onRemoveRecentSearch = { viewModel.removeRecentSearch(it) },
                                onClearRecentSearches = { viewModel.clearRecentSearches() },
                                onLoadMore = { viewModel.loadMoreSearchResults() },
                                onDownloadSong = { viewModel.downloadSong(it) },
                                onDeleteDownload = { viewModel.deleteDownload(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onPlayNext = { viewModel.playNextInQueue(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylist = { viewModel.setShowAddToPlaylistDialog(it) },
                                onOpenSettings = { viewModel.setShowSettingsDialog(true) }
                            )
                        }
                    }
                }
            }
        )

        // Settings Dialog
        if (playerState.showSettingsDialog) {
            SettingsDialog(
                currentApiKey = playerState.customGeminiApiKey,
                geminiStatus = playerState.geminiStatus,
                isAiActive = playerState.isAiActive,
                performanceMode = playerState.performanceMode,
                downloadedCount = playerState.downloadedSongIds.size,
                onSaveApiKey = { key, callback -> viewModel.saveGeminiApiKey(key, callback) },
                onClearApiKey = { viewModel.clearGeminiApiKey() },
                onSetPerformanceMode = { viewModel.setPerformanceMode(it) },
                onDismiss = { viewModel.setShowSettingsDialog(false) }
            )
        }

        // Add to Playlist Sheet
        playerState.showAddToPlaylistDialog?.let { songToAdd ->
            AddToPlaylistSheet(
                song = songToAdd,
                playlists = playerState.customPlaylists,
                onSelectPlaylist = { plId ->
                    viewModel.addSongToPlaylist(plId, songToAdd.id)
                },
                onCreateNewPlaylist = {
                    showCreatePlaylistFromAdd = true
                },
                onDismiss = { viewModel.setShowAddToPlaylistDialog(null) }
            )
        }

        // Create Playlist Dialog when initiated from Add to Playlist Sheet
        if (showCreatePlaylistFromAdd) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistFromAdd = false },
                onCreate = { name, desc ->
                    viewModel.createPlaylist(name, desc)
                    playerState.showAddToPlaylistDialog?.let { song ->
                        val newest = viewModel.state.value.customPlaylists.firstOrNull()
                        if (newest != null) {
                            viewModel.addSongToPlaylist(newest.id, song.id)
                        }
                    }
                    showCreatePlaylistFromAdd = false
                }
            )
        }
    }
}
