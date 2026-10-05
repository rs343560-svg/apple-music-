package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.data.MusicCatalog
import com.example.model.Song
import com.example.util.PerformanceManager
import com.example.util.PerformanceMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MediaPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "apple_music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_STOP = "com.example.action.STOP"

        // Global engine for immediate UI connection
        val engine = MusicPlayerEngine()

        fun startService(context: Context) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java)
                try {
                    context.startService(intent)
                } catch (_: Exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var notificationManager: NotificationManager? = null
    private var currentArtworkBitmap: Bitmap? = null

    private var progressJob: Job? = null
    private var isPrepared = false

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    private var audioFocusRequest: AudioFocusRequest? = null

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        createNotificationChannel()
        setupMediaSession()

        // Link engine with this active service first
        engine.attachService(this)

        // Immediately fulfill Android foreground service contract
        startForegroundSafely()

        // Check offline files on disk
        engine.checkDownloadedSongs(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafely()

        when (intent?.action) {
            ACTION_PLAY -> resumePlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrevious()
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows media controls for active song playback"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "AppleMusicSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { resumePlayback() }
                override fun onPause() { pausePlayback() }
                override fun onSkipToNext() { playNext() }
                override fun onSkipToPrevious() { playPrevious() }
                override fun onSeekTo(pos: Long) { seekTo(pos) }
                override fun onStop() { stopPlayback() }
            })
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
        }
    }

    private fun setupMediaPlayer() {
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            } catch (_: Exception) {}
            val vol = engine.state.value.volume
            setVolume(vol, vol)
        }
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        pausePlayback()
                    }
                }
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        pausePlayback()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    fun playSong(song: Song) {
        val currentQueue = if (engine.state.value.queue.contains(song)) {
            engine.state.value.queue
        } else {
            listOf(song) + engine.state.value.queue
        }
        engine.updateState {
            it.copy(
                currentSong = song,
                queue = currentQueue,
                isBuffering = true
            )
        }
        engine.setPosition(0L)

        // Load artwork bitmap for lock screen and notification
        loadArtworkBitmap(song)

        prepareSong(song, autoPlay = true)
    }

    private fun prepareSong(song: Song, autoPlay: Boolean) {
        setupMediaPlayer()
        isPrepared = false
        try {
            val localFile = File(filesDir, "downloads/${song.id}.m4a")
            val dataSource = if (localFile.exists()) localFile.absolutePath else song.mp4Link

            mediaPlayer?.apply {
                setDataSource(dataSource)
                setOnPreparedListener { mp ->
                    isPrepared = true
                    val dur = mp.duration.toLong().coerceAtLeast(1000L)
                    engine.updateState {
                        it.copy(
                            isBuffering = false,
                            durationMs = if (dur > 0) dur else song.durationMs
                        )
                    }
                    if (autoPlay) {
                        if (requestAudioFocus()) {
                            mp.start()
                            engine.updateState { it.copy(isPlaying = true) }
                            startProgressTracker()
                            startForegroundSafely()
                        }
                    } else {
                        updateNotificationAndMediaSession()
                    }
                }
                setOnCompletionListener {
                    if (engine.state.value.isRepeat) {
                        it.seekTo(0)
                        it.start()
                    } else {
                        playNext()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    engine.updateState { it.copy(isBuffering = false, isPlaying = false) }
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            engine.updateState { it.copy(isBuffering = false, isPlaying = false) }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        val isPlaying = engine.state.value.isPlaying
        if (isPlaying) {
            pausePlayback()
        } else {
            if (isPrepared && player != null) {
                if (requestAudioFocus()) {
                    player.start()
                    engine.updateState { it.copy(isPlaying = true) }
                    startProgressTracker()
                    startForegroundSafely()
                }
            } else {
                engine.state.value.currentSong?.let {
                    prepareSong(it, autoPlay = true)
                }
            }
        }
    }

    fun resumePlayback() {
        if (!engine.state.value.isPlaying) {
            togglePlayPause()
        }
    }

    fun pausePlayback() {
        val player = mediaPlayer
        if (isPrepared && player?.isPlaying == true) {
            player.pause()
        }
        engine.updateState { it.copy(isPlaying = false) }
        stopProgressTracker()
        updateNotificationAndMediaSession()
    }

    fun playNext() {
        val queue = engine.state.value.queue
        if (queue.isEmpty()) return
        val currentSong = engine.state.value.currentSong
        val isShuffle = engine.state.value.isShuffle
        val isAutoplay = engine.state.value.isAutoplayEnabled

        val nextSong = if (isShuffle) {
            val remaining = queue.filter { it.id != currentSong?.id }
            if (remaining.isNotEmpty()) remaining.random() else queue.first()
        } else {
            val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
            if (currentIndex in queue.indices && currentIndex + 1 < queue.size) {
                queue[currentIndex + 1]
            } else if (isAutoplay) {
                // Spotify-Style Smart Autoplay: Pick next suitable song and append to queue!
                val autoplaySong = engine.getAutoplayCandidate()
                if (autoplaySong != null) {
                    engine.addToQueue(autoplaySong)
                    autoplaySong
                } else {
                    queue.first()
                }
            } else {
                queue.first()
            }
        }
        playSong(nextSong)
    }

    fun playPrevious() {
        val player = mediaPlayer
        if (isPrepared && player != null && player.currentPosition > 3000) {
            player.seekTo(0)
            engine.setPosition(0L)
            updateNotificationAndMediaSession()
            return
        }
        val queue = engine.state.value.queue
        if (queue.isEmpty()) return
        val currentSong = engine.state.value.currentSong
        val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }
        val prevSong = if (currentIndex > 0) {
            queue[currentIndex - 1]
        } else {
            queue.last()
        }
        playSong(prevSong)
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer
        if (isPrepared && player != null) {
            val safePos = positionMs.coerceIn(0L, engine.state.value.durationMs).toInt()
            player.seekTo(safePos)
        }
        engine.setPosition(positionMs)
        updateNotificationAndMediaSession()
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        engine.updateState { it.copy(volume = clamped) }
        mediaPlayer?.setVolume(clamped, clamped)
    }

    fun stopPlayback() {
        pausePlayback()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun loadArtworkBitmap(song: Song) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(this@MediaPlaybackService)
                val request = ImageRequest.Builder(this@MediaPlaybackService)
                    .data(song.artwork)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                withContext(Dispatchers.Main) {
                    currentArtworkBitmap = bitmap
                    updateNotificationAndMediaSession()
                }
            } catch (_: Exception) {}
        }
    }

    private fun startForegroundSafely() {
        val notification = try {
            buildMediaNotification()
        } catch (_: Exception) {
            buildFallbackNotification()
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }
        try {
            updateMediaSessionState()
        } catch (_: Exception) {}
    }

    private fun buildFallbackNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Apple Music")
                .setContentText("Playing")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Apple Music")
                .setContentText("Playing")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        }
    }

    private fun updateNotificationAndMediaSession() {
        try {
            val notification = buildMediaNotification()
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
        try {
            updateMediaSessionState()
        } catch (_: Exception) {}
    }

    private fun updateMediaSessionState() {
        val song = engine.state.value.currentSong ?: return
        val isPlaying = engine.state.value.isPlaying
        val position = engine.positionMs.value

        // Update PlaybackState for lock screen
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                position,
                1.0f
            )
        mediaSession?.setPlaybackState(stateBuilder.build())

        // Update MediaMetadata for lock screen
        val metaBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, engine.state.value.durationMs)

        currentArtworkBitmap?.let {
            metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
        }
        mediaSession?.setMetadata(metaBuilder.build())
    }

    private fun buildMediaNotification(): Notification {
        val song = engine.state.value.currentSong ?: MusicCatalog.songs.first()
        val isPlaying = engine.state.value.isPlaying

        // Content Intent: Opens MainActivity
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents
        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album.ifBlank { "Apple Music" })
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)

        currentArtworkBitmap?.let {
            builder.setLargeIcon(it)
        }

        // Add Previous Action
        builder.addAction(
            Notification.Action.Builder(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevIntent
            ).build()
        )

        // Add Play/Pause Action
        builder.addAction(
            Notification.Action.Builder(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            ).build()
        )

        // Add Next Action
        builder.addAction(
            Notification.Action.Builder(
                android.R.drawable.ic_media_next,
                "Next",
                nextIntent
            ).build()
        )

        // MediaStyle attachment
        val mediaStyle = Notification.MediaStyle()
        mediaSession?.let {
            mediaStyle.setMediaSession(it.sessionToken)
            mediaStyle.setShowActionsInCompactView(0, 1, 2)
        }
        builder.style = mediaStyle

        return builder.build()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                if (isPrepared && mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    engine.setPosition(pos)
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releasePlayer() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPrepared = false
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
        engine.detachService()
    }
}

