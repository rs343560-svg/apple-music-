package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.MusicCatalog
import com.example.data.api.AiQueryAnalysis
import com.example.data.api.GeminiSearchService
import com.example.data.api.ItunesSearchApi
import com.example.data.api.JamendoSearchApi
import com.example.data.local.Playlist
import com.example.data.local.UserPreferencesManager
import com.example.data.model.ActiveMusicFilters
import com.example.data.model.AvailabilityFilter
import com.example.data.model.ContentType
import com.example.data.model.DurationFilter
import com.example.data.model.ExplicitFilter
import com.example.data.model.GenreFilter
import com.example.data.model.LanguageFilter
import com.example.data.model.SortOption
import com.example.data.model.YearFilter
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SearchResultState(
    val query: String,
    val songs: List<Song>,
    val matchingPlaylists: List<Playlist> = emptyList(),
    val aiAnalysis: AiQueryAnalysis? = null,
    val isAiPowered: Boolean = false,
    val isOfflineOnly: Boolean = false,
    val hasNetworkError: Boolean = false,
    val totalMatches: Int = 0,
    val isSmartFallback: Boolean = false,
    val fallbackNotice: String? = null
)

class MusicRepository(private val context: Context) {

    private val prefsManager = UserPreferencesManager(context)

    // Hindi, Punjabi, and common music alias dictionary for instant zero-latency match
    private val localAliases = mapOf(
        "arjit" to "Arijit Singh",
        "arijit" to "Arijit Singh",
        "kesaria" to "Kesariya",
        "kesariya" to "Kesariya",
        "diljit" to "Diljit Dosanjh",
        "shabozy" to "Shaboozey",
        "tipsy" to "A Bar Song (Tipsy)",
        "billie" to "Billie Eilish",
        "sabrina" to "Sabrina Carpenter",
        "espresso" to "Espresso",
        "weekend" to "The Weeknd",
        "starboy" to "Starboy",
        "dhillon" to "AP Dhillon",
        "pritam" to "Pritam",
        "barsaat" to "Barsaat Ki Dhun",
        "banjaare" to "Banjaare",
        "gym" to "Workout Energy",
        "sad" to "Acoustic Ballad"
    )

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Multi-Source Search with Smart Fallback:
     * 1. Primary: Fast retrieval from Curated Vault + Apple Music (iTunes).
     * 2. Smart Fallback: If primary provider returns 0 or limited playable results,
     *    automatically queries authorized secondary sources (Jamendo Open Audio,
     *    curated catalog fuzzy match) and tags results clearly with their provider source.
     */
    suspend fun searchProviderFast(
        query: String,
        filters: ActiveMusicFilters,
        offset: Int = 0,
        limit: Int = 30
    ): SearchResultState = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val online = isOnline()

        val allLocalSongs = (MusicCatalog.songs + prefsManager.getDownloadedSongsMetadata()).distinctBy { it.id }
        val allPlaylists = prefsManager.getPlaylists()

        if (trimmed.isEmpty()) {
            val filteredSongs = applyFiltersAndSorting(allLocalSongs, filters)
            return@withContext SearchResultState(
                query = "",
                songs = filteredSongs,
                matchingPlaylists = allPlaylists,
                totalMatches = filteredSongs.size,
                isOfflineOnly = !online
            )
        }

        // Matching playlists
        val matchingPlaylists = allPlaylists.filter {
            it.name.contains(trimmed, ignoreCase = true) || it.description.contains(trimmed, ignoreCase = true)
        }

        // Search local curated songs
        val localMatches = searchLocalCatalog(trimmed, allLocalSongs)

        if (!online) {
            val filtered = applyFiltersAndSorting(localMatches, filters)
            return@withContext SearchResultState(
                query = trimmed,
                songs = filtered,
                matchingPlaylists = matchingPlaylists,
                totalMatches = filtered.size,
                isOfflineOnly = true
            )
        }

