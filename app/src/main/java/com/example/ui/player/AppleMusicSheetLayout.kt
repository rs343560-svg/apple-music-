package com.example.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.player.MusicPlayerState
import com.example.player.MusicPlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppleMusicSheetLayout(
    playerState: MusicPlayerState,
    viewModel: MusicPlayerViewModel,
    bottomBar: @Composable () -> Unit,
    backgroundContent: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Collect isolated playback position for maximum UI fluidity
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()

    // Expansion progress: 0f = collapsed (mini player), 1f = fully expanded
    val expansionProgress = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressOffset by remember { mutableFloatStateOf(0f) }

    val isExpanded by remember {
        derivedStateOf { expansionProgress.value > 0.05f }
    }

    // Spring physics spec matching iOS Apple Music sheet
    val sheetSpringSpec = remember {
        spring<Float>(
            dampingRatio = 0.82f,
            stiffness = 380f
        )
    }

    // Android back button handling: smoothly collapses the sheet if open
    BackHandler(enabled = isExpanded) {
        coroutineScope.launch {
            expansionProgress.animateTo(0f, animationSpec = sheetSpringSpec)
        }
    }

    // Audio route selection dialog
    if (playerState.showAudioRoute) {
        AudioRouteDialog(
            currentRoute = playerState.selectedAudioRoute,
            onSelectRoute = { viewModel.setAudioRoute(it) },
            onDismiss = { viewModel.setShowAudioRoute(false) }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val bottomNavHeightDp = 78.dp

        // Current real-time progress combining animation and interactive drag
        val currentProgress = (expansionProgress.value + dragProgressOffset).coerceIn(0f, 1f)

        // Background Scaling Transformation:
        val bgScale = 1.0f - (currentProgress * 0.08f)
        val bgCornerRadiusDp = (currentProgress * 34f).dp
        val bgTranslationY = (currentProgress * with(density) { 16.dp.toPx() })
        val bgDimAlpha = currentProgress * 0.38f

        // 1. Root Background Content (Scales down, gets rounded, and dims)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                    translationY = bgTranslationY
                    clip = currentProgress > 0.01f
                    shape = RoundedCornerShape(bgCornerRadiusDp)
                }
                .background(Color(0xFF141416))
        ) {
            backgroundContent()

            // Dim overlay over background content while sheet is expanding
            if (bgDimAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = bgDimAlpha))
                )
            }
        }

        // 2. Bottom Navigation Bar (Fades and slides out when sheet is expanding)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    alpha = (1f - currentProgress * 2f).coerceIn(0f, 1f)
                    translationY = currentProgress * with(density) { bottomNavHeightDp.toPx() }
                }
        ) {
            bottomBar()
        }

        // 3. Mini Player (Visible when progress is near 0, resting right above bottom nav bar)
        val song = playerState.currentSong
        if (song != null) {
            val miniPlayerAlpha = (1f - currentProgress * 3.5f).coerceIn(0f, 1f)
            if (miniPlayerAlpha > 0.01f) {
                val velocityTracker = remember { VelocityTracker() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = -with(density) { (bottomNavHeightDp + 4.dp).roundToPx() }
                            )
                        }
                        .graphicsLayer {
                            alpha = miniPlayerAlpha
                            translationY = -currentProgress * with(density) { 30.dp.toPx() }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { velocityTracker.resetTracking() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    if (dragAmount.y < 0) {
                                        val deltaFraction = -dragAmount.y / screenHeightPx
                                        dragProgressOffset = (dragProgressOffset + deltaFraction).coerceIn(0f, 1f)
                                    }
                                },
                                onDragEnd = {
                                    val velocityY = velocityTracker.calculateVelocity().y
                                    val shouldOpen = dragProgressOffset > 0.15f || velocityY < -600f
                                    coroutineScope.launch {
                                        dragProgressOffset = 0f
                                        if (shouldOpen) {
                                            expansionProgress.animateTo(1f, animationSpec = sheetSpringSpec)
                                        } else {
                                            expansionProgress.animateTo(0f, animationSpec = sheetSpringSpec)
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    val progressFraction = if (playerState.durationMs > 0) {
                        positionMs.toFloat() / playerState.durationMs
                    } else 0f

                    MiniPlayer(
                        song = song,
                        isPlaying = playerState.isPlaying,
                        progressFraction = progressFraction,
                        performanceMode = playerState.performanceMode,
                        onExpand = {
                            coroutineScope.launch {
                                expansionProgress.animateTo(1f, animationSpec = sheetSpringSpec)
                            }
                        },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playNext() }
                    )
                }
            }
        }

        // 4. Full-screen Expanded Player Sheet (Slides up, interactive drag gestures)
        if (song != null && currentProgress > 0.01f) {
            val sheetTopRadius = (36f * currentProgress).dp
            val sheetOffsetY = screenHeightPx * (1f - currentProgress)
            val sheetVelocityTracker = remember { VelocityTracker() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, sheetOffsetY.roundToInt()) }
                    .shadow(
                        elevation = if (playerState.performanceMode == com.example.util.PerformanceMode.BATTERY_SAVER) 4.dp else (28 * currentProgress).dp,
                        shape = RoundedCornerShape(topStart = sheetTopRadius, topEnd = sheetTopRadius)
                    )
                    .clip(RoundedCornerShape(topStart = sheetTopRadius, topEnd = sheetTopRadius))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                sheetVelocityTracker.resetTracking()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                sheetVelocityTracker.addPosition(change.uptimeMillis, change.position)

                                val deltaFraction = -dragAmount.y / screenHeightPx
                                dragProgressOffset = (dragProgressOffset + deltaFraction).coerceIn(
                                    -expansionProgress.value,
                                    1f - expansionProgress.value
                                )
                            },
                            onDragEnd = {
                                isDragging = false
                                val finalProgress = (expansionProgress.value + dragProgressOffset).coerceIn(0f, 1f)
                                val velocityY = sheetVelocityTracker.calculateVelocity().y
                                val shouldDismiss = (finalProgress < 0.75f) || (velocityY > 800f)

                                coroutineScope.launch {
                                    expansionProgress.snapTo(finalProgress)
                                    dragProgressOffset = 0f
                                    if (shouldDismiss) {
                                        expansionProgress.animateTo(0f, animationSpec = sheetSpringSpec)
                                    } else {
                                        expansionProgress.animateTo(1f, animationSpec = sheetSpringSpec)
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                coroutineScope.launch {
                                    dragProgressOffset = 0f
                                    expansionProgress.animateTo(1f, animationSpec = sheetSpringSpec)
                                }
                            }
                        )
                    }
            ) {
                val isDownloaded = playerState.downloadedSongIds.contains(song.id)
                val downloadProgress = playerState.downloadProgress[song.id]

                ExpandedPlayer(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    positionMs = positionMs,
                    durationMs = playerState.durationMs,
                    isShuffle = playerState.isShuffle,
                    isRepeat = playerState.isRepeat,
                    volume = playerState.volume,
                    isFavorite = playerState.favorites.contains(song.id),
                    showLyrics = playerState.showLyrics,
                    showQueue = playerState.showQueue,
                    showVideoCanvas = playerState.showVideoCanvas,
                    isDownloaded = isDownloaded,
                    downloadProgress = downloadProgress,
                    performanceMode = playerState.performanceMode,
                    selectedAudioRoute = playerState.selectedAudioRoute,
                    queue = playerState.queue,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onSeek = { viewModel.seekTo(it) },
                    onVolumeChange = { viewModel.setVolume(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                    onToggleLyrics = { viewModel.toggleLyrics() },
                    onToggleQueue = { viewModel.toggleQueue() },
                    onToggleVideoCanvas = { viewModel.toggleVideoCanvas() },
                    onDownloadSong = { viewModel.downloadSong(song) },
                    onOpenAudioRoute = { viewModel.setShowAudioRoute(true) },
                    onSelectSongFromQueue = { viewModel.playSong(it) }
                )
            }
        }
    }
}