/**
 * Singleton Music Player Engine holding StateFlows and dispatching commands.
 * Connects the Android Foreground Service with Jetpack Compose UI seamlessly.
 */
class MusicPlayerEngine {
    private var boundService: MediaPlaybackService? = null

    private val _state = MutableStateFlow(
        MusicPlayerState(
            currentSong = MusicCatalog.songs.firstOrNull(),
            queue = MusicCatalog.songs,
            durationMs = 30000L,
            performanceMode = PerformanceMode.HIGH
        )
    )
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private var pendingSongToPlay: Song? = null
    var autoplayCandidateSupplier: ((Song?, Set<Int>) -> Song?)? = null

    fun getAutoplayCandidate(): Song? {
        val current = _state.value.currentSong
        val exclude = _state.value.queue.map { it.id }.toSet()
        return autoplayCandidateSupplier?.invoke(current, exclude)
    }

    fun toggleAutoplay() {
        _state.update { it.copy(isAutoplayEnabled = !it.isAutoplayEnabled) }
    }

    fun attachService(service: MediaPlaybackService) {
        boundService = service
        val pending = pendingSongToPlay
        if (pending != null) {
            pendingSongToPlay = null
            service.playSong(pending)
        }
    }

    fun detachService() {
        boundService = null
    }

    fun updateState(transform: (MusicPlayerState) -> MusicPlayerState) {
        _state.update(transform)
    }