        // Search remote primary catalog (Apple Music / iTunes)
        val primarySongs = mutableListOf<Song>()
        val seenKeys = mutableSetOf<String>()

        localMatches.forEach { seenKeys.add(normalizeKey(it.title, it.artist)) }

        val queriesToFetch = mutableListOf(trimmed)
        // Add alias expansion if present
        localAliases.forEach { (k, v) ->
            if (trimmed.contains(k, ignoreCase = true)) {
                queriesToFetch.add(trimmed.replace(Regex(k, RegexOption.IGNORE_CASE), v))
                queriesToFetch.add(v)
            }
        }

        var fetchFailed = false
        for (q in queriesToFetch.distinct().take(2)) {
            try {
                val results = ItunesSearchApi.searchSongs(q, limit = limit, offset = offset)
                for (s in results) {
                    val key = normalizeKey(s.title, s.artist)
                    if (seenKeys.add(key)) {
                        primarySongs.add(s)
                    }
                }
            } catch (_: Exception) {
                fetchFailed = true
            }
        }

        val primaryCombined = (localMatches + primarySongs).distinctBy { normalizeKey(it.title, it.artist) }
        var isSmartFallbackActive = false
        var fallbackNotice: String? = null

        // SMART FALLBACK: If primary provider returned no results or very sparse (< 3)
        val finalCombined = if (primaryCombined.size < 3) {
            val fallbackSongs = mutableListOf<Song>()

            // 1. Authorized Open Audio Source: Jamendo
            try {
                val jamendoResults = JamendoSearchApi.searchTracks(trimmed, limit = 15)
                for (s in jamendoResults) {
                    val key = normalizeKey(s.title, s.artist)
                    if (seenKeys.add(key)) {
                        fallbackSongs.add(s)
                    }
                }
            } catch (_: Exception) {}

            // 2. Fuzzy Token Match in Curated Vault
            val fuzzyMatches = searchFuzzyCatalog(trimmed, allLocalSongs)
            for (s in fuzzyMatches) {
                val key = normalizeKey(s.title, s.artist)
                if (seenKeys.add(key)) {
                    fallbackSongs.add(s)
                }
            }

            if (primaryCombined.isEmpty() && fallbackSongs.isNotEmpty()) {
                isSmartFallbackActive = true
                fallbackNotice = "No exact match on Apple Music. Showing closest authorized matches from Jamendo & Curated Vault."
            }
            (primaryCombined + fallbackSongs).distinctBy { normalizeKey(it.title, it.artist) }
        } else {
            primaryCombined
        }

        val filtered = applyFiltersAndSorting(finalCombined, filters)

