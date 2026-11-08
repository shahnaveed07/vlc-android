/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.model

import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import java.util.Locale

enum class MediaType {
    VIDEO,
    AUDIO
}

/**
 * Validates that [uriString] is a genuine device-local Android MediaStore content URI
 * (`content://media/.../video/media/{id}` or `content://media/.../audio/media/{id}`)
 * with a positive numeric item ID.
 *
 * Rejects remote URLs (`http`, `https`, `rtsp`), file URIs, third-party authorities,
 * table-level URIs without an item ID, and mismatched media types.
 */
fun parseValidMediaStoreUri(uriString: String, expectedType: MediaType? = null): Uri? {
    if (uriString.isBlank()) return null
    val uri = try {
        Uri.parse(uriString)
    } catch (_: Exception) {
        null
    } ?: return null

    if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
    if (uri.authority != MediaStore.AUTHORITY) return null

    val path = uri.path ?: return null
    val matchesType = when (expectedType) {
        MediaType.VIDEO -> path.contains("/video/media/")
        MediaType.AUDIO -> path.contains("/audio/media/")
        null -> path.contains("/video/media/") || path.contains("/audio/media/")
    }
    if (!matchesType) return null

    val itemId = try {
        ContentUris.parseId(uri)
    } catch (_: Exception) {
        -1L
    }
    if (itemId <= 0L) return null

    return uri
}

/**
 * Returns true only if [item] represents a real device-local MediaStore video or audio entry
 * eligible for permanent device deletion. Built-in remote sample items return false.
 */
fun isEligibleLocalMediaStoreItem(item: MediaItem): Boolean {
    return parseValidMediaStoreUri(item.uri, item.mediaType) != null
}

/**
 * Typed outcomes for device media deletion across Android 8.0 (API 26) through Android 16 (API 36).
 */
sealed class MediaDeleteResult {
    data class Deleted(val mediaItem: MediaItem) : MediaDeleteResult()
    data class RequiresSystemConsent(
        val mediaItem: MediaItem,
        val intentSender: IntentSender,
        val requiresFollowUpDelete: Boolean
    ) : MediaDeleteResult()
    data class RequiresLegacyWritePermission(val mediaItem: MediaItem) : MediaDeleteResult()
    data class Cancelled(val mediaItem: MediaItem) : MediaDeleteResult()
    data class PermissionDenied(val mediaItem: MediaItem, val message: String) : MediaDeleteResult()
    data class NotFoundOrIneligible(val mediaItem: MediaItem, val message: String) : MediaDeleteResult()
    data class Error(val mediaItem: MediaItem, val message: String) : MediaDeleteResult()
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit to screen"),
    FILL("Fill screen"),
    ZOOM("100% Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

/**
 * Shared hour-aware playback duration formatter used across audio, video, and media items.
 * Examples:
 * - 45_000L -> "00:45"
 * - 599_000L -> "09:59"
 * - 3_599_000L -> "59:59"
 * - 3_600_000L -> "1:00:00"
 * - 5_400_000L -> "1:30:00"
 * - 43_540_000L -> "12:05:40"
 */
fun formatPlaybackDuration(durationMs: Long, allowZero: Boolean = true): String {
    if (durationMs < 0L || (!allowZero && durationMs == 0L)) return "--:--"
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Formats duration with sub-second tenths precision for A–B loop marker fine-tuning.
 * Examples:
 * - 45_000L -> "00:45.0"
 * - 45_100L -> "00:45.1"
 * - 5_400_000L -> "1:30:00.0"
 */
fun formatAbTimestamp(durationMs: Long): String {
    if (durationMs < 0L) return "--:--"
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val millis = durationMs % 1000L
    val tenths = (millis / 100L).coerceIn(0L, 9L)
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d.%d", hours, minutes, seconds, tenths)
    } else {
        String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
    }
}

/**
 * Formats remaining sleep timer seconds accurately so sub-minute values never display as "0m".
 * Examples:
 * - 942 -> "15:42"
 * - 3930 -> "01:05:30"
 */
fun formatSleepTimerCountdown(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

sealed class SleepTimerValidationResult {
    data class Valid(val totalMinutes: Int) : SleepTimerValidationResult()
    data class Invalid(val message: String) : SleepTimerValidationResult()
}

/**
 * Validates custom sleep timer hours and minutes input.
 * Minimum: 1 minute. Maximum: 24 hours (1440 minutes).
 */
fun validateCustomSleepTimerInput(hoursInput: String, minutesInput: String): SleepTimerValidationResult {
    val trimmedHours = hoursInput.trim()
    val trimmedMinutes = minutesInput.trim()

    if (trimmedHours.isEmpty() && trimmedMinutes.isEmpty()) {
        return SleepTimerValidationResult.Invalid("Enter hours or minutes (1 min to 24 hours).")
    }

    val hours = if (trimmedHours.isEmpty()) 0 else trimmedHours.toIntOrNull()
    val minutes = if (trimmedMinutes.isEmpty()) 0 else trimmedMinutes.toIntOrNull()

    if (hours == null || minutes == null) {
        return SleepTimerValidationResult.Invalid("Please enter valid whole numbers.")
    }

    if (hours < 0 || minutes < 0) {
        return SleepTimerValidationResult.Invalid("Duration cannot be negative.")
    }

    if (minutes >= 60 && trimmedHours.isNotEmpty() && hours > 0) {
        return SleepTimerValidationResult.Invalid("Minutes must be between 0 and 59 when hours are specified.")
    }

    val totalMinutes = hours * 60 + minutes
    if (totalMinutes < 1) {
        return SleepTimerValidationResult.Invalid("Minimum sleep timer duration is 1 minute.")
    }

    if (totalMinutes > 24 * 60) {
        return SleepTimerValidationResult.Invalid("Maximum sleep timer duration is 24 hours.")
    }

    return SleepTimerValidationResult.Valid(totalMinutes)
}

data class MediaItem(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val uri: String,
    val mediaType: MediaType = MediaType.VIDEO,
    val thumbnailUri: String? = null,
    val sizeBytes: Long = 0L,
    val resolution: String = "HD",
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0L
) {
    val durationFormatted: String
        get() = formatPlaybackDuration(durationMs, allowZero = false)

    val sizeFormatted: String
        get() {
            if (sizeBytes <= 0) return ""
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }
}

data class EqualizerPreset(
    val name: String,
    val bandsDb: List<Int> // dB offset for 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz (-12 to +12)
)

data class VlcSettings(
    val themeMode: String = "Dark", // "Dark", "Light", "System"
    val hardwareAcceleration: Boolean = true,
    val backgroundPlayback: Boolean = true,
    val resumePlayback: Boolean = true,
    val audioBoost: Boolean = false,
    val gesturesEnabled: Boolean = true,
    val subtitleSize: String = "Normal" // "Small", "Normal", "Large"
)