    fun setPosition(pos: Long) {
        _positionMs.value = pos
    }

    fun playSong(song: Song) {
        val currentQueue = if (_state.value.queue.any { it.id == song.id }) {
            _state.value.queue
        } else {
            listOf(song) + _state.value.queue
        }
        _state.update { it.copy(queue = currentQueue, currentSong = song) }
        val service = boundService
        if (service != null) {
            service.playSong(song)
        } else {
            pendingSongToPlay = song
        }
    }

    fun addToQueue(song: Song) {
        _state.update { current ->
            if (current.queue.any { it.id == song.id }) current
            else current.copy(queue = current.queue + song)
        }
    }

    fun playNextInQueue(song: Song) {
        _state.update { current ->
            val currSong = current.currentSong
            val filtered = current.queue.filter { it.id != song.id }
            val currIndex = filtered.indexOfFirst { it.id == currSong?.id }
            val newQueue = if (currIndex != -1 && currIndex + 1 <= filtered.size) {
                filtered.toMutableList().apply { add(currIndex + 1, song) }
            } else {
                listOf(song) + filtered
            }
            current.copy(queue = newQueue)
        }
    }

    fun removeFromQueue(songId: Int) {
        _state.update { current ->
            current.copy(queue = current.queue.filter { it.id != songId })
        }
    }

    fun clearQueue() {
        _state.update { current ->
            current.copy(queue = current.currentSong?.let { listOf(it) } ?: emptyList())
        }
    }

    fun togglePlayPause() {
        boundService?.togglePlayPause()
    }

    fun playNext() {
        boundService?.playNext()
    }

    fun playPrevious() {
        boundService?.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        boundService?.seekTo(positionMs) ?: setPosition(positionMs)
    }

    fun setVolume(volume: Float) {
        boundService?.setVolume(volume) ?: run {
            _state.update { it.copy(volume = volume.coerceIn(0f, 1f)) }
        }
    }

    fun toggleShuffle() {
        _state.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        _state.update { it.copy(isRepeat = !it.isRepeat) }
    }

    fun toggleFavorite(songId: Int) {
        _state.update { current ->
            val newFavorites = if (current.favorites.contains(songId)) {
                current.favorites - songId
            } else {
                current.favorites + songId
            }
            current.copy(favorites = newFavorites)
        }
    }

    fun toggleLyrics() {
        _state.update { it.copy(showLyrics = !it.showLyrics, showQueue = false) }
    }

    fun toggleQueue() {
        _state.update { it.copy(showQueue = !it.showQueue, showLyrics = false) }
    }

    fun toggleVideoCanvas() {
        _state.update { it.copy(showVideoCanvas = !it.showVideoCanvas) }
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        _state.update { it.copy(performanceMode = mode) }
    }

    fun setShowAudioRoute(show: Boolean) {
        _state.update { it.copy(showAudioRoute = show) }
    }

    fun setAudioRoute(route: String) {
        _state.update { it.copy(selectedAudioRoute = route, showAudioRoute = false) }
    }

    fun shuffleAll() {
        val shuffled = MusicCatalog.songs.shuffled()
        _state.update { it.copy(queue = shuffled, isShuffle = true) }
        shuffled.firstOrNull()?.let { playSong(it) }
    }

    fun checkDownloadedSongs(context: Context) {
        val dir = File(context.filesDir, "downloads")
        if (dir.exists()) {
            val downloadedIds = dir.listFiles()?.mapNotNull { file ->
                file.nameWithoutExtension.toIntOrNull()
            }?.toSet() ?: emptySet()
            _state.update { it.copy(downloadedSongIds = downloadedIds) }
        }
    }

    fun downloadSong(context: Context, song: Song, scope: CoroutineScope) {
        if (_state.value.downloadedSongIds.contains(song.id)) return

        scope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(downloadProgress = it.downloadProgress + (song.id to 0.05f))
            }

            try {
                val dir = File(context.filesDir, "downloads").apply { mkdirs() }
                val destFile = File(dir, "${song.id}.m4a")

                val url = URL(song.mp4Link)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.connect()

                val fileLength = conn.contentLength
                val input = conn.inputStream
                val output = FileOutputStream(destFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)
                    if (fileLength > 0) {
                        val progress = (total.toFloat() / fileLength).coerceIn(0f, 1f)
                        _state.update {
                            it.copy(downloadProgress = it.downloadProgress + (song.id to progress))
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    com.example.data.local.UserPreferencesManager(context).saveDownloadedSong(song)
                    _state.update {
                        it.copy(
                            downloadedSongIds = it.downloadedSongIds + song.id,
                            downloadProgress = it.downloadProgress - song.id
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update {
                        it.copy(downloadProgress = it.downloadProgress - song.id)
                    }
                }
            }
        }
    }

    fun deleteDownload(context: Context, songId: Int, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val file = File(context.filesDir, "downloads/$songId.m4a")
            if (file.exists()) file.delete()
            com.example.data.local.UserPreferencesManager(context).removeDownloadedSong(songId)
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(downloadedSongIds = it.downloadedSongIds - songId)
                }
            }
        }
    }
}
