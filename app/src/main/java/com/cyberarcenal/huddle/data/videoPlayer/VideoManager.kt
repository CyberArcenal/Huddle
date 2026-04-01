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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
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

    // Gumamit ng Any key para suportahan ang maraming anchors para sa iisang URL
    private val anchors = mutableMapOf<Any, AnchorInfo>()
    private var screenSize = IntSize.Zero

    private var positionProvider: VideoPositionProvider = DefaultVideoPositionProvider()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackEvents = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 16)
    val playbackEvents: SharedFlow<PlaybackEvent> = _playbackEvents.asSharedFlow()

    private val _isMuted = MutableStateFlow(true)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val evaluationScope = CoroutineScope(Dispatchers.Main)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val evaluationTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var isManuallyPaused = false
    private val externallyManagedUrls = mutableSetOf<String>()

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
        if (enabled) {
            externallyManagedUrls.add(url)
        } else {
            externallyManagedUrls.remove(url)
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

        val bestUrl = positionProvider.getBestVideoUrl(anchors, screenSize)

        if (bestUrl != _currentVideoUrl.value) {
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
                if (externallyManagedUrls.contains(bestUrl)) {
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
        } else if (bestUrl != null && !isManuallyPaused) {
            val player = _currentPlayer.value
            if (player != null && !player.isPlaying && player.playbackState != Player.STATE_BUFFERING) {
                if (externallyManagedUrls.contains(bestUrl)) {
                    _playbackEvents.tryEmit(PlaybackEvent.ShouldPlay(bestUrl))
                } else {
                    player.playWhenReady = true
                    _isPlaying.value = true
                }
            }
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        val existing = _currentPlayer.value
        if (existing != null) return existing

        val newPlayer = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (_isMuted.value) 0f else 1f
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("VideoPlayerManager", "ExoPlayer Error: ${error.message}")
                    stop()
                }
            })
        }
        _currentPlayer.value = newPlayer
        return newPlayer
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
        externallyManagedUrls.clear()
        _currentPlayer.value?.release()
        _currentPlayer.value = null
        _currentVideoUrl.value = null
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
    fun getBestVideoUrl(anchors: Map<Any, VideoPlayerManager.AnchorInfo>, screenSize: IntSize): String?
}

class DefaultVideoPositionProvider(
    private val minVisiblePercentage: Float = 0.2f
) : VideoPositionProvider {
    override fun getBestVideoUrl(anchors: Map<Any, VideoPlayerManager.AnchorInfo>, screenSize: IntSize): String? {
        if (anchors.isEmpty()) return null

        val screenRect = Rect(0f, 0f, screenSize.width.toFloat(), screenSize.height.toFloat())

        return anchors.values.mapNotNull { info ->
            val rect = info.bounds
            // Gumamit ng overlap check para iwas sa invalid rect area
            if (!rect.overlaps(screenRect)) return@mapNotNull null

            val visibleRect = rect.intersect(screenRect)
            val visibleArea = visibleRect.width * visibleRect.height
            val totalArea = rect.width * rect.height

            if (totalArea > 0f && (visibleArea / totalArea) >= minVisiblePercentage) {
                info.url to visibleArea
            } else {
                null
            }
        }.maxByOrNull { it.second }?.first
    }
}

sealed class PlaybackEvent {
    data class Play(val url: String) : PlaybackEvent()
    data class ShouldPlay(val url: String) : PlaybackEvent()
    object Pause : PlaybackEvent()
    object Resume : PlaybackEvent()
    object Stop : PlaybackEvent()
}
