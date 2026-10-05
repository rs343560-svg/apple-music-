package com.example.player

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.MusicCatalog
import com.example.data.api.GeminiSearchService
import com.example.data.local.Playlist
import com.example.data.local.UserPreferencesManager
import com.example.data.model.ActiveMusicFilters
import com.example.data.model.SortOption
import com.example.data.repository.MusicRepository
import com.example.data.repository.SearchResultState
import com.example.model.Song
import com.example.util.PerformanceMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = MediaPlaybackService.engine
    private val repository = MusicRepository(application)
    private val prefsManager = UserPreferencesManager(application)

    val state: StateFlow<MusicPlayerState> = engine.state
    val positionMs: StateFlow<Long> = engine.positionMs

    private var searchJob: Job? = null
    private var aiRankingJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        engine.checkDownloadedSongs(application)

        // Load initial local user state
        val savedKey = prefsManager.getCustomGeminiApiKey().ifEmpty {
            try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        }
        val isAiAvailable = savedKey.isNotBlank() && !savedKey.equals("AIzaSy_demo", ignoreCase = true)
        val initialStatus = if (isAiAvailable) "CONNECTED" else "NOT_CONFIGURED"

        val perfMode = prefsManager.getPerformanceMode()
        val initialFavs = prefsManager.getFavoriteIds()
        val initialPlaylists = prefsManager.getPlaylists()
        val initialRecents = prefsManager.getRecentlyPlayed()
        val initialRecentSearches = prefsManager.getRecentSearches()

        engine.updateState {
            it.copy(
                customGeminiApiKey = savedKey,
                geminiStatus = initialStatus,
                isAiActive = isAiAvailable,
                performanceMode = perfMode,
                favorites = initialFavs,
                customPlaylists = initialPlaylists,
                recentlyPlayed = initialRecents,
                recentSearches = initialRecentSearches,
                isNetworkOnline = repository.isOnline()
            )
        }

        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    engine.updateState { it.copy(isNetworkOnline = true, isOfflineMode = false) }
                }

                override fun onLost(network: Network) {
                    engine.updateState { it.copy(isNetworkOnline = false, isOfflineMode = true) }
                }
            }
            networkCallback?.let { cm?.registerNetworkCallback(request, it) }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
    }

    // --- Smart AI & Two-Stage Search ---

    fun onSearchQueryChanged(newQuery: String) {
        engine.updateState { it.copy(searchQuery = newQuery) }
        computeSuggestions(newQuery)

        searchJob?.cancel()
        aiRankingJob?.cancel()

        if (newQuery.isBlank()) {
            engine.updateState {
                it.copy(
                    searchResults = emptyList(),
                    matchingPlaylists = emptyList(),
                    isSearching = false,
                    isAiRanking = false,
                    aiAnalysis = null,
                    searchError = null,
                    searchPageOffset = 0,
                    canLoadMore = false
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            // 300ms debounce for responsive typing without UI stutter
            delay(300)
            executeTwoStageSearch(newQuery, isNewSearch = true)
        }
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        searchJob?.cancel()
        aiRankingJob?.cancel()

        engine.updateState { it.copy(searchQuery = trimmed, searchSuggestions = emptyList()) }
        prefsManager.addRecentSearch(trimmed)
        engine.updateState { it.copy(recentSearches = prefsManager.getRecentSearches()) }

        viewModelScope.launch {
            executeTwoStageSearch(trimmed, isNewSearch = true)
        }
    }

    private suspend fun executeTwoStageSearch(query: String, isNewSearch: Boolean) {
        if (isNewSearch) {
            engine.updateState {
                it.copy(
                    isSearching = true,
                    isAiRanking = false,
                    searchError = null,
                    searchPageOffset = 0,
                    canLoadMore = true
                )
            }
        }

        try {
            val filters = state.value.activeFilters

            // STAGE 1: Immediate Music Provider Retrieval
            val stage1 = repository.searchProviderFast(query, filters, offset = 0, limit = 30)

            engine.updateState {
                it.copy(
                    searchResults = stage1.songs,
                    matchingPlaylists = stage1.matchingPlaylists,
                    isSearching = false,
                    isAiRanking = it.isAiActive,
                    isOfflineMode = stage1.isOfflineOnly,
                    searchError = if (stage1.hasNetworkError) "Unable to reach music servers. Showing offline & cached results." else null,
                    canLoadMore = stage1.songs.size >= 25
                )
            }

            // STAGE 2: Gemini AI Understanding & Ranking
            val apiKey = state.value.customGeminiApiKey
            if (state.value.isAiActive && apiKey.isNotBlank() && stage1.songs.isNotEmpty() && !stage1.isOfflineOnly) {
                aiRankingJob = viewModelScope.launch {
                    try {
                        val aiResult = repository.enhanceWithAi(query, stage1, filters, apiKey)
                        engine.updateState {
                            it.copy(
                                searchResults = aiResult.songs,
                                aiAnalysis = aiResult.aiAnalysis,
                                isAiRanking = false
                            )
                        }
                    } catch (_: Exception) {
                        engine.updateState { it.copy(isAiRanking = false) }
                    }
                }
            } else {
                engine.updateState { it.copy(isAiRanking = false) }
            }
        } catch (e: Exception) {
            engine.updateState {
                it.copy(
                    isSearching = false,
                    isAiRanking = false,
                    searchError = "Search error: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun loadMoreSearchResults() {
        if (state.value.isLoadingMore || !state.value.canLoadMore) return
        val currentQuery = state.value.searchQuery.trim()
        if (currentQuery.isBlank()) return

        viewModelScope.launch {
            engine.updateState { it.copy(isLoadingMore = true) }
            try {
                val currentOffset = state.value.searchResults.size
                val moreResults = repository.searchProviderFast(
                    currentQuery,
                    state.value.activeFilters,
                    offset = currentOffset,
                    limit = 20
                )
                val existingIds = state.value.searchResults.map { it.id }.toSet()
                val distinctNew = moreResults.songs.filter { !existingIds.contains(it.id) }

                engine.updateState {
                    it.copy(
                        searchResults = it.searchResults + distinctNew,
                        isLoadingMore = false,
                        canLoadMore = distinctNew.isNotEmpty()
                    )
                }
            } catch (_: Exception) {
                engine.updateState { it.copy(isLoadingMore = false) }
            }
        }
    }

    private fun computeSuggestions(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            engine.updateState { it.copy(searchSuggestions = emptyList()) }
            return
        }

        val allCatalog = MusicCatalog.songs
        val suggestions = mutableListOf<String>()

        // Match titles
        allCatalog.forEach { s ->
            if (s.title.lowercase().startsWith(q) || s.title.lowercase().contains(q)) {
                if (!suggestions.contains(s.title)) suggestions.add(s.title)
            }
        }

        // Match artists
        allCatalog.forEach { s ->
            if (s.artist.lowercase().startsWith(q) || s.artist.lowercase().contains(q)) {
                if (!suggestions.contains(s.artist)) suggestions.add(s.artist)
            }
        }

        // Match common search phrases
        val commonPhrases = listOf(
            "sad songs", "arijit singh romantic", "latest hindi songs",
            "90s bollywood", "gym music", "barsaat banjaare", "best punjabi playlist",
            "espresso sabrina carpenter", "birds of a feather billie eilish",
            "a bar song tipsy shaboozey", "lover diljit dosanjh", "kesariya arijit singh"
        )
        commonPhrases.forEach { phrase ->
            if (phrase.contains(q) && !suggestions.contains(phrase)) {
                suggestions.add(phrase)
            }
        }

        engine.updateState { it.copy(searchSuggestions = suggestions.take(5)) }
    }

    fun applyFilters(newFilters: ActiveMusicFilters) {
        engine.updateState { it.copy(activeFilters = newFilters) }
        val currentQuery = state.value.searchQuery
        viewModelScope.launch {
            executeTwoStageSearch(currentQuery, isNewSearch = true)
        }
    }

    fun clearFilters() {
        applyFilters(ActiveMusicFilters())
    }

    fun setSortOption(sortOption: SortOption) {
        val updated = state.value.activeFilters.copy(sortOption = sortOption)
        applyFilters(updated)
    }

    // --- Recent Searches ---

    fun removeRecentSearch(query: String) {
        prefsManager.removeRecentSearch(query)
        engine.updateState { it.copy(recentSearches = prefsManager.getRecentSearches()) }
    }

    fun clearRecentSearches() {
        prefsManager.clearRecentSearches()
        engine.updateState { it.copy(recentSearches = emptyList()) }
    }

    // --- Playback Controls ---

    fun playSong(song: Song) {
        MediaPlaybackService.startService(getApplication())
        prefsManager.addRecentlyPlayed(song)
        engine.updateState { it.copy(recentlyPlayed = prefsManager.getRecentlyPlayed()) }
        engine.playSong(song)
    }

    fun togglePlayPause() {
        MediaPlaybackService.startService(getApplication())
        engine.togglePlayPause()
    }

    fun playNext() {
        engine.playNext()
    }

    fun playPrevious() {
        engine.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        engine.seekTo(positionMs)
    }

    fun setVolume(volume: Float) {
        engine.setVolume(volume)
    }

    fun toggleShuffle() {
        engine.toggleShuffle()
    }

    fun toggleRepeat() {
        engine.toggleRepeat()
    }

    fun toggleFavorite(song: Song) {
        prefsManager.toggleFavorite(song)
        val favIds = prefsManager.getFavoriteIds()
        engine.updateState { it.copy(favorites = favIds) }
    }

    fun toggleFavorite(songId: Int) {
        val song = state.value.currentSong?.takeIf { it.id == songId }
            ?: state.value.queue.firstOrNull { it.id == songId }
            ?: MusicCatalog.songs.firstOrNull { it.id == songId }
        if (song != null) {
            toggleFavorite(song)
        } else {
            engine.toggleFavorite(songId)
        }
    }

    fun toggleLyrics() {
        engine.toggleLyrics()
    }

    fun toggleQueue() {
        engine.toggleQueue()
    }

    fun toggleVideoCanvas() {
        engine.toggleVideoCanvas()
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        prefsManager.setPerformanceMode(mode)
        engine.setPerformanceMode(mode)
    }

    fun setShowAudioRoute(show: Boolean) {
        engine.setShowAudioRoute(show)
    }

    fun setAudioRoute(route: String) {
        engine.setAudioRoute(route)
    }

    fun shuffleAll() {
        MediaPlaybackService.startService(getApplication())
        engine.shuffleAll()
    }

    // --- Queue Management ---

    fun addToQueue(song: Song) {
        engine.addToQueue(song)
    }

    fun playNextInQueue(song: Song) {
        engine.playNextInQueue(song)
    }

    fun removeFromQueue(songId: Int) {
        engine.removeFromQueue(songId)
    }

    fun clearQueue() {
        engine.clearQueue()
    }

    // --- Downloads ---

    fun downloadSong(song: Song) {
        engine.downloadSong(getApplication(), song, viewModelScope)
    }

    fun deleteDownload(songId: Int) {
        engine.deleteDownload(getApplication(), songId, viewModelScope)
    }

    // --- Gemini AI Configuration ---

    fun saveGeminiApiKey(key: String, onValidated: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val trimmed = key.trim()
            if (trimmed.isEmpty()) {
                prefsManager.clearCustomGeminiApiKey()
                prefsManager.setGeminiStatus("NOT_CONFIGURED")
                engine.updateState {
                    it.copy(
                        customGeminiApiKey = "",
                        geminiStatus = "NOT_CONFIGURED",
                        isAiActive = false
                    )
                }
                onValidated(true, "API Key removed. Reverted to standard search.")
                return@launch
            }

            engine.updateState { it.copy(geminiStatus = "VALIDATING") }
            val isValid = GeminiSearchService.validateApiKey(trimmed)

            if (isValid) {
                prefsManager.setCustomGeminiApiKey(trimmed)
                prefsManager.setGeminiStatus("CONNECTED")
                engine.updateState {
                    it.copy(
                        customGeminiApiKey = trimmed,
                        geminiStatus = "CONNECTED",
                        isAiActive = true
                    )
                }
                onValidated(true, "Connected to Gemini AI (gemini-3.5-flash) successfully!")
            } else {
                prefsManager.setGeminiStatus("ERROR")
                engine.updateState {
                    it.copy(geminiStatus = "ERROR")
                }
                onValidated(false, "Invalid API key or network error. Please verify key.")
            }
        }
    }

    fun clearGeminiApiKey() {
        prefsManager.clearCustomGeminiApiKey()
        prefsManager.setGeminiStatus("NOT_CONFIGURED")
        engine.updateState {
            it.copy(
                customGeminiApiKey = "",
                geminiStatus = "NOT_CONFIGURED",
                isAiActive = false,
                aiAnalysis = null
            )
        }
    }

    // --- Custom Playlists ---

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        prefsManager.createPlaylist(name, description)
        engine.updateState { it.copy(customPlaylists = prefsManager.getPlaylists()) }
    }

    fun addSongToPlaylist(playlistId: String, songId: Int) {
        prefsManager.addSongToPlaylist(playlistId, songId)
        engine.updateState {
            it.copy(
                customPlaylists = prefsManager.getPlaylists(),
                showAddToPlaylistDialog = null
            )
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Int) {
        prefsManager.removeSongFromPlaylist(playlistId, songId)
        engine.updateState { it.copy(customPlaylists = prefsManager.getPlaylists()) }
    }

    fun deletePlaylist(playlistId: String) {
        prefsManager.deletePlaylist(playlistId)
        engine.updateState { it.copy(customPlaylists = prefsManager.getPlaylists()) }
    }

    // --- Dialog Toggles ---

    fun setShowSettingsDialog(show: Boolean) {
        engine.updateState { it.copy(showSettingsDialog = show) }
    }

    fun setShowFilterSheet(show: Boolean) {
        engine.updateState { it.copy(showFilterSheet = show) }
    }

    fun setShowAddToPlaylistDialog(song: Song?) {
        engine.updateState { it.copy(showAddToPlaylistDialog = song) }
    }
}
