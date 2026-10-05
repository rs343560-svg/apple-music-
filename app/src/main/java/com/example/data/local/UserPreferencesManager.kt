package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Song
import com.example.util.PerformanceMode
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val songIds: List<Int> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

class UserPreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("apple_music_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key_custom"
        private const val KEY_FAVORITES = "user_favorites_set"
        private const val KEY_FAVORITE_SONGS_JSON = "user_favorites_songs_json"
        private const val KEY_RECENTLY_PLAYED_JSON = "user_recently_played_json"
        private const val KEY_PLAYLISTS_JSON = "user_playlists_json"
        private const val KEY_DOWNLOADED_SONGS_JSON = "user_downloaded_songs_json"
        private const val KEY_PERFORMANCE_MODE = "user_performance_mode"
        private const val KEY_AUDIO_QUALITY = "user_audio_quality"
        private const val KEY_RECENT_SEARCHES_JSON = "user_recent_searches_json"
        private const val KEY_GEMINI_STATUS = "user_gemini_status"
        private const val KEY_LAST_PLAYED_SONG_ID = "user_last_played_song_id"
        private const val KEY_LAST_PLAYED_POSITION_MS = "user_last_played_pos_ms"
        private const val KEY_SAVED_QUEUE_IDS = "user_saved_queue_ids"
        private const val KEY_AUTOPLAY_ENABLED = "user_autoplay_enabled"
    }

    // --- Gemini Connection Status ---
    fun getGeminiStatus(): String {
        val key = getCustomGeminiApiKey()
        if (key.isBlank()) return "NOT_CONFIGURED"
        return prefs.getString(KEY_GEMINI_STATUS, "CONNECTED") ?: "CONNECTED"
    }

    fun setGeminiStatus(status: String) {
        prefs.edit().putString(KEY_GEMINI_STATUS, status).apply()
    }

    // --- Recent Searches ---
    fun getRecentSearches(): List<String> {
        val json = prefs.getString(KEY_RECENT_SEARCHES_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || trimmed.length < 2) return
        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val arr = JSONArray()
        current.take(15).forEach { arr.put(it) }
        prefs.edit().putString(KEY_RECENT_SEARCHES_JSON, arr.toString()).apply()
    }

    fun removeRecentSearch(query: String) {
        val current = getRecentSearches().filter { !it.equals(query, ignoreCase = true) }
        val arr = JSONArray()
        current.forEach { arr.put(it) }
        prefs.edit().putString(KEY_RECENT_SEARCHES_JSON, arr.toString()).apply()
    }

    fun clearRecentSearches() {
        prefs.edit().remove(KEY_RECENT_SEARCHES_JSON).apply()
    }

    // --- Gemini API Key ---
    fun getCustomGeminiApiKey(): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun setCustomGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun clearCustomGeminiApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    // --- Favorites ---
    fun getFavoriteIds(): Set<Int> {
        val stringSet = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun toggleFavorite(song: Song): Boolean {
        val currentIds = getFavoriteIds().toMutableSet()
        val isFav = if (currentIds.contains(song.id)) {
            currentIds.remove(song.id)
            removeFavoriteSongMetadata(song.id)
            false
        } else {
            currentIds.add(song.id)
            saveFavoriteSongMetadata(song)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, currentIds.map { it.toString() }.toSet()).apply()
        return isFav
    }

    private fun saveFavoriteSongMetadata(song: Song) {
        try {
            val list = getFavoriteSongs().toMutableList()
            list.removeAll { it.id == song.id }
            list.add(0, song)
            val jsonArray = JSONArray()
            list.take(100).forEach { s -> jsonArray.put(songToJson(s)) }
            prefs.edit().putString(KEY_FAVORITE_SONGS_JSON, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun removeFavoriteSongMetadata(songId: Int) {
        try {
            val list = getFavoriteSongs().filter { it.id != songId }
            val jsonArray = JSONArray()
            list.forEach { s -> jsonArray.put(songToJson(s)) }
            prefs.edit().putString(KEY_FAVORITE_SONGS_JSON, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getFavoriteSongs(): List<Song> {
        val jsonStr = prefs.getString(KEY_FAVORITE_SONGS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val songs = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                songs.add(jsonToSong(obj))
            }
            songs
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- Recently Played ---
    fun addRecentlyPlayed(song: Song) {
        try {
            val list = getRecentlyPlayed().toMutableList()
            list.removeAll { it.id == song.id }
            list.add(0, song)
            val jsonArray = JSONArray()
            list.take(30).forEach { s -> jsonArray.put(songToJson(s)) }
            prefs.edit().putString(KEY_RECENTLY_PLAYED_JSON, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getRecentlyPlayed(): List<Song> {
        val jsonStr = prefs.getString(KEY_RECENTLY_PLAYED_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val songs = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                songs.add(jsonToSong(obj))
            }
            songs
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- Custom Playlists ---
    fun getPlaylists(): List<Playlist> {
        val jsonStr = prefs.getString(KEY_PLAYLISTS_JSON, null)
        if (jsonStr.isNullOrEmpty()) {
            // Default built-in starter playlists
            val defaultPlaylists = listOf(
                Playlist(id = "p_fav", name = "Favorite Tracks", description = "Your starred favorites"),
                Playlist(id = "p_chill", name = "Chill & Relax", description = "Mellow beats & acoustic sounds"),
                Playlist(id = "p_party", name = "Party Hits", description = "High-energy bangers")
            )
            savePlaylists(defaultPlaylists)
            return defaultPlaylists
        }
        return try {
            val arr = JSONArray(jsonStr)
            val playlists = mutableListOf<Playlist>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                val name = obj.optString("name", "Untitled")
                val desc = obj.optString("description", "")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val songIdsArr = obj.optJSONArray("songIds")
                val songIds = mutableListOf<Int>()
                if (songIdsArr != null) {
                    for (j in 0 until songIdsArr.length()) {
                        songIds.add(songIdsArr.getInt(j))
                    }
                }
                playlists.add(Playlist(id, name, desc, songIds, createdAt))
            }
            playlists
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePlaylists(playlists: List<Playlist>) {
        try {
            val arr = JSONArray()
            playlists.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("description", p.description)
                    put("createdAt", p.createdAt)
                    val sArr = JSONArray()
                    p.songIds.forEach { sArr.put(it) }
                    put("songIds", sArr)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_PLAYLISTS_JSON, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun createPlaylist(name: String, description: String = ""): Playlist {
        val newPlaylist = Playlist(
            id = "p_" + System.currentTimeMillis(),
            name = name,
            description = description
        )
        val current = getPlaylists().toMutableList()
        current.add(0, newPlaylist)
        savePlaylists(current)
        return newPlaylist
    }

    fun addSongToPlaylist(playlistId: String, songId: Int) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val p = current[index]
            if (!p.songIds.contains(songId)) {
                current[index] = p.copy(songIds = p.songIds + songId)
                savePlaylists(current)
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Int) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val p = current[index]
            current[index] = p.copy(songIds = p.songIds - songId)
            savePlaylists(current)
        }
    }

    fun deletePlaylist(playlistId: String) {
        val current = getPlaylists().filter { it.id != playlistId }
        savePlaylists(current)
    }

    // --- Offline Downloaded Songs Metadata Cache ---
    fun saveDownloadedSong(song: Song) {
        try {
            val current = getDownloadedSongsMetadata().toMutableList()
            current.removeAll { it.id == song.id }
            current.add(song.copy(isDownloaded = true))
            val arr = JSONArray()
            current.forEach { arr.put(songToJson(it)) }
            prefs.edit().putString(KEY_DOWNLOADED_SONGS_JSON, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun removeDownloadedSong(songId: Int) {
        try {
            val current = getDownloadedSongsMetadata().filter { it.id != songId }
            val arr = JSONArray()
            current.forEach { arr.put(songToJson(it)) }
            prefs.edit().putString(KEY_DOWNLOADED_SONGS_JSON, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getDownloadedSongsMetadata(): List<Song> {
        val jsonStr = prefs.getString(KEY_DOWNLOADED_SONGS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val songs = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                songs.add(jsonToSong(arr.getJSONObject(i)))
            }
            songs
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- Performance Mode ---
    fun getPerformanceMode(): PerformanceMode {
        val name = prefs.getString(KEY_PERFORMANCE_MODE, PerformanceMode.HIGH.name)
        return try {
            PerformanceMode.valueOf(name ?: PerformanceMode.HIGH.name)
        } catch (_: Exception) {
            PerformanceMode.HIGH
        }
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        prefs.edit().putString(KEY_PERFORMANCE_MODE, mode.name).apply()
    }

    // --- Autoplay & Playback Persistence ---
    fun isAutoplayEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTOPLAY_ENABLED, true)
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY_ENABLED, enabled).apply()
    }

    fun savePlaybackState(songId: Int?, queueIds: List<Int>, positionMs: Long) {
        val editor = prefs.edit()
        if (songId != null) {
            editor.putInt(KEY_LAST_PLAYED_SONG_ID, songId)
        } else {
            editor.remove(KEY_LAST_PLAYED_SONG_ID)
        }
        editor.putLong(KEY_LAST_PLAYED_POSITION_MS, positionMs)
        val arr = JSONArray()
        queueIds.take(100).forEach { arr.put(it) }
        editor.putString(KEY_SAVED_QUEUE_IDS, arr.toString())
        editor.apply()
    }

    fun getSavedPlaybackSongId(): Int? {
        if (!prefs.contains(KEY_LAST_PLAYED_SONG_ID)) return null
        val id = prefs.getInt(KEY_LAST_PLAYED_SONG_ID, -1)
        return if (id != -1) id else null
    }

    fun getSavedPlaybackPositionMs(): Long {
        return prefs.getLong(KEY_LAST_PLAYED_POSITION_MS, 0L)
    }

    fun getSavedPlaybackQueueIds(): List<Int> {
        val json = prefs.getString(KEY_SAVED_QUEUE_IDS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Int>()
            for (i in 0 until arr.length()) {
                list.add(arr.getInt(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // JSON serialization helpers
    private fun songToJson(song: Song): JSONObject {
        return JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("artwork", song.artwork)
            put("mp4Link", song.mp4Link)
            put("videoUrl", song.videoUrl ?: "")
            put("artworkBgColor", song.artworkBgColor)
            put("durationMs", song.durationMs)
            put("genre", song.genre)
            put("releaseYear", song.releaseYear)
            put("isExplicit", song.isExplicit)
            put("language", song.language)
            put("popularity", song.popularity)
            put("providerSource", song.providerSource)
            put("sourceUrl", song.sourceUrl ?: "")
            put("youtubeSearchQuery", song.youtubeSearchQuery ?: "")
        }
    }

    private fun jsonToSong(obj: JSONObject): Song {
        val videoUrl = obj.optString("videoUrl").ifEmpty { null }
        val sourceUrl = obj.optString("sourceUrl").ifEmpty { null }
        val youtubeQuery = obj.optString("youtubeSearchQuery").ifEmpty { null }
        return Song(
            id = obj.optInt("id", 0),
            title = obj.optString("title", "Unknown"),
            artist = obj.optString("artist", "Unknown"),
            album = obj.optString("album", ""),
            artwork = obj.optString("artwork", ""),
            mp4Link = obj.optString("mp4Link", ""),
            videoUrl = videoUrl,
            artworkBgColor = obj.optString("artworkBgColor", "#333333"),
            durationMs = obj.optLong("durationMs", 30000L),
            genre = obj.optString("genre", "Pop"),
            releaseYear = obj.optString("releaseYear", "2024"),
            isExplicit = obj.optBoolean("isExplicit", false),
            language = obj.optString("language", "English"),
            popularity = obj.optInt("popularity", 80),
            isDownloaded = true,
            providerSource = obj.optString("providerSource", "Apple Music"),
            sourceUrl = sourceUrl,
            youtubeSearchQuery = youtubeQuery
        )
    }
}
