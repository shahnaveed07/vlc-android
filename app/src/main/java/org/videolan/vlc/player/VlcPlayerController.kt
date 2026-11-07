/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.model.formatPlaybackDuration

@OptIn(UnstableApi::class)
class VlcPlayerController(
    private val context: Context,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val elapsedRealtimeMs: () -> Long = { SystemClock.elapsedRealtime() }
) {

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + mainDispatcher)

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 PLayMuse/3.6.0")
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(15000)
        .setAllowCrossProtocolRedirects(true)

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    private val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
    private val renderersFactory = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)

    var exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        private set

    private val _currentMedia = MutableStateFlow<MediaItem?>(null)
    val currentMedia: StateFlow<MediaItem?> = _currentMedia.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

    // Shared Sleep Timer state (in seconds)
    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

    private var sleepTimerDeadlineMs: Long = 0L

    // A–B Segment Repeat state
    private val _abPointAMs = MutableStateFlow<Long?>(null)
    val abPointAMs: StateFlow<Long?> = _abPointAMs.asStateFlow()

    private val _abPointBMs = MutableStateFlow<Long?>(null)
    val abPointBMs: StateFlow<Long?> = _abPointBMs.asStateFlow()

    private val _isAbPointASelected = MutableStateFlow(false)
    val isAbPointASelected: StateFlow<Boolean> = _isAbPointASelected.asStateFlow()

    private val _isAbPointBSelected = MutableStateFlow(false)
    val isAbPointBSelected: StateFlow<Boolean> = _isAbPointBSelected.asStateFlow()

    private val _isAbLoopActive = MutableStateFlow(false)
    val isAbLoopActive: StateFlow<Boolean> = _isAbLoopActive.asStateFlow()

    private val _abLoopStatusMessage = MutableStateFlow("Waiting to set A")
    val abLoopStatusMessage: StateFlow<String> = _abLoopStatusMessage.asStateFlow()

    private val _abLoopErrorMessage = MutableStateFlow<String?>(null)
    val abLoopErrorMessage: StateFlow<String?> = _abLoopErrorMessage.asStateFlow()

    private val _isCurrentMediaSeekable = MutableStateFlow(false)
    val isCurrentMediaSeekable: StateFlow<Boolean> = _isCurrentMediaSeekable.asStateFlow()

    private var abLoopMediaId: String? = null
    private var abLoopPlayerMessage: PlayerMessage? = null
    private var isRetryingSameMedia: Boolean = false
    private var customSeekableOverride: Boolean? = null
    private var codecRecoveryAttempted: Boolean = false

    // Equalizer & AudioFX
    private var androidEqualizer: Equalizer? = null
    private var androidBassBoost: BassBoost? = null
    private var androidVirtualizer: Virtualizer? = null

    private val _equalizerEnabled = MutableStateFlow(false)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _equalizerBands = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val equalizerBands: StateFlow<List<Int>> = _equalizerBands.asStateFlow()

    private val _bassBoostLevel = MutableStateFlow(0) // 0 - 1000
    val bassBoostLevel: StateFlow<Int> = _bassBoostLevel.asStateFlow()

    private val _virtualizerLevel = MutableStateFlow(0) // 0 - 1000
    val virtualizerLevel: StateFlow<Int> = _virtualizerLevel.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    val isSleepTimerJobActive: Boolean
        get() = sleepTimerJob?.isActive == true

    init {
        setupPlayerListener()
        startProgressUpdater()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    _playbackError.value = null
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val exoDur = exoPlayer.duration
                    val effectiveDur = if (exoDur > 0L && exoDur != C.TIME_UNSET) {
                        exoDur
                    } else {
                        (_currentMedia.value?.durationMs ?: 0L).coerceAtLeast(0L)
                    }
                    _duration.value = effectiveDur
                    val seekable = customSeekableOverride
                        ?: (effectiveDur > 0L && exoPlayer.isCurrentMediaItemSeekable && !exoPlayer.isCurrentMediaItemLive)
                    _isCurrentMediaSeekable.value = seekable

                    if (!seekable && (_isAbPointASelected.value || _isAbLoopActive.value)) {
                        clearAbLoop()
                        _abLoopStatusMessage.value = "A–B repeat is unavailable for live or unseekable media"
                    } else if (_isAbLoopActive.value) {
                        scheduleAbLoopPlayerMessage()
                    }

                    _playbackError.value = null
                    codecRecoveryAttempted = false
                    initAudioEffects(exoPlayer.audioSessionId)
                } else if (playbackState == Player.STATE_ENDED) {
                    // If A–B loop is active (e.g., B is at the end of the media), loop takes precedence over queue advancement
                    if (_isAbLoopActive.value &&
                        _abPointAMs.value != null &&
                        _abPointBMs.value != null &&
                        _currentMedia.value?.id != null &&
                        _currentMedia.value?.id == abLoopMediaId
                    ) {
                        onAbLoopBoundaryReached(abLoopMediaId)
                    } else {
                        playNext()
                    }
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (_isAbLoopActive.value && _currentMedia.value?.id == abLoopMediaId) {
                    scheduleAbLoopPlayerMessage()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val cause = error.cause
                val isCodecReclaimedOrFailed =
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                        cause is MediaCodecRenderer.DecoderInitializationException ||
                        (error.message?.contains("Released by resource manager", ignoreCase = true) == true) ||
                        (cause?.message?.contains("Released by resource manager", ignoreCase = true) == true)

                if (isCodecReclaimedOrFailed && !codecRecoveryAttempted && _currentMedia.value != null) {
                    codecRecoveryAttempted = true
                    val resumePos = _currentPosition.value.coerceAtLeast(0L)
                    val shouldPlay = _isPlaying.value
                    exoPlayer.prepare()
                    if (resumePos > 0L) {
                        exoPlayer.seekTo(resumePos)
                    }
                    exoPlayer.playWhenReady = shouldPlay
                    return
                }

                val message = when {
                    isCodecReclaimedOrFailed -> {
                        "Hardware media decoder was reclaimed by the system. Tap Retry to resume playback."
                    }
                    cause is HttpDataSource.InvalidResponseCodeException -> {
                        when (cause.responseCode) {
                            403 -> "Stream access forbidden (HTTP 403). The server requires authentication or has restricted this stream."
                            404 -> "Media stream not found (HTTP 404). Please verify the stream URL."
                            500, 502, 503 -> "Media server error (${cause.responseCode}). Please try again shortly."
                            else -> "Network error (HTTP ${cause.responseCode}). Unable to load stream."
                        }
                    }
                    cause is HttpDataSource.HttpDataSourceException -> {
                        "Network connection failed. Unable to connect to media host."
                    }
                    else -> error.message ?: "Playback error encountered."
                }
                _playbackError.value = message
                _isPlaying.value = false
            }
        })
    }

    private fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            androidEqualizer?.release()
            androidEqualizer = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerEnabled.value
                applyBandsToHardware()
            }

            androidBassBoost?.release()
            androidBassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _equalizerEnabled.value
                setStrength(_bassBoostLevel.value.toShort())
            }

            androidVirtualizer?.release()
            androidVirtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _equalizerEnabled.value
                setStrength(_virtualizerLevel.value.toShort())
            }
        } catch (e: Exception) {
            // AudioFX not supported on device or emulator
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration
                    if (dur > 0 && dur != C.TIME_UNSET) {
                        _duration.value = dur
                    }
                    onPlaybackPositionUpdated(pos)
                }
                delay(250)
            }
        }
    }

    fun onPlaybackPositionUpdated(positionMs: Long) {
        val safePos = positionMs.coerceAtLeast(0L)
        _currentPosition.value = safePos
        if (_isAbLoopActive.value && _currentMedia.value?.id == abLoopMediaId) {
            val bPos = _abPointBMs.value
            if (bPos != null && safePos >= bPos) {
                onAbLoopBoundaryReached(abLoopMediaId)
            }
        }
    }

    fun playMedia(item: MediaItem, queue: List<MediaItem>? = null) {
        val preserveAbLoop = isRetryingSameMedia && abLoopMediaId != null && abLoopMediaId == item.id
        isRetryingSameMedia = false
        codecRecoveryAttempted = false

        _playbackError.value = null
        _currentMedia.value = item
        _currentPosition.value = item.lastPositionMs.coerceAtLeast(0L)
        _duration.value = item.durationMs.coerceAtLeast(0L)
        customSeekableOverride = null
        _isCurrentMediaSeekable.value = item.durationMs > 0L

        if (!preserveAbLoop) {
            clearAbLoop()
            if (item.durationMs <= 0L) {
                _abLoopStatusMessage.value = "A–B repeat is unavailable for live or unknown-duration media"
            }
        }

        if (queue != null && queue.isNotEmpty()) {
            _playlist.value = queue
        } else if (_playlist.value.isEmpty()) {
            _playlist.value = listOf(item)
        }

        val exoItem = ExoMediaItem.fromUri(Uri.parse(item.uri))
        exoPlayer.setMediaItem(exoItem)
        exoPlayer.prepare()
        if (item.lastPositionMs > 0) {
            exoPlayer.seekTo(item.lastPositionMs)
        }
        exoPlayer.play()
        _isPlaying.value = true

        if (preserveAbLoop && _isAbLoopActive.value) {
            scheduleAbLoopPlayerMessage()
        }
    }

    fun retry() {
        val current = _currentMedia.value ?: return
        _playbackError.value = null
        isRetryingSameMedia = true
        playMedia(current, _playlist.value)
    }

    fun clearPlaybackError() {
        _playbackError.value = null
    }

    /**
     * Safely cleans up playback and playlist state when a media item is deleted from the device.
     * - Removes only the deleted item from [_playlist].
     * - If the deleted item is currently loaded/playing, stops ExoPlayer, clears active media state,
     *   and resets A–B repeat so no stale mini-player or full-screen player remains.
     * - If a different item is playing, leaves active playback uninterrupted.
     *
     * @return true if the deleted item was the currently loaded/playing media item.
     */
    fun onMediaItemDeleted(deletedItem: MediaItem): Boolean {
        val matchesDeleted: (MediaItem) -> Boolean = { candidate ->
            candidate.id == deletedItem.id || candidate.uri == deletedItem.uri
        }

        _playlist.value = _playlist.value.filterNot(matchesDeleted)

        val current = _currentMedia.value
        if (current != null && matchesDeleted(current)) {
            cancelAbLoopPlayerMessage()
            clearAbLoop()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            _isPlaying.value = false
            _currentMedia.value = null
            _currentPosition.value = 0L
            _duration.value = 0L
            _playbackError.value = null
            _isCurrentMediaSeekable.value = false
            customSeekableOverride = null
            return true
        }
        return false
    }

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        _playlist.value = items
        val target = items.getOrElse(startIndex) { items[0] }
        playMedia(target, items)
    }

    fun togglePlayPause() {
        if (_isPlaying.value || exoPlayer.isPlaying) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun pausePlayback() {
        exoPlayer.pause()
        _isPlaying.value = false
    }

    fun resumePlayback() {
        exoPlayer.play()
        _isPlaying.value = true
    }

    private fun resolveEffectiveDurationMs(): Long {
        val exoDur = exoPlayer.duration
        if (exoDur > 0L && exoDur != C.TIME_UNSET) return exoDur
        if (_duration.value > 0L) return _duration.value
        return (_currentMedia.value?.durationMs ?: 0L).coerceAtLeast(0L)
    }

    private fun currentPlaybackPositionMs(): Long {
        val exoPos = exoPlayer.currentPosition
        return if (exoPos > 0L) exoPos else _currentPosition.value.coerceAtLeast(0L)
    }

    fun setSeekableStateForMedia(seekable: Boolean, durationMs: Long = _duration.value) {
        customSeekableOverride = seekable
        _duration.value = durationMs.coerceAtLeast(0L)
        _isCurrentMediaSeekable.value = seekable && durationMs > 0L
        if (!_isCurrentMediaSeekable.value) {
            clearAbLoop()
            _abLoopStatusMessage.value = "A–B repeat is unavailable for live or unseekable media"
        }
    }

    fun canUseAbRepeat(): Boolean {
        if (_currentMedia.value == null) return false
        val dur = resolveEffectiveDurationMs()
        if (dur <= 0L) return false
        customSeekableOverride?.let { return it }
        if (exoPlayer.playbackState == Player.STATE_READY) {
            if (!exoPlayer.isCurrentMediaItemSeekable || exoPlayer.isCurrentMediaItemLive) {
                return false
            }
        }
        return _isCurrentMediaSeekable.value
    }

    fun setPointA(positionMs: Long = currentPlaybackPositionMs()): Boolean {
        if (!canUseAbRepeat()) {
            _abLoopErrorMessage.value = "A–B repeat is only available for seekable media with a known finite duration."
            _abLoopStatusMessage.value = "A–B repeat unavailable for live or unseekable media"
            return false
        }

        val dur = resolveEffectiveDurationMs()
        if (positionMs < 0L || positionMs >= dur) {
            _abLoopErrorMessage.value = "Point A must be within the media duration (00:00 – ${formatPlaybackDuration(dur)})."
            return false
        }

        val currentId = _currentMedia.value?.id ?: return false
        val existingB = _abPointBMs.value

        _abPointAMs.value = positionMs
        _isAbPointASelected.value = true
        abLoopMediaId = currentId
        _abLoopErrorMessage.value = null

        if (existingB != null && positionMs >= existingB) {
            cancelAbLoopPlayerMessage()
            _abPointBMs.value = null
            _isAbPointBSelected.value = false
            _isAbLoopActive.value = false
            _abLoopStatusMessage.value = "A: ${formatPlaybackDuration(positionMs)} • Waiting to set B"
        } else if (existingB != null && _isAbLoopActive.value) {
            _abLoopStatusMessage.value = "Loop: ${formatPlaybackDuration(positionMs)} – ${formatPlaybackDuration(existingB)}"
            scheduleAbLoopPlayerMessage()
        } else if (existingB != null) {
            _abLoopStatusMessage.value = "A: ${formatPlaybackDuration(positionMs)} • B: ${formatPlaybackDuration(existingB)}"
        } else {
            _abLoopStatusMessage.value = "A: ${formatPlaybackDuration(positionMs)} • Waiting to set B"
        }
        return true
    }

    fun setPointB(positionMs: Long = currentPlaybackPositionMs()): Boolean {
        if (!canUseAbRepeat()) {
            _abLoopErrorMessage.value = "A–B repeat is only available for seekable media with a known finite duration."
            _abLoopStatusMessage.value = "A–B repeat unavailable for live or unseekable media"
            return false
        }

        val currentId = _currentMedia.value?.id ?: return false
        val aPos = _abPointAMs.value
        if (aPos == null || !_isAbPointASelected.value || abLoopMediaId != currentId) {
            _abLoopErrorMessage.value = "Please set Point A before setting Point B."
            _abLoopStatusMessage.value = "Waiting to set A"
            return false
        }

        val dur = resolveEffectiveDurationMs()
        if (positionMs <= aPos) {
            _abLoopErrorMessage.value = "Point B (${formatPlaybackDuration(positionMs)}) must be later than Point A (${formatPlaybackDuration(aPos)})."
            _abLoopStatusMessage.value = "A: ${formatPlaybackDuration(aPos)} • Set B after ${formatPlaybackDuration(aPos)}"
            return false
        }

        if (positionMs > dur) {
            _abLoopErrorMessage.value = "Point B cannot exceed media duration (${formatPlaybackDuration(dur)})."
            return false
        }

        _abPointBMs.value = positionMs
        _isAbPointBSelected.value = true
        _abLoopErrorMessage.value = null
        return activateAbLoop()
    }

    fun activateAbLoop(): Boolean {
        if (!canUseAbRepeat()) {
            _abLoopErrorMessage.value = "A–B repeat is only available for seekable media with a known finite duration."
            _isAbLoopActive.value = false
            return false
        }

        val currentId = _currentMedia.value?.id ?: return false
        val aPos = _abPointAMs.value
        val bPos = _abPointBMs.value
        val dur = resolveEffectiveDurationMs()

        if (aPos == null || bPos == null || abLoopMediaId != currentId || aPos < 0L || bPos <= aPos || bPos > dur) {
            _abLoopErrorMessage.value = "Set valid Point A and Point B (with B after A) to activate loop."
            _isAbLoopActive.value = false
            return false
        }

        _isAbLoopActive.value = true
        _abLoopErrorMessage.value = null
        _abLoopStatusMessage.value = "Loop: ${formatPlaybackDuration(aPos)} – ${formatPlaybackDuration(bPos)}"
        scheduleAbLoopPlayerMessage()

        val currentPos = currentPlaybackPositionMs()
        if (currentPos >= bPos || currentPos < aPos) {
            seekToInternal(aPos)
        }
        return true
    }

    fun clearAbLoop() {
        cancelAbLoopPlayerMessage()
        _abPointAMs.value = null
        _abPointBMs.value = null
        _isAbPointASelected.value = false
        _isAbPointBSelected.value = false
        _isAbLoopActive.value = false
        _abLoopErrorMessage.value = null
        abLoopMediaId = null
        _abLoopStatusMessage.value = if (canUseAbRepeat() || _currentMedia.value == null) {
            "Waiting to set A"
        } else {
            "A–B repeat is unavailable for live or unseekable media"
        }
    }

    private fun scheduleAbLoopPlayerMessage() {
        cancelAbLoopPlayerMessage()
        val aPos = _abPointAMs.value ?: return
        val bPos = _abPointBMs.value ?: return
        val targetMediaId = abLoopMediaId ?: _currentMedia.value?.id ?: return
        if (!_isAbLoopActive.value || bPos <= aPos) return

        try {
            val message = exoPlayer.createMessage { _, _ ->
                onAbLoopBoundaryReached(targetMediaId)
            }
            val mediaItemIndex = exoPlayer.currentMediaItemIndex.coerceAtLeast(0)
            message.setLooper(Looper.getMainLooper())
            message.setPosition(mediaItemIndex, bPos)
            message.setDeleteAfterDelivery(false)
            message.send()
            abLoopPlayerMessage = message
        } catch (e: Exception) {
            // Safe fallback if player is not yet attached or released
        }
    }

    private fun cancelAbLoopPlayerMessage() {
        try {
            abLoopPlayerMessage?.cancel()
        } catch (e: Exception) {
            // ignore
        } finally {
            abLoopPlayerMessage = null
        }
    }

    fun onAbLoopBoundaryReached(expectedMediaId: String? = abLoopMediaId) {
        if (!_isAbLoopActive.value) return
        val currentId = _currentMedia.value?.id ?: return
        if (expectedMediaId == null || currentId != expectedMediaId) return

        val aPos = _abPointAMs.value ?: return
        val bPos = _abPointBMs.value ?: return
        if (bPos <= aPos) return

        val wasPlaying = _isPlaying.value || exoPlayer.isPlaying
        seekToInternal(aPos)
        if (wasPlaying) {
            exoPlayer.play()
            _isPlaying.value = true
        } else {
            exoPlayer.pause()
            _isPlaying.value = false
        }
    }

    private fun seekToInternal(positionMs: Long) {
        val safePos = positionMs.coerceAtLeast(0L)
        exoPlayer.seekTo(safePos)
        _currentPosition.value = safePos
    }

    fun seekTo(positionMs: Long) {
        val dur = resolveEffectiveDurationMs()
        val safePos = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)

        if (_isAbLoopActive.value && _currentMedia.value?.id == abLoopMediaId) {
            val aPos = _abPointAMs.value
            val bPos = _abPointBMs.value
            if (aPos != null && bPos != null && safePos >= bPos) {
                onAbLoopBoundaryReached(abLoopMediaId)
                return
            }
            seekToInternal(safePos)
            scheduleAbLoopPlayerMessage()
        } else {
            seekToInternal(safePos)
        }
    }

    fun seekRelative(deltaMs: Long) {
        val dur = resolveEffectiveDurationMs()
        val basePos = currentPlaybackPositionMs()
        val newPos = if (dur > 0L) (basePos + deltaMs).coerceIn(0L, dur) else (basePos + deltaMs).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun playNext() {
        val current = _currentMedia.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex + 1 < list.size) {
            playMedia(list[currentIndex + 1], list)
        } else if (_repeatMode.value == RepeatMode.ALL && list.isNotEmpty()) {
            playMedia(list[0], list)
        } else {
            pausePlayback()
        }
    }

    fun playPrevious() {
        val current = _currentMedia.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return

        if (currentPlaybackPositionMs() > 3000L) {
            seekTo(0L)
            return
        }

        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playMedia(list[currentIndex - 1], list)
        } else {
            seekTo(0L)
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        exoPlayer.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle
        exoPlayer.shuffleModeEnabled = newShuffle
    }

    /**
     * Starts or replaces the shared Sleep Timer.
     * Valid range: 1 minute to 24 hours (1440 minutes).
     */
    fun setSleepTimer(minutes: Int): Boolean {
        if (minutes < 1 || minutes > 24 * 60) {
            return false
        }
        sleepTimerJob?.cancel()
        val totalSeconds = minutes * 60
        val nowMs = elapsedRealtimeMs()
        sleepTimerDeadlineMs = nowMs + totalSeconds * 1000L
        _sleepTimerSeconds.value = totalSeconds

        sleepTimerJob = scope.launch {
            while (isActive) {
                val remainingMs = sleepTimerDeadlineMs - elapsedRealtimeMs()
                if (remainingMs <= 0L) {
                    break
                }
                val remainingSec = ((remainingMs + 999L) / 1000L).toInt().coerceAtLeast(1)
                _sleepTimerSeconds.value = remainingSec
                val nextTickMs = (remainingMs - (remainingSec - 1) * 1000L).coerceIn(50L, 1000L)
                delay(nextTickMs)
            }
            sleepTimerDeadlineMs = 0L
            _sleepTimerSeconds.value = null
            pausePlayback()
        }
        return true
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerDeadlineMs = 0L
        _sleepTimerSeconds.value = null
    }

    // Equalizer controls
    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        try {
            androidEqualizer?.enabled = enabled
            androidBassBoost?.enabled = enabled
            androidVirtualizer?.enabled = enabled
        } catch (e: Exception) {
            // ignore
        }
    }

    fun setEqualizerBand(index: Int, levelDb: Int) {
        val list = _equalizerBands.value.toMutableList()
        if (index in list.indices) {
            list[index] = levelDb.coerceIn(-12, 12)
            _equalizerBands.value = list
            applyBandsToHardware()
        }
    }

    fun applyPreset(bandsDb: List<Int>) {
        _equalizerBands.value = bandsDb
        applyBandsToHardware()
    }

    fun setBassBoost(level: Int) { // 0 - 1000
        _bassBoostLevel.value = level.coerceIn(0, 1000)
        try {
            androidBassBoost?.setStrength(level.toShort())
        } catch (e: Exception) {
            // ignore
        }
    }

    fun setVirtualizer(level: Int) { // 0 - 1000
        _virtualizerLevel.value = level.coerceIn(0, 1000)
        try {
            androidVirtualizer?.setStrength(level.toShort())
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun applyBandsToHardware() {
        try {
            val eq = androidEqualizer ?: return
            val numBands = eq.numberOfBands.toInt()
            val bands = _equalizerBands.value
            for (i in 0 until minOf(numBands, bands.size)) {
                val mbLevel = (bands[i] * 100).toShort()
                eq.setBandLevel(i.toShort(), mbLevel)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        cancelAbLoopPlayerMessage()
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        scope.cancel()
        try {
            androidEqualizer?.release()
            androidBassBoost?.release()
            androidVirtualizer?.release()
            exoPlayer.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}
