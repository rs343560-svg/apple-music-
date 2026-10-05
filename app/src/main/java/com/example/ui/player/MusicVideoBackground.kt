package com.example.ui.player

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.Song
import com.example.util.PerformanceMode
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MusicVideoBackground(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    showVideoCanvas: Boolean,
    performanceMode: PerformanceMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoUrl = song.videoUrl

    // Ambient glow colors derived from song artwork color
    val dominantColor = remember(song.artworkBgColor) {
        try {
            val clean = song.artworkBgColor.removePrefix("#")
            Color(0xFF000000 or clean.toLong(16))
        } catch (_: Exception) {
            Color(0xFF8B4513)
        }
    }

    val secondaryColor = remember(dominantColor) {
        Color(
            red = (dominantColor.red * 0.7f + 0.3f).coerceIn(0f, 1f),
            green = (dominantColor.green * 0.5f).coerceIn(0f, 1f),
            blue = (dominantColor.blue * 0.8f + 0.2f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    // Slow moving ambient background glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "ambientGlow")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0A0A0C))) {
        // 1. Video Canvas (plays in background if enabled and video is available)
        val shouldPlayVideo = showVideoCanvas && videoUrl != null && performanceMode != PerformanceMode.BATTERY_SAVER

        AnimatedVisibility(
            visible = shouldPlayVideo,
            enter = fadeIn(tween(600)),
            exit = fadeOut(tween(400))
        ) {
            if (videoUrl != null) {
                VideoPlayerSurface(
                    videoUri = Uri.parse(videoUrl),
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Slow-Moving Ambient Glowing Orbs (GPU Canvas without recomposing whole tree)
        if (performanceMode != PerformanceMode.BATTERY_SAVER) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Orb 1 moving slowly in elliptical orbit
                val orb1X = w * (0.35f + 0.25f * sin(glowPhase))
                val orb1Y = h * (0.25f + 0.15f * cos(glowPhase))

                // Orb 2 moving in opposite phase
                val orb2X = w * (0.65f + 0.20f * cos(glowPhase * 0.7f))
                val orb2Y = h * (0.55f + 0.20f * sin(glowPhase * 0.7f))

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = if (shouldPlayVideo) 0.35f else 0.65f),
                            dominantColor.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(orb1X, orb1Y),
                        radius = w * 0.85f
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = w * 0.85f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = if (shouldPlayVideo) 0.25f else 0.45f),
                            secondaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(orb2X, orb2Y),
                        radius = w * 0.75f
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = w * 0.75f
                )
            }
        } else {
            // Static lightweight gradient on low-end devices
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                dominantColor.copy(alpha = 0.5f),
                                Color(0xFF101014),
                                Color(0xFF0A0A0C)
                            )
                        )
                    )
            )
        }

        // 3. Frosted Dark Liquid Glass Scrim (Ensures 100% controls readability)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = if (shouldPlayVideo) 0.62f else 0.45f),
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun VideoPlayerSurface(
    videoUri: Uri,
    isPlaying: Boolean,
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(videoUri) {
        var surface: Surface? = null
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, videoUri)
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(0f, 0f) // Muted background video; audio is played by main player
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {}

        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.reset()
                mediaPlayer.release()
                surface?.release()
            } catch (_: Exception) {}
        }
    }

    // Sync play/pause with audio playback
    LaunchedEffect(isPlaying) {
        try {
            if (isPlaying && !mediaPlayer.isPlaying) {
                mediaPlayer.start()
            } else if (!isPlaying && mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        } catch (_: Exception) {}
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        try {
                            val surf = Surface(st)
                            mediaPlayer.setSurface(surf)
                            if (isPlaying) {
                                mediaPlayer.start()
                            }
                        } catch (_: Exception) {}
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        mediaPlayer.setSurface(null)
                        return true
                    }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
}
