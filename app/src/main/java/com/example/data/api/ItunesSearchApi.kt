package com.example.data.api

import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ItunesSearchApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    // Fast in-memory cache to prevent duplicate requests and smooth UI
    private val queryCache = ConcurrentHashMap<String, List<Song>>()

    // Vibrant background colors for artwork ambience fallback
    private val ambientColors = listOf(
        "#8B4513", "#2F4F4F", "#CD853F", "#5C2D91", "#3E2723",
        "#8B2500", "#A55F47", "#880E4F", "#2C3E50", "#6A1B9A",
        "#283593", "#4E342E", "#1A1A1D", "#7A5C00", "#1A365D",
        "#C2185B", "#1565C0", "#B7410E", "#7B1FA2", "#1A237E"
    )

    /**
     * Search iTunes for songs matching [query] with [limit] and [offset].
     * Query both international ("US") and Indian ("IN") storefronts when relevant
     * to provide comprehensive catalog for Hindi, Punjabi, and International music.
     */
    suspend fun searchSongs(
        query: String,
        limit: Int = 30,
        offset: Int = 0,
        country: String? = null
    ): List<Song> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext emptyList()

        val cacheKey = "${cleanQuery.lowercase()}_${limit}_${offset}_${country ?: "auto"}"
        queryCache[cacheKey]?.let { return@withContext it }

        val countriesToQuery = if (country != null) {
            listOf(country)
        } else if (isIndicQuery(cleanQuery)) {
            listOf("IN", "US")
        } else {
            listOf("US", "IN")
        }

        val allResults = mutableListOf<Song>()
        val seenTrackKeys = mutableSetOf<String>()

        for (c in countriesToQuery) {
            try {
                val songs = executeQuery(cleanQuery, limit, offset, c)
                for (song in songs) {
                    val normalizedKey = normalizeSongKey(song.title, song.artist)
                    if (seenTrackKeys.add(normalizedKey)) {
                        allResults.add(song)
                    }
                }
                if (allResults.size >= limit) break
            } catch (_: Exception) {
                // Ignore per-storefront failure and try next
            }
        }

        if (allResults.isNotEmpty()) {
            queryCache[cacheKey] = allResults
        }

        allResults
    }

    private fun executeQuery(
        query: String,
        limit: Int,
        offset: Int,
        country: String
    ): List<Song> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=$limit&offset=$offset&country=$country"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AppleMusicAndroid/3.0")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        val json = JSONObject(body)
        val resultsArray = json.optJSONArray("results") ?: return emptyList()

        val songs = mutableListOf<Song>()
        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.getJSONObject(i)

            val previewUrl = item.optString("previewUrl")
            if (previewUrl.isNullOrEmpty() || !previewUrl.startsWith("http")) {
                // Skip songs without authorized playable preview
                continue
            }

            val trackId = item.optInt("trackId", (query.hashCode() * 31 + i).coerceAtLeast(1000))
            val trackName = cleanTitle(item.optString("trackName", "Unknown Title"))
            val artistName = item.optString("artistName", "Unknown Artist")
            val collectionName = item.optString("collectionName", "")
            val rawArtwork = item.optString("artworkUrl100", "")

            // Enhance resolution to 600x600 for sharp liquid glass display
            val artworkUrl = if (rawArtwork.isNotEmpty()) {
                rawArtwork.replace("100x100bb.jpg", "600x600bb.jpg")
                    .replace("100x100bb.png", "600x600bb.png")
            } else {
                "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/cb/64/8c/cb648cf7-e7bb-bd00-efe0-d744312c29a8/24UMGIM32304.rgb.jpg/600x600bb.jpg"
            }

            val primaryGenre = item.optString("primaryGenreName", "Pop")
            val releaseDate = item.optString("releaseDate", "")
            val releaseYear = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2024"
            val trackExplicitness = item.optString("trackExplicitness", "notExplicit")
            val isExplicit = trackExplicitness.equals("explicit", ignoreCase = true)
            val trackTimeMillis = item.optLong("trackTimeMillis", 30000L)

            val detectedLanguage = detectLanguage(trackName, artistName, primaryGenre, country)
            val colorIndex = Math.abs(trackId.hashCode()) % ambientColors.size
            val trackViewUrl = item.optString("trackViewUrl", "")

            songs.add(
                Song(
                    id = trackId,
                    title = trackName,
                    artist = artistName,
                    album = collectionName,
                    artwork = artworkUrl,
                    mp4Link = previewUrl,
                    videoUrl = null,
                    artworkBgColor = ambientColors[colorIndex],
                    durationMs = 30000L.coerceAtMost(trackTimeMillis),
                    genre = primaryGenre,
                    releaseYear = releaseYear,
                    isExplicit = isExplicit,
                    language = detectedLanguage,
                    popularity = 85,
                    isPlayable = true,
                    providerSource = "Apple Music",
                    sourceUrl = trackViewUrl.ifEmpty { null },
                    youtubeSearchQuery = "$artistName $trackName official"
                )
            )
        }
        return songs
    }

    private fun normalizeSongKey(title: String, artist: String): String {
        val cleanT = cleanTitle(title).lowercase().replace(Regex("[^a-z0-9]"), "")
        val cleanA = artist.lowercase().replace(Regex("[^a-z0-9]"), "")
        return "${cleanT}_${cleanA}"
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\s*\\(from.*\\)"), "")
            .replace(Regex("(?i)\\s*\\[from.*\\]"), "")
            .replace(Regex("(?i)\\s*\\(feat.*\\)"), "")
            .replace(Regex("(?i)\\s*\\[feat.*\\]"), "")
            .replace(Regex("(?i)\\s*\\(official.*\\)"), "")
            .trim()
    }

    private fun isIndicQuery(query: String): Boolean {
        val q = query.lowercase()
        val indicKeywords = listOf(
            "arijit", "diljit", "kesariya", "sidhu", "moosewala", "ap dhillon",
            "shreya", "pritam", "hindi", "punjabi", "bollywood", "badshah", "honey singh",
            "jubin", "neha", "kakkar", "armaan", "malik", "atif", "aslam", "b praak",
            "guru randhawa", "karan aujla", "shubh", "mc stan", "anuv jain", "channa mereya",
            "tum hi ho", "raataan lambiyan", "desi", "bhangra", "sufi", "ghazal", "barsaat",
            "banjaare", "himesh", "sonu nigam", "alka yagnik", "kumar sanu", "udit narayan"
        )
        return indicKeywords.any { q.contains(it) } || query.any { it.code in 0x0900..0x097F || it.code in 0x0A00..0x0A7F }
    }

    private fun detectLanguage(title: String, artist: String, genre: String, country: String): String {
        val combined = "$title $artist $genre".lowercase()
        return when {
            combined.contains("punjabi") || combined.contains("diljit") || combined.contains("dhillon") || combined.contains("aujla") -> "Punjabi"
            combined.contains("hindi") || combined.contains("bollywood") || combined.contains("arijit") || combined.contains("pritam") || combined.contains("shreya") -> "Hindi"
            combined.contains("spanish") || combined.contains("latino") || combined.contains("reggaeton") -> "Spanish"
            country == "IN" && (genre.contains("Bollywood") || genre.contains("Indian")) -> "Hindi"
            else -> "English"
        }
    }
}
