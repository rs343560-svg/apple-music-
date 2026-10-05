package com.example.data.api

import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiQueryAnalysis(
    val correctedQuery: String,
    val identifiedArtist: String? = null,
    val identifiedSong: String? = null,
    val genre: String? = null,
    val language: String? = null,
    val mood: String? = null,
    val era: String? = null,
    val searchKeywords: List<String> = emptyList(),
    val similarRecommendations: List<String> = emptyList(),
    val rankedSongIds: List<Int> = emptyList(),
    val explanation: String
)

object GeminiSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /**
     * Test whether an API key is valid by sending a lightweight ping prompt.
     */
    suspend fun validateApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext false
        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$cleanKey"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Ping test"))
                        })
                    })
                })
            }
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Stage 2 AI Analysis & Intelligent Ranking:
     * Analyzes query intent (e.g. 'sad songs', 'arijit singh romantic', 'latest hindi songs',
     * '90s bollywood', 'gym music', 'barsaat banjaare'), resolves typos and Hinglish,
     * suggests targeted provider keywords, and intelligently ranks the real candidate songs.
     *
     * STRICTURE: Gemini does NOT invent songs or playback URLs. It only ranks and classifies
     * the real candidate songs from the connected provider.
     */
    suspend fun analyzeAndRank(
        query: String,
        candidateSongs: List<Song>,
        apiKey: String
    ): AiQueryAnalysis? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanQuery = query.trim()
        if (cleanKey.isBlank() || cleanQuery.isBlank()) return@withContext null

        try {
            val candidatesSummary = JSONArray()
            candidateSongs.take(25).forEach { song ->
                candidatesSummary.put(
                    JSONObject().apply {
                        put("id", song.id)
                        put("title", song.title)
                        put("artist", song.artist)
                        put("genre", song.genre)
                        put("language", song.language)
                        put("year", song.releaseYear)
                    }
                )
            }

            val systemInstruction = """
                You are a smart music intelligence engine for Apple Music / Spotify.
                Your task is to understand user search queries (which may be in English, Hindi, Hinglish, mood/vibe, or have typos like "barsaat banjaare", "arijit singh romantic", "sad songs", "gym music", "90s bollywood") and rank real candidate songs.

                CRITICAL RULES:
                1. DO NOT invent songs, artists, albums, or playback URLs.
                2. Evaluate the provided real candidate songs and rank their IDs in order of relevance (best match first).
                3. If the user query implies a specific song/artist that is missing or misspelled (e.g., "barsaat banjaare" -> "Barsaat Ki Dhun" or "B Praak Banjaare"), include the accurate search keywords in "searchKeywords" so the provider can retrieve them.

                Output ONLY valid JSON with this schema:
                {
                  "correctedQuery": "string (cleaned up canonical search query)",
                  "identifiedArtist": "string or null",
                  "identifiedSong": "string or null",
                  "genre": "string or null",
                  "language": "string or null",
                  "mood": "string or null (e.g. Romantic, Sad, Workout, Party, Chill)",
                  "era": "string or null (e.g. 90s, 2000s, 2024)",
                  "searchKeywords": ["2 to 3 alternative search keywords for provider"],
                  "rankedSongIds": [integer IDs from candidates ordered by relevance],
                  "explanation": "short 1-sentence friendly explanation of the user's intent"
                }
            """.trimIndent()

            val userContent = "User Query: \"$cleanQuery\"\nCandidate Songs JSON: ${candidatesSummary.toString()}"

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userContent))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$cleanKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseStr = response.body?.string() ?: return@withContext null
            val root = JSONObject(responseStr)
            val candidates = root.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null

            val rawText = parts.getJSONObject(0).optString("text", "")
            if (rawText.isBlank()) return@withContext null

            val cleanJson = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = JSONObject(cleanJson)

            val keywordsList = mutableListOf<String>()
            parsed.optJSONArray("searchKeywords")?.let { arr ->
                for (i in 0 until arr.length()) keywordsList.add(arr.getString(i))
            }

            val rankedIds = mutableListOf<Int>()
            parsed.optJSONArray("rankedSongIds")?.let { arr ->
                for (i in 0 until arr.length()) rankedIds.add(arr.getInt(i))
            }

            val similarRecs = mutableListOf<String>()
            parsed.optJSONArray("similarRecommendations")?.let { arr ->
                for (i in 0 until arr.length()) similarRecs.add(arr.getString(i))
            }

            AiQueryAnalysis(
                correctedQuery = parsed.optString("correctedQuery", cleanQuery),
                identifiedArtist = parsed.optString("identifiedArtist").ifEmpty { null },
                identifiedSong = parsed.optString("identifiedSong").ifEmpty { null },
                genre = parsed.optString("genre").ifEmpty { null },
                language = parsed.optString("language").ifEmpty { null },
                mood = parsed.optString("mood").ifEmpty { null },
                era = parsed.optString("era").ifEmpty { null },
                searchKeywords = if (keywordsList.isNotEmpty()) keywordsList else listOf(cleanQuery),
                similarRecommendations = similarRecs,
                rankedSongIds = rankedIds,
                explanation = parsed.optString("explanation", "Understood intent: $cleanQuery")
            )
        } catch (_: Exception) {
            null
        }
    }
}
