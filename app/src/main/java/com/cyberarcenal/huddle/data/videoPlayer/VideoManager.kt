package com.cyberarcenal.huddle.data.videoPlayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.cache.CacheDataSource
import com.cyberarcenal.huddle.data.videoPlayer.VideoCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

private val Context.videoDataStore by preferencesDataStore("video_prefs")
private val IS_MUTED_KEY = booleanPreferencesKey("is_muted")

class VideoPlayerManager private constructor(private val context: Context) {

    data class AnchorInfo(val url: String, val bounds: Rect)

    private val _currentPlayer = MutableStateFlow<ExoPlayer?>(null)
    val currentPlayer: StateFlow<ExoPlayer?> = _currentPlayer.asStateFlow()

    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()

    private val _activeAnchorKey = MutableStateFlow<Any?>(null)
    val activeAnchorKey: StateFlow<Any?> = _activeAnchorKey.asStateFlow()

    // Gumamit ng Any key para suportahan ang maraming anchors para sa iisang URL
    private val anchors = mutableMapOf<Any, AnchorInfo>()
    private var screenSize = IntSize.Zero

    private var positionProvider: VideoPositionProvider = DefaultVideoPositionProvider()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackEvents = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 16)
    val playbackEvents: SharedFlow<PlaybackEvent> = _playbackEvents.asSharedFlow()

    private val _isMuted = MutableStateFlow(true)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val evaluationScope = CoroutineScope(Dispatchers.Main)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val evaluationTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var isManuallyPaused = false
    private val _externallyManagedUrls = MutableStateFlow<Set<String>>(emptySet())
    val externallyManagedUrls: StateFlow<Set<String>> = _externallyManagedUrls.asStateFlow()

    init {
        evaluationScope.launch {
            val savedMute = context.videoDataStore.data
                .map { it[IS_MUTED_KEY] ?: true }
                .first()
            _isMuted.value = savedMute
            _currentPlayer.value?.volume = if (savedMute) 0f else 1f
            triggerEvaluation()
        }

        @OptIn(FlowPreview::class)
        evaluationScope.launch {
            evaluationTrigger.debounce(50).collect {
                evaluate()
            }
        }

        evaluationScope.launch {
            while (true) {
                _currentPlayer.value?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition
                        _duration.value = player.duration
                    }
                }
                delay(500)
            }
        }
    }

    fun updateScreenSize(size: IntSize) {
        if (screenSize != size) {
            screenSize = size
            triggerEvaluation()
        }
    }

    fun updateAnchorBounds(key: Any, url: String, bounds: Rect) {
        val existing = anchors[key]
        if (existing == null || existing.url != url ||
            abs(existing.bounds.left - bounds.left) > 1f ||
            abs(existing.bounds.top - bounds.top) > 1f ||
            abs(existing.bounds.width - bounds.width) > 1f ||
            abs(existing.bounds.height - bounds.height) > 1f
        ) {
            anchors[key] = AnchorInfo(url, bounds)
            triggerEvaluation()
        }
    }

    fun removeAnchor(key: Any) {
        if (anchors.remove(key) != null) {
            triggerEvaluation()
        }
    }

    fun setExternalControl(url: String, enabled: Boolean) {
        _externallyManagedUrls.update { 
            if (enabled) it + url else it - url
        }
        triggerEvaluation()
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        _currentPlayer.value?.volume = if (muted) 0f else 1f
        evaluationScope.launch {
            context.videoDataStore.edit { it[IS_MUTED_KEY] = muted }
        }
    }

    fun toggleMute() {
        setMuted(!_isMuted.value)
    }

    fun setPositionProvider(provider: VideoPositionProvider) {
        positionProvider = provider
        triggerEvaluation()
    }

    private fun triggerEvaluation() {
        evaluationTrigger.tryEmit(Unit)
    }

    private fun evaluate() {
        if (screenSize == IntSize.Zero) return

        val bestEntry = positionProvider.getBestAnchor(anchors, screenSize)
        val bestUrl = bestEntry?.second?.url
        val bestKey = bestEntry?.first
        
        val managedUrls = _externallyManagedUrls.value

        if (bestUrl != _currentVideoUrl.value) {
            _activeAnchorKey.value = bestKey
            
            // Kung manually paused, i-update lang ang tracking pero huwag baguhin ang player state
            if (isManuallyPaused) {
                if (bestUrl == null) {
                    _currentVideoUrl.value = null
                    _isPlaying.value = false
                } else {
                    _currentVideoUrl.value = bestUrl
                }
                return
            }

            if (bestUrl != null) {
                if (managedUrls.contains(bestUrl)) {
                    _playbackEvents.tryEmit(PlaybackEvent.ShouldPlay(bestUrl))
                    // I-update ang current URL para alam ng UI sino ang 'active'
                    _currentVideoUrl.value = bestUrl
                    _isPlaying.value = true
                } else {
                    play(bestUrl)
                }
            } else {
                stop()
            }
        } else {
            // Kahit pareho ang URL, baka lumipat ng anchor (e.g. duplicate video in feed)
            if (_activeAnchorKey.value != bestKey) {
                _activeAnchorKey.value = bestKey
            }

            if (bestUrl != null && !isManuallyPaused) {
                val player = _currentPlayer.value
                if (player != null && !player.isPlaying && player.playbackState != Player.STATE_BUFFERING) {
                    if (managedUrls.contains(bestUrl)) {
                        _playbackEvents.tryEmit(PlaybackEvent.ShouldPlay(bestUrl))
                    } else {
                        player.playWhenReady = true
                        _isPlaying.value = true
                    }
                }
            }
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        val existing = _currentPlayer.value
        if (existing != null) return existing

        val cacheDataSourceFactory = VideoCache.createCacheDataSourceFactory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        val newPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (_isMuted.value) 0f else 1f
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = duration
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("VideoPlayerManager", "ExoPlayer Error: ${error.message}")
                    stop()
                }
            })
        }
        _currentPlayer.value = newPlayer
        return newPlayer
    }

    fun seekTo(positionMs: Long) {
        _currentPlayer.value?.let {
            it.seekTo(positionMs)
            _currentPosition.value = positionMs
        }
    }


    private fun play(url: String) {
        Log.d("VideoPlayerManager", "Playing: $url")
        val player = getOrCreatePlayer()
        _currentVideoUrl.value = url

        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
        _isPlaying.value = true
        _playbackEvents.tryEmit(PlaybackEvent.Play(url))

        requestAudioFocus()
    }

    private fun stop() {
        Log.d("VideoPlayerManager", "Stopping player")
        _currentPlayer.value?.let {
            it.playWhenReady = false
            it.stop()
            it.clearMediaItems()
        }
        _currentVideoUrl.value = null
        _isPlaying.value = false
        _playbackEvents.tryEmit(PlaybackEvent.Stop)
        abandonAudioFocus()
    }

    fun pause() {
        isManuallyPaused = true
        pausePlayback()
    }

    fun resume() {
        isManuallyPaused = false
        triggerEvaluation()
    }

    fun release() {
        isManuallyPaused = false
        _externallyManagedUrls.value = emptySet()
        _currentPlayer.value?.release()
        _currentPlayer.value = null
        _currentVideoUrl.value = null
        _activeAnchorKey.value = null
        anchors.clear()
        abandonAudioFocus()
    }

    fun onPause() = pausePlayback()
    fun onStop() = pausePlayback()
    fun onResume() {
        triggerEvaluation()
    }
    fun onDestroy() = release()


    private fun pausePlayback() {
        _currentPlayer.value?.playWhenReady = false
        _isPlaying.value = false
        _playbackEvents.tryEmit(PlaybackEvent.Pause)
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> pausePlayback()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        _currentPlayer.value?.volume = if (_isMuted.value) 0f else 0.3f
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        _currentPlayer.value?.volume = if (_isMuted.value) 0f else 1f
                        if (!isManuallyPaused) _currentPlayer.value?.playWhenReady = true
                    }
                }
            }.build()
        audioManager.requestAudioFocus(audioFocusRequest!!)
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    companion object {
        @Volatile
        private var INSTANCE: VideoPlayerManager? = null
        fun getInstance(context: Context): VideoPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VideoPlayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

interface VideoPositionProvider {
    fun getBestAnchor(anchors: Map<Any, VideoPlayerManager.AnchorInfo>, screenSize: IntSize): Pair<Any, VideoPlayerManager.AnchorInfo>?
    @Deprecated("Use getBestAnchor instead", ReplaceWith("getBestAnchor(anchors, screenSize)?.second?.url"))
    fun getBestVideoUrl(anchors: Map<Any, VideoPlayerManager.AnchorInfo>, screenSize: IntSize): String? = getBestAnchor(anchors, screenSize)?.second?.url
}

class DefaultVideoPositionProvider(
    private val minVisiblePercentage: Float = 0.2f
) : VideoPositionProvider {
    override fun getBestAnchor(anchors: Map<Any, VideoPlayerManager.AnchorInfo>, screenSize: IntSize): Pair<Any, VideoPlayerManager.AnchorInfo>? {
        if (anchors.isEmpty()) return null

        val screenRect = Rect(0f, 0f, screenSize.width.toFloat(), screenSize.height.toFloat())

        return anchors.entries.mapNotNull { (key, info) ->
            val rect = info.bounds
            // Gumamit ng overlap check para iwas sa invalid rect area
            if (!rect.overlaps(screenRect)) return@mapNotNull null

            val visibleRect = rect.intersect(screenRect)
            val visibleArea = visibleRect.width * visibleRect.height
            val totalArea = rect.width * rect.height

            if (totalArea > 0f && (visibleArea / totalArea) >= minVisiblePercentage) {
                key to (info to visibleArea)
            } else {
                null
            }
        }.maxByOrNull { it.second.second }?.let { it.first to it.second.first }
    }
}

sealed class PlaybackEvent {
    data class Play(val url: String) : PlaybackEvent()
    data class ShouldPlay(val url: String) : PlaybackEvent()
    object Pause : PlaybackEvent()
    object Resume : PlaybackEvent()
    object Stop : PlaybackEvent()
}
