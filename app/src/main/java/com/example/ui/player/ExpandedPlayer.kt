package com.example.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Song
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassCircleButton
import com.example.ui.theme.AppleMusicRed
import com.example.util.PerformanceMode

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayer(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    isRepeat: Boolean,
    volume: Float,
    isFavorite: Boolean,
    showLyrics: Boolean,
    showQueue: Boolean,
    showVideoCanvas: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    performanceMode: PerformanceMode,
    selectedAudioRoute: String,
    queue: List<Song>,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    onToggleVideoCanvas: () -> Unit,
    onDownloadSong: () -> Unit,
    onOpenAudioRoute: () -> Unit,
    onSelectSongFromQueue: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic artwork scale animation
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.86f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "artworkScale"
    )

    // Favorite star scale bounce
    val starScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "starScale"
    )

    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Music Video / Ambient Glow Background
        MusicVideoBackground(
            song = song,
            isPlaying = isPlaying,
            positionMs = positionMs,
            showVideoCanvas = showVideoCanvas,
            performanceMode = performanceMode,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Foreground Glass UI Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Drag Handle & Quick Actions Row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video Canvas Toggle Button
                if (song.videoUrl != null) {
                    IconButton(
                        onClick = onToggleVideoCanvas,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_video_canvas")
                    ) {
                        Icon(
                            imageVector = if (showVideoCanvas) Icons.Filled.Videocam else Icons.Outlined.Videocam,
                            contentDescription = "Toggle Video Background",
                            tint = if (showVideoCanvas) AppleMusicRed else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }

                // Centered Drag Pill
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.45f))
                        .testTag("expanded_player_drag_handle")
                )

                // Download Button / Status
                IconButton(
                    onClick = onDownloadSong,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("download_song_button")
                ) {
                    when {
                        downloadProgress != null -> {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                strokeWidth = 2.dp,
                                color = AppleMusicRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        isDownloaded -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded for Offline Play",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Song",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Body: Crossfade between (Artwork & Info), (Lyrics), (Queue)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Crossfade(
                    targetState = when {
                        showLyrics -> "lyrics"
                        showQueue -> "queue"
                        else -> "player"
                    },
                    label = "playerContent"
                ) { state ->
                    when (state) {
                        "lyrics" -> {
                            LyricsView(
                                song = song,
                                currentPositionMs = positionMs,
                                durationMs = durationMs
                            )
                        }
                        "queue" -> {
                            QueueView(
                                queue = queue,
                                currentSong = song,
                                isPlaying = isPlaying,
                                onSongSelect = onSelectSongFromQueue
                            )
                        }
                        else -> {
                            PlayerMainArtworkAndInfo(
                                song = song,
                                artworkScale = artworkScale,
                                isFavorite = isFavorite,
                                starScale = starScale,
                                showMenu = showMenu,
                                performanceMode = performanceMode,
                                onToggleFavorite = onToggleFavorite,
                                onShowMenu = { showMenu = it }
                            )
                        }
                    }
                }
            }

            // Bottom Liquid Glass Controls Panel
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color(0xFF141418).copy(alpha = 0.65f),
                elevation = 12.dp,
                performanceMode = performanceMode
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Scrubber Timeline Slider
                    var isScrubbing by remember { mutableStateOf(false) }
                    var scrubPosition by remember { mutableFloatStateOf(0f) }

                    val currentProgress = if (isScrubbing) {
                        scrubPosition
                    } else if (durationMs > 0) {
                        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = currentProgress,
                        onValueChange = {
                            isScrubbing = true
                            scrubPosition = it
                        },
                        onValueChangeFinished = {
                            isScrubbing = false
                            onSeek((scrubPosition * durationMs).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White.copy(alpha = 0.9f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .testTag("player_progress_slider")
                    )

                    val effectivePos = if (isScrubbing) (scrubPosition * durationMs).toLong() else positionMs
                    val remainingMs = (durationMs - effectivePos).coerceAtLeast(0L)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(effectivePos),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "-${formatTime(remainingMs)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Playback Controls Row: Shuffle | Skip Back | Play/Pause Glass Button | Skip Next | Repeat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier.testTag("button_shuffle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) AppleMusicRed else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("button_previous")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Song",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Prominent Liquid Glass Play/Pause Button
                        GlassCircleButton(
                            onClick = onPlayPause,
                            size = 64.dp,
                            backgroundColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.testTag("button_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("button_next")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Song",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleRepeat,
                            modifier = Modifier.testTag("button_repeat")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isRepeat) AppleMusicRed else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Volume Slider Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = "Volume Down",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White.copy(alpha = 0.9f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .height(22.dp)
                                .testTag("player_volume_slider")
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume Up",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Utility Toolbar: Lyrics | Audio Route | Queue
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleLyrics,
                            modifier = Modifier.testTag("button_lyrics")
                        ) {
                            Icon(
                                imageVector = if (showLyrics) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Lyrics",
                                tint = if (showLyrics) AppleMusicRed else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenAudioRoute)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Airplay,
                                contentDescription = "Audio Route",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedAudioRoute,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        IconButton(
                            onClick = onToggleQueue,
                            modifier = Modifier.testTag("button_queue")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (showQueue) AppleMusicRed else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerMainArtworkAndInfo(
    song: Song,
    artworkScale: Float,
    isFavorite: Boolean,
    starScale: Float,
    showMenu: Boolean,
    performanceMode: PerformanceMode,
    onToggleFavorite: () -> Unit,
    onShowMenu: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large Album Artwork with responsive sizing & spring scale
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val artSize = (maxWidth - 16.dp).coerceAtMost(maxHeight - 16.dp).coerceAtMost(350.dp)

            Box(
                modifier = Modifier
                    .size(artSize)
                    .scale(artworkScale)
                    .shadow(
                        elevation = if (performanceMode == PerformanceMode.BATTERY_SAVER) 4.dp else (24 * artworkScale).dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = Color.Black.copy(alpha = 0.6f),
                        ambientColor = Color.Black.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = song.artwork,
                    contentDescription = "${song.title} album cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title, Artist, Favorite & More Options Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (song.album.isNotBlank()) {
                    Text(
                        text = song.album,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Favorite star button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .scale(starScale)
                    .testTag("button_favorite")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) AppleMusicRed else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(26.dp)
                )
            }

            // More Options menu button
            Box {
                IconButton(
                    onClick = { onShowMenu(true) },
                    modifier = Modifier.testTag("button_more_options")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More Options",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { onShowMenu(false) },
                    modifier = Modifier.background(Color(0xFF28282C))
                ) {
                    DropdownMenuItem(
                        text = { Text("View Album", color = Color.White) },
                        onClick = { onShowMenu(false) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Song", color = Color.White) },
                        onClick = { onShowMenu(false) }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = Color.White) },
                        onClick = { onShowMenu(false) }
                    )
                    DropdownMenuItem(
                        text = { Text("Create Station", color = Color.White) },
                        onClick = { onShowMenu(false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
