/*
 * Copyright (C) 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * This file is part of PLayMuse, a fork of VideoLAN VLC for Android.
 * Original copyright: Copyright (C) 1996-2026 VideoLAN and VLC Authors.
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.model.SleepTimerValidationResult
import org.videolan.vlc.model.formatPlaybackDuration
import org.videolan.vlc.model.formatSleepTimerCountdown
import org.videolan.vlc.model.validateCustomSleepTimerInput
import org.videolan.vlc.player.VlcPlayerController

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaybackControlsAndLoopTest {

    private var fakeClockMs: Long = 100_000L
    private lateinit var controller: VlcPlayerController

    private val sampleVideo = MediaItem(
        id = "test_video_1",
        title = "Sample Feature Film",
        artist = "Blender Foundation",
        durationMs = 5_400_000L, // 1h 30m
        uri = "https://media.w3.org/2010/05/sintel/trailer.mp4",
        mediaType = MediaType.VIDEO
    )

    private val sampleAudio = MediaItem(
        id = "test_audio_1",
        title = "Midnight Jazz & Chill",
        artist = "PLayMuse Sound Lab",
        durationMs = 214_000L, // 3m 34s
        uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        mediaType = MediaType.AUDIO
    )

    @Before
    fun setUp() {
        fakeClockMs = 100_000L
        val context = RuntimeEnvironment.getApplication()
        controller = VlcPlayerController(
            context = context,
            elapsedRealtimeMs = { fakeClockMs }
        )
    }

    @After
    fun tearDown() {
        controller.release()
    }

    // =========================================================================
    // 1. Duration & Countdown Formatting Tests
    // =========================================================================

    @Test
    fun verifyDurationFormattingForShort59MinOneHourAndMultiHourMedia() {
        assertEquals("00:00", formatPlaybackDuration(0L))
        assertEquals("--:--", formatPlaybackDuration(0L, allowZero = false))
        assertEquals("--:--", formatPlaybackDuration(-1L))
        assertEquals("00:45", formatPlaybackDuration(45_000L))
        assertEquals("09:59", formatPlaybackDuration(599_000L))
        assertEquals("59:59", formatPlaybackDuration(3_599_000L))
        assertEquals("1:00:00", formatPlaybackDuration(3_600_000L))
        assertEquals("1:30:00", formatPlaybackDuration(5_400_000L))
        assertEquals("12:05:40", formatPlaybackDuration(43_540_000L))
    }

    @Test
    fun verifySleepTimerCountdownFormattingNeverDisplaysZeroMinutesWhileSecondsRemain() {
        assertEquals("15:42", formatSleepTimerCountdown(15 * 60 + 42))
        assertEquals("01:05:30", formatSleepTimerCountdown(3600 + 5 * 60 + 30))
        assertEquals("00:45", formatSleepTimerCountdown(45))
        assertEquals("00:01", formatSleepTimerCountdown(1))
    }

    // =========================================================================
    // 2. Sleep Timer Regression Tests
    // =========================================================================

    @Test
    fun verifySleepTimerPresetsAndCustomValidation() {
        val presets = listOf(15, 30, 45, 60, 90)
        for (presetMinutes in presets) {
            assertTrue("Preset $presetMinutes min should succeed", controller.setSleepTimer(presetMinutes))
            assertEquals(presetMinutes * 60, controller.sleepTimerSeconds.value)
            assertTrue("Single timer job must be active", controller.isSleepTimerJobActive)
        }

        // Custom timer validation
        val valid1Min = validateCustomSleepTimerInput("0", "1")
        assertTrue(valid1Min is SleepTimerValidationResult.Valid)
        assertEquals(1, (valid1Min as SleepTimerValidationResult.Valid).totalMinutes)

        val valid24Hours = validateCustomSleepTimerInput("24", "0")
        assertTrue(valid24Hours is SleepTimerValidationResult.Valid)
        assertEquals(1440, (valid24Hours as SleepTimerValidationResult.Valid).totalMinutes)

        val validMixed = validateCustomSleepTimerInput("1", "35")
        assertTrue(validMixed is SleepTimerValidationResult.Valid)
        assertEquals(95, (validMixed as SleepTimerValidationResult.Valid).totalMinutes)

        // Reject zero, negative, malformed, or > 24h
        assertTrue(validateCustomSleepTimerInput("0", "0") is SleepTimerValidationResult.Invalid)
        assertTrue(validateCustomSleepTimerInput("-1", "10") is SleepTimerValidationResult.Invalid)
        assertTrue(validateCustomSleepTimerInput("abc", "15") is SleepTimerValidationResult.Invalid)
        assertTrue(validateCustomSleepTimerInput("24", "1") is SleepTimerValidationResult.Invalid)
        assertTrue(validateCustomSleepTimerInput("0", "1441") is SleepTimerValidationResult.Invalid)

        assertFalse(controller.setSleepTimer(0))
        assertFalse(controller.setSleepTimer(-10))
        assertFalse(controller.setSleepTimer(1441))
    }

    @Test
    fun verifySleepTimerReplaceCancelAndExpiryPausesPlayback() {
        controller.playMedia(sampleAudio)
        assertTrue(controller.isPlaying.value)

        // Start 30-minute timer
        assertTrue(controller.setSleepTimer(30))
        assertEquals(1800, controller.sleepTimerSeconds.value)

        // Replace with 15-minute timer
        assertTrue(controller.setSleepTimer(15))
        assertEquals(900, controller.sleepTimerSeconds.value)
        assertTrue(controller.isSleepTimerJobActive)

        // Cancel active timer
        controller.cancelSleepTimer()
        assertNull(controller.sleepTimerSeconds.value)
        assertFalse(controller.isSleepTimerJobActive)

        // Start 1-minute timer and advance monotonic clock past deadline
        assertTrue(controller.setSleepTimer(1))
        assertEquals(60, controller.sleepTimerSeconds.value)

        fakeClockMs += 61_000L
        Thread.sleep(120)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertNull("Timer state must reset after expiration", controller.sleepTimerSeconds.value)
        assertFalse("Timer job must finish after expiration", controller.isSleepTimerJobActive)
        assertFalse("Playback must be paused when sleep timer expires", controller.isPlaying.value)
        assertEquals("Current media must be preserved after sleep timer pauses playback", sampleAudio.id, controller.currentMedia.value?.id)
    }

    @Test
    fun verifyAudioAndVideoShareSameSleepTimerStateAcrossMediaChanges() {
        controller.playMedia(sampleAudio)
        assertTrue(controller.setSleepTimer(45))
        assertEquals(2700, controller.sleepTimerSeconds.value)

        // Switch to video playback — active sleep timer must remain active and shared
        controller.playMedia(sampleVideo)
        assertEquals(2700, controller.sleepTimerSeconds.value)
        assertTrue(controller.isSleepTimerJobActive)
    }

    // =========================================================================
    // 3. A–B Segment Loop Regression Tests
    // =========================================================================

    @Test
    fun verifyAbLoopSetASetBAndRejectInvalidSegments() {
        controller.playMedia(sampleVideo)

        // Setting B before A is rejected
        assertFalse(controller.setPointB(30_000L))
        assertNotNull(controller.abLoopErrorMessage.value)
        assertFalse(controller.isAbLoopActive.value)

        // Set A at 00:45
        assertTrue(controller.setPointA(45_000L))
        assertEquals(45_000L, controller.abPointAMs.value)
        assertTrue(controller.isAbPointASelected.value)
        assertFalse(controller.isAbLoopActive.value)

        // Reject B earlier than A (00:30 < 00:45)
        assertFalse(controller.setPointB(30_000L))
        assertNotNull(controller.abLoopErrorMessage.value)
        assertNull(controller.abPointBMs.value)
        assertFalse(controller.isAbLoopActive.value)

        // Reject B equal to A (00:45 == 00:45)
        assertFalse(controller.setPointB(45_000L))
        assertNotNull(controller.abLoopErrorMessage.value)
        assertNull(controller.abPointBMs.value)
        assertFalse(controller.isAbLoopActive.value)

        // Set valid B at 01:20
        assertTrue(controller.setPointB(80_000L))
        assertEquals(80_000L, controller.abPointBMs.value)
        assertTrue(controller.isAbPointBSelected.value)
        assertTrue(controller.isAbLoopActive.value)
        assertNull(controller.abLoopErrorMessage.value)
        assertEquals("Loop: 00:45 – 01:20", controller.abLoopStatusMessage.value)
    }

    @Test
    fun verifyAbLoopReturnsToAOnReachingBAcrossRepeatedCycles() {
        controller.playMedia(sampleVideo)
        controller.seekTo(50_000L)

        assertTrue(controller.setPointA(45_000L))
        assertTrue(controller.setPointB(80_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Cycle 1: playback reaches B (80_000L) -> jumps back to A (45_000L)
        controller.onPlaybackPositionUpdated(80_000L)
        assertEquals(45_000L, controller.currentPosition.value)
        assertTrue(controller.isPlaying.value)
        assertTrue(controller.isAbLoopActive.value)

        // Cycle 2: playback progresses and reaches B again -> jumps back to A
        controller.onPlaybackPositionUpdated(60_000L)
        assertEquals(60_000L, controller.currentPosition.value)
        controller.onPlaybackPositionUpdated(80_500L)
        assertEquals(45_000L, controller.currentPosition.value)
        assertTrue(controller.isPlaying.value)
        assertTrue(controller.isAbLoopActive.value)

        // Cycle 3: direct PlayerMessage boundary callback also returns to A
        controller.seekTo(75_000L)
        controller.onAbLoopBoundaryReached(sampleVideo.id)
        assertEquals(45_000L, controller.currentPosition.value)
    }

    @Test
    fun verifyAbLoopClearMediaChangeAndRetryBehavior() {
        controller.playMedia(sampleVideo)
        assertTrue(controller.setPointA(10_000L))
        assertTrue(controller.setPointB(25_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Retry of the same media item preserves active A–B loop
        controller.retry()
        assertEquals(10_000L, controller.abPointAMs.value)
        assertEquals(25_000L, controller.abPointBMs.value)
        assertTrue(controller.isAbLoopActive.value)

        // Switching to a different media item clears old markers
        controller.playMedia(sampleAudio)
        assertNull(controller.abPointAMs.value)
        assertNull(controller.abPointBMs.value)
        assertFalse(controller.isAbPointASelected.value)
        assertFalse(controller.isAbPointBSelected.value)
        assertFalse(controller.isAbLoopActive.value)

        // Set new loop on audio and clear explicitly
        assertTrue(controller.setPointA(5_000L))
        assertTrue(controller.setPointB(15_000L))
        assertTrue(controller.isAbLoopActive.value)
        controller.clearAbLoop()
        assertNull(controller.abPointAMs.value)
        assertNull(controller.abPointBMs.value)
        assertFalse(controller.isAbLoopActive.value)
    }

    @Test
    fun verifyAbLoopDoesNotInterfereWithTrackRepeatModesAndSupportsManualSeekAndPauseResume() {
        controller.playMedia(sampleAudio)
        assertEquals(RepeatMode.OFF, controller.repeatMode.value)

        assertTrue(controller.setPointA(20_000L))
        assertTrue(controller.setPointB(40_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Changing track repeat mode (OFF -> ALL -> ONE) does not disable A–B loop
        controller.toggleRepeatMode()
        assertEquals(RepeatMode.ALL, controller.repeatMode.value)
        assertTrue(controller.isAbLoopActive.value)

        controller.toggleRepeatMode()
        assertEquals(RepeatMode.ONE, controller.repeatMode.value)
        assertTrue(controller.isAbLoopActive.value)

        // Manual seek inside loop keeps loop active
        controller.seekTo(30_000L)
        assertEquals(30_000L, controller.currentPosition.value)
        assertTrue(controller.isAbLoopActive.value)

        // Manual seek past B immediately wraps back to A without breaking loop
        controller.seekTo(50_000L)
        assertEquals(20_000L, controller.currentPosition.value)
        assertTrue(controller.isAbLoopActive.value)

        // Pause state is preserved if boundary is reached while paused
        controller.pausePlayback()
        assertFalse(controller.isPlaying.value)
        controller.onAbLoopBoundaryReached(sampleAudio.id)
        assertEquals(20_000L, controller.currentPosition.value)
        assertFalse(controller.isPlaying.value)

        // Resume playback continues normally
        controller.resumePlayback()
        assertTrue(controller.isPlaying.value)
    }

    @Test
    fun verifyUnseekableOrUnknownDurationMediaCannotEnableAbRepeat() {
        val unseekableLiveItem = MediaItem(
            id = "live_unseekable",
            title = "Live Broadcast",
            durationMs = 0L,
            uri = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            mediaType = MediaType.VIDEO
        )
        controller.playMedia(unseekableLiveItem)
        assertFalse(controller.canUseAbRepeat())
        assertFalse(controller.setPointA(10_000L))
        assertNotNull(controller.abLoopErrorMessage.value)
        assertFalse(controller.isAbLoopActive.value)

        // Also verify explicit unseekable flag disables active loop
        controller.playMedia(sampleVideo)
        assertTrue(controller.setPointA(10_000L))
        assertTrue(controller.setPointB(20_000L))
        assertTrue(controller.isAbLoopActive.value)

        controller.setSeekableStateForMedia(seekable = false, durationMs = 0L)
        assertFalse(controller.isAbLoopActive.value)
        assertFalse(controller.canUseAbRepeat())
    }
}
