package com.example.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song

@Composable
fun LyricsView(
    song: Song,
    currentPositionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val lyrics = if (song.lyrics.isNotEmpty()) {
        song.lyrics
    } else {
        listOf(
            "Sing along with ${song.artist}",
            "",
            "Lyrics are synced to",
            song.title,
            "",
            "Enjoy the music in Spatial Audio"
        )
    }

    // Determine active line based on position fraction
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val nonEmptyCount = lyrics.filter { it.isNotBlank() }.size
    val activeNonEmptyIndex = (progress * nonEmptyCount).toInt().coerceIn(0, (nonEmptyCount - 1).coerceAtLeast(0))

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            var currentNonEmptyCounter = 0
            lyrics.forEach { line ->
                if (line.isBlank()) {
                    Spacer(modifier = Modifier.height(28.dp))
                } else {
                    val isActive = currentNonEmptyCounter == activeNonEmptyIndex
                    currentNonEmptyCounter++

                    Text(
                        text = line,
                        fontSize = if (isActive) 28.sp else 24.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.38f),
                        lineHeight = 36.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