        SearchResultState(
            query = trimmed,
            songs = filtered,
            matchingPlaylists = matchingPlaylists,
            totalMatches = filtered.size,
            isOfflineOnly = false,
            hasNetworkError = fetchFailed && finalCombined.isEmpty(),
            isSmartFallback = isSmartFallbackActive,
            fallbackNotice = fallbackNotice
        )
    }

    /**
     * Stage 2: Gemini AI Analysis & Intelligent Ranking.
     * Takes candidate results, understands intent (Hindi/Hinglish, moods, genres, eras),
     * and intelligently ranks real results without inventing fake songs or URLs.
     */
    suspend fun enhanceWithAi(
        query: String,
        stage1Results: SearchResultState,
        filters: ActiveMusicFilters,
        geminiApiKey: String
    ): SearchResultState = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || geminiApiKey.isBlank()) return@withContext stage1Results

        try {
            val analysis = GeminiSearchService.analyzeAndRank(trimmed, stage1Results.songs, geminiApiKey)
                ?: return@withContext stage1Results

            val existingSongs = stage1Results.songs.toMutableList()
            val seenKeys = existingSongs.map { normalizeKey(it.title, it.artist) }.toMutableSet()

            // If AI identified targeted search keywords, query authorized providers
            if (analysis.searchKeywords.isNotEmpty()) {
                for (kw in analysis.searchKeywords.take(2)) {
                    if (!kw.equals(trimmed, ignoreCase = true)) {
                        try {
                            val newSongs = ItunesSearchApi.searchSongs(kw, limit = 10)
                            for (s in newSongs) {
                                val key = normalizeKey(s.title, s.artist)
                                if (seenKeys.add(key)) {
                                    existingSongs.add(s)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // Apply Gemini's intelligent ranking
            val rankedMap = analysis.rankedSongIds.withIndex().associate { it.value to it.index }
            val sortedByAi = existingSongs.sortedWith(
                Comparator { a, b ->
                    val rankA = rankedMap[a.id] ?: 999
                    val rankB = rankedMap[b.id] ?: 999
                    rankA.compareTo(rankB)
                }
            )

            val filtered = applyFiltersAndSorting(sortedByAi, filters)

            stage1Results.copy(
                songs = filtered,
                aiAnalysis = analysis,
                isAiPowered = true,
                totalMatches = filtered.size
            )
        } catch (_: Exception) {
            stage1Results
        }
    }

    /**
     * Spotify-Style Smart Autoplay & Recommendations Engine:
     * Generates next suitable tracks based on current song's artist, genre, language, and vibe,
     * while strictly excluding recently played songs to prevent repetition.
     */
    fun getSmartRecommendations(
        currentSong: Song?,
        excludeIds: Set<Int> = emptySet(),
        limit: Int = 10
    ): List<Song> {
        val allCatalog = (MusicCatalog.songs + prefsManager.getDownloadedSongsMetadata()).distinctBy { it.id }
        if (currentSong == null) {
            return allCatalog.filter { !excludeIds.contains(it.id) }.shuffled().take(limit)
        }

        val candidates = allCatalog.filter { !excludeIds.contains(it.id) && it.id != currentSong.id }

        val scored = candidates.map { song ->
            var score = 0
            if (song.artist.equals(currentSong.artist, ignoreCase = true)) score += 60
            if (song.genre.equals(currentSong.genre, ignoreCase = true)) score += 35
            if (song.language.equals(currentSong.language, ignoreCase = true)) score += 25
            val currentYear = currentSong.releaseYear.toIntOrNull() ?: 2024
            val songYear = song.releaseYear.toIntOrNull() ?: 2024
            if (Math.abs(currentYear - songYear) <= 3) score += 10
            song to score
        }.sortedByDescending { it.second }

        val topScored = scored.map { it.first }.take(limit)
        return if (topScored.size >= limit) {
            topScored
        } else {
            val remaining = candidates.filter { !topScored.contains(it) }.shuffled()
            (topScored + remaining).distinctBy { it.id }.take(limit)
        }
    }

    private fun searchLocalCatalog(query: String, catalog: List<Song>): List<Song> {
        val q = query.lowercase()
        return catalog.filter { song ->
            song.title.lowercase().contains(q) ||
            song.artist.lowercase().contains(q) ||
            song.album.lowercase().contains(q) ||
            song.genre.lowercase().contains(q) ||
            song.language.lowercase().contains(q) ||
            localAliases.entries.any { (k, v) ->
                q.contains(k) && (song.artist.contains(v, ignoreCase = true) || song.title.contains(v, ignoreCase = true))
            }
        }
    }

    private fun searchFuzzyCatalog(query: String, catalog: List<Song>): List<Song> {
        val tokens = query.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        if (tokens.isEmpty()) return emptyList()

        return catalog.filter { song ->
            val hayStack = "${song.title} ${song.artist} ${song.album} ${song.genre} ${song.language}".lowercase()
            tokens.any { hayStack.contains(it) }
        }
    }

    fun applyFiltersAndSorting(songs: List<Song>, filters: ActiveMusicFilters): List<Song> {
        var result = songs.asSequence()

        // Content Type
        when (filters.contentType) {
            ContentType.SONGS -> {}
            ContentType.ARTISTS -> {
                result = result.distinctBy { it.artist.lowercase() }
            }
            ContentType.ALBUMS -> {
                result = result.filter { it.album.isNotBlank() }.distinctBy { "${it.album}__${it.artist}".lowercase() }
            }
            ContentType.ALL -> {}
        }

        // Genre filter
        if (filters.genre != GenreFilter.ALL) {
            val genreTarget = filters.genre.label.lowercase()
            result = result.filter { song ->
                song.genre.lowercase().contains(genreTarget) ||
                (filters.genre == GenreFilter.BOLLYWOOD && (song.genre.contains("Indian", ignoreCase = true) || song.language.equals("Hindi", ignoreCase = true))) ||
                (filters.genre == GenreFilter.PUNJABI && song.language.equals("Punjabi", ignoreCase = true)) ||
                (filters.genre == GenreFilter.DANCE && (song.genre.contains("Electronic", ignoreCase = true) || song.genre.contains("EDM", ignoreCase = true)))
            }
        }

        // Language filter
        if (filters.language != LanguageFilter.ALL) {
            val langTarget = filters.language.label.lowercase()
            result = result.filter { song ->
                song.language.lowercase().contains(langTarget)
            }
        }

        // Year filter
        if (filters.year != YearFilter.ALL) {
            result = result.filter { song ->
                val yearInt = song.releaseYear.toIntOrNull() ?: 2024
                when (filters.year) {
                    YearFilter.CURRENT_2024_2026 -> yearInt >= 2024
                    YearFilter.YEARS_2020_2023 -> yearInt in 2020..2023
                    YearFilter.DECADE_2010S -> yearInt in 2010..2019
                    YearFilter.DECADE_2000S -> yearInt in 2000..2009
                    YearFilter.CLASSIC -> yearInt < 2000
                    YearFilter.ALL -> true
                }
            }
        }

        // Duration filter
        if (filters.duration != DurationFilter.ANY) {
            result = result.filter { song ->
                val seconds = song.durationMs / 1000
                when (filters.duration) {
                    DurationFilter.SHORT -> seconds < 180
                    DurationFilter.MEDIUM -> seconds in 180..240
                    DurationFilter.LONG -> seconds > 240
                    DurationFilter.ANY -> true
                }
            }
        }

        // Explicitness
        if (filters.explicit != ExplicitFilter.ALL) {
            result = result.filter { song ->
                when (filters.explicit) {
                    ExplicitFilter.CLEAN_ONLY -> !song.isExplicit
                    ExplicitFilter.EXPLICIT_ONLY -> song.isExplicit
                    ExplicitFilter.ALL -> true
                }
            }
        }

        // Availability (Downloaded only)
        if (filters.availability == AvailabilityFilter.DOWNLOADED_ONLY) {
            val downloadedIds = prefsManager.getDownloadedSongsMetadata().map { it.id }.toSet()
            result = result.filter { song -> downloadedIds.contains(song.id) || song.isDownloaded }
        }

        val list = result.toList()

        // Sort Option
        return when (filters.sortOption) {
            SortOption.BEST_MATCH -> list
            SortOption.POPULARITY -> list.sortedByDescending { it.popularity }
            SortOption.NEWEST -> list.sortedByDescending { it.releaseYear }
            SortOption.OLDEST -> list.sortedBy { it.releaseYear }
            SortOption.TITLE_A_Z -> list.sortedBy { it.title.lowercase() }
            SortOption.ARTIST_A_Z -> list.sortedBy { it.artist.lowercase() }
        }
    }

    private fun normalizeKey(title: String, artist: String): String {
        val t = title.lowercase().replace(Regex("[^a-z0-9]"), "")
        val a = artist.lowercase().replace(Regex("[^a-z0-9]"), "")
        return "${t}_$a"
    }
}
