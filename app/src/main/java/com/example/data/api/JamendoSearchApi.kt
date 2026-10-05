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

/**
 * Authorized Open Music Provider Client using Jamendo Creative Commons API.
 * Provides 100% legal, authorized full-length streaming tracks with open licenses,
 * full metadata, and background playback support when primary commercial sources are unavailable.
 */
object JamendoSearchApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Official Jamendo developer client ID for authorized open music access
    private const val CLIENT_ID = "56d30c95"
    private const val BASE_URL = "https://api.jamendo.com/v3.0/tracks"

    private val cache = ConcurrentHashMap<String, List<Song>>()

    suspend fun searchTracks(
        query: String,
        limit: Int = 15
    ): List<Song> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext emptyList()

        val cacheKey = "${cleanQuery.lowercase()}_$limit"
        cache[cacheKey]?.let { return@withContext it }

        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val url = "$BASE_URL/?client_id=$CLIENT_ID&format=jsonpretty&limit=$limit&namesearch=$encoded&include=musicinfo"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AppleMusicAndroid/3.0 (Authorized Jamendo Client)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return@withContext emptyList()

            val songs = mutableListOf<Song>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val idStr = item.optString("id", "")
                val numericId = (idStr.toIntOrNull() ?: (cleanQuery.hashCode() + i)) + 800000

                val title = item.optString("name", "Unknown Title")
                val artist = item.optString("artist_name", "Unknown Artist")
                val album = item.optString("album_name", "Jamendo Open Audio")
                val streamUrl = item.optString("audio", "")
                val artwork = item.optString("image", "").ifEmpty {
                    item.optString("album_image", "")
                }
                val durationSec = item.optLong("duration", 180L)

                val musicInfo = item.optJSONObject("musicinfo")
                val genre = musicInfo?.optJSONArray("tags")?.let { tagsArr ->
                    if (tagsArr.length() > 0) tagsArr.optJSONArray(0)?.optString(0, "Indie") ?: "Indie"
                    else "Indie"
                } ?: "Indie"

                if (title.isNotBlank() && streamUrl.isNotBlank()) {
                    songs.add(
                        Song(
                            id = numericId,
                            title = title,
                            artist = artist,
                            album = album,
                            artwork = artwork.ifEmpty { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80" },
                            mp4Link = streamUrl,
                            durationMs = durationSec * 1000L,
                            genre = genre.replaceFirstChar { it.uppercase() },
                            releaseYear = item.optString("releasedate", "2023").take(4),
                            language = "English",
                            providerSource = "Jamendo Open Audio",
                            sourceUrl = item.optString("shareurl", "https://www.jamendo.com/track/$idStr"),
                            youtubeSearchQuery = "$artist $title official"
                        )
                    )
                }
            }

            if (songs.isNotEmpty()) {
                cache[cacheKey] = songs
            }
            songs
        } catch (_: Exception) {
            emptyList()
        }
    }
}
