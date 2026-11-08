/*
 * Copyright (C) 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * This file is part of PLayMuse, a fork of VideoLAN VLC for Android.
 * Original copyright: Copyright (C) 1996-2026 VideoLAN and VLC Authors.
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.ui.video

import android.app.Activity
import android.provider.Settings

/**
 * Encapsulates calculation and lifecycle state for video player swipe gestures
 * (volume on right half, brightness on left half) with cumulative distance tracking.
 */
object PlayerGestureHelper {
    const val VOLUME_STEP_DP = 64f
    const val BRIGHTNESS_FULL_RANGE_DP = 900f
    const val MIN_BRIGHTNESS = 0.01f
    const val MAX_BRIGHTNESS = 1.0f

    enum class GestureControl {
        BRIGHTNESS,
        VOLUME
    }

    /**
     * Determines whether the initial touch position controls brightness (left half)
     * or volume (right half).
     */
    fun determineControl(startX: Float, totalWidth: Float): GestureControl {
        return if (startX < totalWidth / 2f) GestureControl.BRIGHTNESS else GestureControl.VOLUME
    }

    /**
     * Calculates the target volume using cumulative vertical movement from gesture start.
     * Moving upward (positive [upwardDragPx]) increases volume.
     * Moving downward (negative [upwardDragPx]) decreases volume.
     * Steps are scaled by [pxPerStep] (approx 64dp).
     * Result is safely clamped between 0 and [maxVolume].
     */
    fun calculateTargetVolume(
        initialVolume: Int,
        maxVolume: Int,
        upwardDragPx: Float,
        pxPerStep: Float
    ): Int {
        if (pxPerStep <= 0f || maxVolume <= 0) return initialVolume.coerceIn(0, maxVolume.coerceAtLeast(0))
        val steps = (upwardDragPx / pxPerStep).toInt()
        return (initialVolume + steps).coerceIn(0, maxVolume)
    }

    /**
     * Calculates target window brightness using cumulative vertical movement from gesture start.
     * Moving upward (positive [upwardDragPx]) increases brightness.
     * Moving downward (negative [upwardDragPx]) decreases brightness.
     * Result is safely clamped between 0.01 and 1.0.
     */
    fun calculateTargetBrightness(
        initialBrightness: Float,
        upwardDragPx: Float,
        fullRangePx: Float
    ): Float {
        if (fullRangePx <= 0f) return initialBrightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        val delta = upwardDragPx / fullRangePx
        return (initialBrightness + delta).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }

    /**
     * Resolves the initial brightness for the Activity window.
     * If the window is using system default (< 0), reads current system brightness
     * without requesting WRITE_SETTINGS permission, falling back to 0.5f on failure.
     */
    fun resolveInitialBrightness(activity: Activity?): Float {
        if (activity == null) return 0.5f
        val lp = activity.window.attributes
        return if (lp.screenBrightness in MIN_BRIGHTNESS..MAX_BRIGHTNESS) {
            lp.screenBrightness
        } else {
            try {
                val sysBright = Settings.System.getInt(
                    activity.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                )
                (sysBright / 255f).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
            } catch (e: Exception) {
                0.5f
            }
        }
    }
}
