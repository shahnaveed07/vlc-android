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
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.model.formatAbTimestamp
import org.videolan.vlc.model.formatPlaybackDuration
import org.videolan.vlc.player.VlcPlayerController
import org.videolan.vlc.ui.video.PlayerGestureHelper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VideoPlayerGesturesAndAbLoopUpgradeTest {

    private var fakeClockMs: Long = 100_000L
    private lateinit var controller: VlcPlayerController

    private val sampleVideo = MediaItem(
        id = "sample_video_hd",
        title = "Elephant's Dream 4K",
        artist = "Orange Open Source",
        durationMs = 653_000L, // 10m 53s
        uri = "https://media.w3.org/2010/05/bunny/movie.mp4",
        mediaType = MediaType.VIDEO
    )

    private val shortVideo = MediaItem(
        id = "short_clip",
        title = "Short Clip",
        artist = "Tester",
        durationMs = 60_000L, // 1 minute
        uri = "https://media.w3.org/2010/05/bunny/trailer.mp4",
        mediaType = MediaType.VIDEO
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
    // PART A: GESTURE CALCULATIONS & SENSITIVITY TESTS
    // =========================================================================

    @Test
    fun gestureCase1_shortUpwardSwipeOnRightSideChangesVolumeByNoMoreThanExpectedThreshold() {
        val screenWidth = 1080f
        val touchX = 800f // Right side -> volume
        assertEquals(PlayerGestureHelper.GestureControl.VOLUME, PlayerGestureHelper.determineControl(touchX, screenWidth))

        val pxPerStep = 128f // e.g., 64dp at 2x density
        val initialVol = 5
        val maxVol = 15

        // Movement below threshold (e.g. 50px < 128px) produces 0 step change
        val smallSwipeUpwardPx = 50f
        val volAfterSmall = PlayerGestureHelper.calculateTargetVolume(
            initialVolume = initialVol,
            maxVolume = maxVol,
            upwardDragPx = smallSwipeUpwardPx,
            pxPerStep = pxPerStep
        )
        assertEquals("Sub-threshold movement must not change volume", initialVol, volAfterSmall)

        // Movement just crossing 1 step threshold (130px >= 128px) produces exactly 1 step change
        val oneStepSwipeUpwardPx = 130f
        val volAfterOneStep = PlayerGestureHelper.calculateTargetVolume(
            initialVolume = initialVol,
            maxVolume = maxVol,
            upwardDragPx = oneStepSwipeUpwardPx,
            pxPerStep = pxPerStep
        )
        assertEquals("Threshold movement must change volume by exactly 1 step", initialVol + 1, volAfterOneStep)
    }

    @Test
    fun gestureCase2_longerUpwardSwipeIncreasesVolumeProportionally() {
        val pxPerStep = 100f
        val initialVol = 4
        val maxVol = 15

        // Upward swipe of 300px (3 steps)
        val vol3Steps = PlayerGestureHelper.calculateTargetVolume(
            initialVolume = initialVol,
            maxVolume = maxVol,
            upwardDragPx = 300f,
            pxPerStep = pxPerStep
        )
        assertEquals(7, vol3Steps)

        // Upward swipe of 650px (6 steps)
        val vol6Steps = PlayerGestureHelper.calculateTargetVolume(
            initialVolume = initialVol,
            maxVolume = maxVol,
            upwardDragPx = 650f,
            pxPerStep = pxPerStep
        )
        assertEquals(10, vol6Steps)
    }

    @Test
    fun gestureCase3_downwardSwipeDecreasesVolumeProportionally() {
        val pxPerStep = 100f
        val initialVol = 8
        val maxVol = 15

        // Downward swipe of 200px (-200px upward) -> -2 steps
        val volDown2Steps = PlayerGestureHelper.calculateTargetVolume(
            initialVolume = initialVol,
            maxVolume = maxVol,
            upwardDragPx = -200f,
            pxPerStep = pxPerStep
        )
        assertEquals(6, volDown2Steps)

        // Downward swipe of 500px -> -5 steps
        val volDown5Steps = PlayerGestureHelper.calculateTargetVolume(
            initialVolume = initialVol,
            maxVolume = maxVol,
            upwardDragPx = -500f,
            pxPerStep = pxPerStep
        )
        assertEquals(3, volDown5Steps)
    }

    @Test
    fun gestureCase4_reversingDirectionWithinSameGestureProducesCorrectResult() {
        val pxPerStep = 100f
        val initialVol = 5
        val maxVol = 15

        // First swiped up by +300px -> 8
        var cumulativeUpward = 300f
        assertEquals(8, PlayerGestureHelper.calculateTargetVolume(initialVol, maxVol, cumulativeUpward, pxPerStep))

        // Finger reverses and moves back down by 100px (cumulative is now +200px) -> 7
        cumulativeUpward -= 100f
        assertEquals(7, PlayerGestureHelper.calculateTargetVolume(initialVol, maxVol, cumulativeUpward, pxPerStep))

        // Finger reverses further down past initial start by another 300px (cumulative is now -100px) -> 4
        cumulativeUpward -= 300f
        assertEquals(4, PlayerGestureHelper.calculateTargetVolume(initialVol, maxVol, cumulativeUpward, pxPerStep))
    }

    @Test
    fun gestureCase5_volumeNeverExceedsDeviceMaximumOrGoesBelowZero() {
        val pxPerStep = 50f
        val maxVol = 15

        // Massive upward swipe
        val cappedAtMax = PlayerGestureHelper.calculateTargetVolume(
            initialVolume = 10,
            maxVolume = maxVol,
            upwardDragPx = 5000f,
            pxPerStep = pxPerStep
        )
        assertEquals(maxVol, cappedAtMax)

        // Massive downward swipe
        val cappedAtZero = PlayerGestureHelper.calculateTargetVolume(
            initialVolume = 5,
            maxVolume = maxVol,
            upwardDragPx = -5000f,
            pxPerStep = pxPerStep
        )
        assertEquals(0, cappedAtZero)
    }

    @Test
    fun gestureCase6_brightnessChangesSmoothlyOnTheLeftSide() {
        val screenWidth = 1080f
        val touchX = 300f // Left side -> brightness
        assertEquals(PlayerGestureHelper.GestureControl.BRIGHTNESS, PlayerGestureHelper.determineControl(touchX, screenWidth))

        val fullRangePx = 900f
        val initialBrightness = 0.5f

        // Upward swipe of 450px (+50% of 900px range) -> 0.5 + 0.5 = 1.0
        val brightUp = PlayerGestureHelper.calculateTargetBrightness(initialBrightness, 450f, fullRangePx)
        assertEquals(1.0f, brightUp, 0.001f)

        // Downward swipe of 270px (-30% of range) -> 0.5 - 0.3 = 0.2
        val brightDown = PlayerGestureHelper.calculateTargetBrightness(initialBrightness, -270f, fullRangePx)
        assertEquals(0.2f, brightDown, 0.001f)

        // Clamped at 0.01f min and 1.0f max
        val clampedMin = PlayerGestureHelper.calculateTargetBrightness(initialBrightness, -1500f, fullRangePx)
        assertEquals(0.01f, clampedMin, 0.001f)

        val clampedMax = PlayerGestureHelper.calculateTargetBrightness(initialBrightness, 2000f, fullRangePx)
        assertEquals(1.0f, clampedMax, 0.001f)
    }

    @Test
    fun gestureCase7_brightnessIsNotChangedByTouchingRightSide() {
        val screenWidth = 1080f
        val rightTouchX = 600f
        val control = PlayerGestureHelper.determineControl(rightTouchX, screenWidth)
        assertEquals("Right side touch must only map to VOLUME", PlayerGestureHelper.GestureControl.VOLUME, control)
        assertFalse(control == PlayerGestureHelper.GestureControl.BRIGHTNESS)
    }

    @Test
    fun gestureCase8_noSuddenJumpOccursAtGestureStart() {
        val pxPerStep = 64f
        val fullRangePx = 900f
        val initialVol = 7
        val initialBright = 0.65f

        // When gesture starts, cumulative movement is 0
        val volAtStart = PlayerGestureHelper.calculateTargetVolume(initialVol, 15, 0f, pxPerStep)
        val brightAtStart = PlayerGestureHelper.calculateTargetBrightness(initialBright, 0f, fullRangePx)

        assertEquals("Volume at start of gesture must exactly equal initial volume", initialVol, volAtStart)
        assertEquals("Brightness at start of gesture must exactly equal initial brightness", initialBright, brightAtStart, 0.0001f)
    }

    // =========================================================================
    // PART B: A–B SEGMENT REPEAT TESTS
    // =========================================================================

    @Test
    fun abCase1_setAThenSetBAndVerifyLoopStartsCorrectly() {
        controller.playMedia(sampleVideo)
        assertTrue(controller.canUseAbRepeat())

        assertTrue(controller.setPointA(30_000L))
        assertEquals(30_000L, controller.abPointAMs.value)
        assertTrue(controller.isAbPointASelected.value)
        assertFalse("Loop must not be active before Point B is set", controller.isAbLoopActive.value)

        assertTrue(controller.setPointB(60_000L))
        assertEquals(60_000L, controller.abPointBMs.value)
        assertTrue(controller.isAbPointBSelected.value)
        assertTrue("Loop must be active once valid A and B are set", controller.isAbLoopActive.value)
        assertEquals("Loop: 00:30 – 01:00", controller.abLoopStatusMessage.value)
        assertNull(controller.abLoopErrorMessage.value)
    }

    @Test
    fun abCase2_verifyPlaybackReturnsToAWheneverBIsReached() {
        controller.playMedia(sampleVideo)
        controller.seekTo(35_000L)

        assertTrue(controller.setPointA(30_000L))
        assertTrue(controller.setPointB(60_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Reach boundary B
        controller.onPlaybackPositionUpdated(60_000L)
        assertEquals("Position must return to Point A", 30_000L, controller.currentPosition.value)
        assertTrue("Playback must remain active", controller.isPlaying.value)
    }

    @Test
    fun abCase3_verifyAtLeast5To10ConsecutiveLoopCyclesWithoutDuplicateSeeks() {
        controller.playMedia(sampleVideo)
        assertTrue(controller.setPointA(10_000L))
        assertTrue(controller.setPointB(25_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Execute 10 consecutive cycles
        for (cycle in 1..10) {
            // Simulated playback progress through the segment
            controller.onPlaybackPositionUpdated(15_000L)
            assertEquals(15_000L, controller.currentPosition.value)

            controller.onPlaybackPositionUpdated(20_000L)
            assertEquals(20_000L, controller.currentPosition.value)

            // Reaching boundary B
            controller.onPlaybackPositionUpdated(25_000L)
            assertEquals("Cycle $cycle: Must return to A", 10_000L, controller.currentPosition.value)
            assertTrue("Cycle $cycle: Loop must remain active", controller.isAbLoopActive.value)
            assertTrue("Cycle $cycle: Player must continue playing", controller.isPlaying.value)

            // Direct duplicate call at boundary must be rejected by idempotency
            controller.onPlaybackPositionUpdated(25_000L)
            assertEquals("Cycle $cycle: Duplicate boundary call must not seek again", 10_000L, controller.currentPosition.value)
        }
    }

    @Test
    fun abCase4_verifyPlaybackContinuesAfterEveryLoopBoundary() {
        controller.playMedia(sampleVideo)
        assertTrue(controller.isPlaying.value)

        assertTrue(controller.setPointA(10_000L))
        assertTrue(controller.setPointB(20_000L))

        controller.onAbLoopBoundaryReached(sampleVideo.id)
        assertEquals(10_000L, controller.currentPosition.value)
        assertTrue("Playback state must remain playing after boundary seek", controller.isPlaying.value)
    }

    @Test
    fun abCase5_verifyPausedPlaybackRemainsPaused() {
        controller.playMedia(sampleVideo)
        controller.pausePlayback()
        assertFalse(controller.isPlaying.value)

        assertTrue(controller.setPointA(10_000L))
        assertTrue(controller.setPointB(20_000L))

        controller.onAbLoopBoundaryReached(sampleVideo.id)
        assertEquals(10_000L, controller.currentPosition.value)
        assertFalse("Paused player must remain paused after boundary seek", controller.isPlaying.value)
    }

    @Test
    fun abCase6_verifyBAtTheEndOfMediaDoesNotAdvanceToNextTrack() {
        val playlist = listOf(shortVideo, sampleVideo)
        controller.playMedia(shortVideo, playlist)
        assertEquals(shortVideo.id, controller.currentMedia.value?.id)

        // Set B exactly at end of media (60_000L)
        assertTrue(controller.setPointA(50_000L))
        assertTrue(controller.setPointB(60_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Reaching end of media
        controller.onPlaybackPositionUpdated(60_000L)
        assertEquals(50_000L, controller.currentPosition.value)
        assertEquals("Must NOT advance to next track in playlist when A–B loop is active", shortVideo.id, controller.currentMedia.value?.id)
    }

    @Test
    fun abCase7_verifyInvalidAbPositionsAndShortIntervalsAreRejected() {
        controller.playMedia(sampleVideo) // duration 653_000L

        // Point A negative -> rejected
        assertFalse(controller.setPointA(-100L))
        assertNotNull(controller.abLoopErrorMessage.value)

        // Point A >= duration -> rejected
        assertFalse(controller.setPointA(653_000L))
        assertNotNull(controller.abLoopErrorMessage.value)

        // Valid A
        assertTrue(controller.setPointA(20_000L))

        // Point B <= A -> rejected
        assertFalse(controller.setPointB(20_000L))
        assertNotNull(controller.abLoopErrorMessage.value)

        assertFalse(controller.setPointB(15_000L))
        assertNotNull(controller.abLoopErrorMessage.value)

        // Point B interval < 500ms (too short) -> rejected
        assertFalse(controller.setPointB(20_300L))
        assertNotNull(controller.abLoopErrorMessage.value)

        // Point B > duration -> rejected
        assertFalse(controller.setPointB(700_000L))
        assertNotNull(controller.abLoopErrorMessage.value)
    }

    @Test
    fun abCase8_verifyManualSeekingInsideAndOutsideLoopRange() {
        controller.playMedia(sampleVideo)
        assertTrue(controller.setPointA(30_000L))
        assertTrue(controller.setPointB(60_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Seeking inside A–B works normally
        controller.seekTo(45_000L)
        assertEquals(45_000L, controller.currentPosition.value)
        assertTrue(controller.isAbLoopActive.value)

        // Seeking before A (10_000L < 30_000L) wraps back to Point A
        controller.seekTo(10_000L)
        assertEquals(30_000L, controller.currentPosition.value)
        assertTrue(controller.isAbLoopActive.value)

        // Seeking at or past B (70_000L >= 60_000L) wraps back to Point A
        controller.seekTo(70_000L)
        assertEquals(30_000L, controller.currentPosition.value)
        assertTrue(controller.isAbLoopActive.value)
    }

    @Test
    fun abCase9_verifyTemporarilyDisablingAndResumingLoopPreservesMarkers() {
        controller.playMedia(sampleVideo)
        assertTrue(controller.setPointA(15_000L))
        assertTrue(controller.setPointB(35_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Temporarily pause/disable loop
        assertTrue(controller.pauseAbLoop())
        assertFalse(controller.isAbLoopActive.value)
        assertEquals(15_000L, controller.abPointAMs.value)
        assertEquals(35_000L, controller.abPointBMs.value)
        assertTrue(controller.isAbPointASelected.value)
        assertTrue(controller.isAbPointBSelected.value)

        // While loop is paused, normal seeking outside segment is permitted
        controller.seekTo(50_000L)
        assertEquals(50_000L, controller.currentPosition.value)

        // Resume/reactivate loop -> immediately brings playback back into segment
        assertTrue(controller.toggleAbLoop())
        assertTrue(controller.isAbLoopActive.value)
        assertEquals(15_000L, controller.currentPosition.value)
    }

    @Test
    fun abCase10_verifyClearingLoopRemovesEveryMarkerAndScheduledCallback() {
        controller.playMedia(sampleVideo)
        assertTrue(controller.setPointA(10_000L))
        assertTrue(controller.setPointB(25_000L))
        assertTrue(controller.isAbLoopActive.value)

        controller.clearAbLoop()
        assertNull(controller.abPointAMs.value)
        assertNull(controller.abPointBMs.value)
        assertFalse(controller.isAbPointASelected.value)
        assertFalse(controller.isAbPointBSelected.value)
        assertFalse(controller.isAbLoopActive.value)
        assertNull(controller.abLoopErrorMessage.value)
        assertEquals("Waiting to set A", controller.abLoopStatusMessage.value)

        // After clearing, seeking past old B does NOT wrap to A
        controller.seekTo(50_000L)
        assertEquals(50_000L, controller.currentPosition.value)
    }

    @Test
    fun abCase11_verifySwitchingMediaClearsPreviousLoop() {
        controller.playMedia(sampleVideo)
        assertTrue(controller.setPointA(10_000L))
        assertTrue(controller.setPointB(25_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Switch to different media
        controller.playMedia(shortVideo)
        assertNull("Switching media must reset Point A", controller.abPointAMs.value)
        assertNull("Switching media must reset Point B", controller.abPointBMs.value)
        assertFalse(controller.isAbLoopActive.value)
    }

    @Test
    fun abCase12_verifyLiveAndUnseekableMediaCannotActivateAbRepeat() {
        val liveMedia = MediaItem(
            id = "live_stream_feed",
            title = "Live Cam",
            durationMs = 0L,
            uri = "https://example.com/live.m3u8",
            mediaType = MediaType.VIDEO
        )
        controller.playMedia(liveMedia)
        assertFalse(controller.canUseAbRepeat())
        assertFalse(controller.setPointA(5_000L))
        assertFalse(controller.setPointB(15_000L))
        assertFalse(controller.activateAbLoop())
        assertFalse(controller.isAbLoopActive.value)
        assertNotNull(controller.abLoopErrorMessage.value)
    }

    @Test
    fun abCase13_verifyLoopPlaybackAtDifferentPlaybackSpeeds() {
        controller.playMedia(sampleVideo)
        assertTrue(controller.setPointA(10_000L))
        assertTrue(controller.setPointB(20_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Change playback speed to 1.5x
        controller.setSpeed(1.5f)
        assertEquals(1.5f, controller.playbackSpeed.value, 0.01f)
        assertTrue(controller.isAbLoopActive.value)

        // Loop still triggers reliably
        controller.onPlaybackPositionUpdated(20_000L)
        assertEquals(10_000L, controller.currentPosition.value)
        assertTrue(controller.isPlaying.value)

        // Advance playback within segment following return to Point A
        controller.onPlaybackPositionUpdated(15_000L)
        assertEquals(15_000L, controller.currentPosition.value)

        // Change playback speed to 0.75x
        controller.setSpeed(0.75f)
        assertEquals(0.75f, controller.playbackSpeed.value, 0.01f)
        controller.onPlaybackPositionUpdated(20_000L)
        assertEquals(10_000L, controller.currentPosition.value)
    }

    @Test
    fun abCase14_verifyNoInterferenceWithSleepTimerOrNormalRepeatModes() {
        controller.playMedia(sampleVideo)
        assertEquals(RepeatMode.OFF, controller.repeatMode.value)

        assertTrue(controller.setSleepTimer(30))
        assertEquals(1800, controller.sleepTimerSeconds.value)

        assertTrue(controller.setPointA(10_000L))
        assertTrue(controller.setPointB(25_000L))
        assertTrue(controller.isAbLoopActive.value)

        // Repeat mode toggle does not interfere
        controller.toggleRepeatMode()
        assertEquals(RepeatMode.ALL, controller.repeatMode.value)
        assertTrue(controller.isAbLoopActive.value)
        assertEquals(1800, controller.sleepTimerSeconds.value)

        // Fine adjustment works and maintains constraints
        assertTrue(controller.adjustPointA(500L)) // 10_500L
        assertEquals(10_500L, controller.abPointAMs.value)
        assertEquals("00:10.5", formatAbTimestamp(10_500L))

        assertTrue(controller.adjustPointB(-500L)) // 24_500L
        assertEquals(24_500L, controller.abPointBMs.value)
        assertEquals("00:24.5", formatAbTimestamp(24_500L))

        // Jump to Point A
        controller.seekTo(15_000L)
        controller.jumpToPointA()
        assertEquals(10_500L, controller.currentPosition.value)

        // Sleep timer remains intact
        assertEquals(1800, controller.sleepTimerSeconds.value)
    }
}
