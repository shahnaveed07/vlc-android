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
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.RepeatMode

class VlcPlayerController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    var exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
        private set

    private val _currentMedia = MutableStateFlow<MediaItem?>(null)
    val currentMedia: StateFlow<MediaItem?> = _currentMedia.asStateFlow()

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

    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

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

    init {
        setupPlayerListener()
        startProgressUpdater()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                    initAudioEffects(exoPlayer.audioSessionId)
                } else if (playbackState == Player.STATE_ENDED) {
                    playNext()
                }
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
            while (true) {
                if (exoPlayer.isPlaying) {
                    _currentPosition.value = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration
                    if (dur > 0) _duration.value = dur
                }
                delay(500)
            }
        }
    }

    fun playMedia(item: MediaItem, queue: List<MediaItem>? = null) {
        _currentMedia.value = item
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
    }

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        _playlist.value = items
        val target = items.getOrElse(startIndex) { items[0] }
        playMedia(target, items)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _currentPosition.value = positionMs
    }

    fun seekRelative(deltaMs: Long) {
        val newPos = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
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
            exoPlayer.pause()
        }
    }

    fun playPrevious() {
        val current = _currentMedia.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return

        if (exoPlayer.currentPosition > 3000L) {
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

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSeconds.value = null
            return
        }
        _sleepTimerSeconds.value = minutes * 60
        sleepTimerJob = scope.launch {
            var remaining = minutes * 60
            while (remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerSeconds.value = remaining
            }
            _sleepTimerSeconds.value = null
            exoPlayer.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
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
                // Android Equalizer level is in millibels (dB * 100)
                val mbLevel = (bands[i] * 100).toShort()
                eq.setBandLevel(i.toShort(), mbLevel)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
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
