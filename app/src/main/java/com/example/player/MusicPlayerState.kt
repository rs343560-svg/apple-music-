package com.example.player

import com.example.data.api.AiQueryAnalysis
import com.example.data.local.Playlist
import com.example.data.model.ActiveMusicFilters
import com.example.model.Song
import com.example.util.PerformanceMode

data class MusicPlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 30000L,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val volume: Float = 0.8f,
    val favorites: Set<Int> = emptySet(),
    val queue: List<Song> = emptyList(),
    val showLyrics: Boolean = false,
    val showQueue: Boolean = false,
    val showAudioRoute: Boolean = false,
    val selectedAudioRoute: String = "iPhone Speaker",
    val showVideoCanvas: Boolean = true,
    val downloadedSongIds: Set<Int> = emptySet(),
    val downloadProgress: Map<Int, Float> = emptyMap(),
    val performanceMode: PerformanceMode = PerformanceMode.HIGH,
    // Spotify-Style Smart Autoplay & Queue State
    val isAutoplayEnabled: Boolean = true,
    val smartRecommendations: List<Song> = emptyList(),
    val showYouTubePlayer: Boolean = false,
    val activeYouTubeSong: Song? = null,
    // Enhanced Smart AI Search, Discovery & Fallback State
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val matchingPlaylists: List<Playlist> = emptyList(),
    val isSearching: Boolean = false,
    val isAiRanking: Boolean = false,
    val searchError: String? = null,
    val isOfflineMode: Boolean = false,
    val isSmartFallback: Boolean = false,
    val searchFallbackNotice: String? = null,
    val aiAnalysis: AiQueryAnalysis? = null,
    val activeFilters: ActiveMusicFilters = ActiveMusicFilters(),
    val recentSearches: List<String> = emptyList(),
    val searchSuggestions: List<String> = emptyList(),
    val customPlaylists: List<Playlist> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val customGeminiApiKey: String = "",
    val geminiStatus: String = "NOT_CONFIGURED",
    val isAiActive: Boolean = false,
    val isNetworkOnline: Boolean = true,
    val showSettingsDialog: Boolean = false,
    val showFilterSheet: Boolean = false,
    val showAddToPlaylistDialog: Song? = null,
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val searchPageOffset: Int = 0
)
