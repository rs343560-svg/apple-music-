package com.example.model

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String = "",
    val artwork: String,
    val mp4Link: String,
    val videoUrl: String? = null,
    val artworkBgColor: String = "#333333",
    val durationMs: Long = 30000L,
    val lyrics: List<String> = emptyList(),
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val genre: String = "Pop",
    val releaseYear: String = "2024",
    val isExplicit: Boolean = false,
    val language: String = "English",
    val popularity: Int = 80,
    val isPlayable: Boolean = true,
    val providerSource: String = "Apple Music", // "Apple Music", "Jamendo Open Audio", "Curated Vault", "Offline Library"
    val sourceUrl: String? = null,
    val youtubeSearchQuery: String? = null
)
