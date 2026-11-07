/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.model.AspectRatioMode
import org.videolan.vlc.model.MediaItem
import org.videolan.vlc.model.MediaType
import org.videolan.vlc.model.RepeatMode
import org.videolan.vlc.model.SleepTimerValidationResult
import org.videolan.vlc.model.formatPlaybackDuration
import org.videolan.vlc.model.formatSleepTimerCountdown
import org.videolan.vlc.model.validateCustomSleepTimerInput

@Composable
fun SpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSpeedSelected(speed)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal,
                                color = if (speed == currentSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (speed == currentSpeed) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SleepTimerDialog(
    currentRemainingSec: Int?,
    onSetMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        15 to "15 minutes",
        30 to "30 minutes",
        45 to "45 minutes",
        60 to "1 hour",
        90 to "1.5 hours"
    )

    var customHours by remember { mutableStateOf("") }
    var customMinutes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Playback will pause automatically when the timer expires.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (currentRemainingSec != null && currentRemainingSec > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sleep_timer_active_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Active countdown",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = formatSleepTimerCountdown(currentRemainingSec),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.testTag("sleep_timer_countdown_text")
                            )
                        }
                    }
                }

                // Preset options
                options.forEach { (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                validationError = null
                                onSetMinutes(minutes)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("sleep_preset_$minutes"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Custom duration input
                Text(
                    text = "Custom Duration (1 min – 24 hours)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customHours,
                        onValueChange = {
                            customHours = it
                            validationError = null
                        },
                        label = { Text("Hours") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sleep_custom_hours_input")
                    )

                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = {
                            customMinutes = it
                            validationError = null
                        },
                        label = { Text("Minutes") },
                        placeholder = { Text("25") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sleep_custom_minutes_input")
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.testTag("sleep_custom_error_text")
                    )
                }

                Button(
                    onClick = {
                        when (val result = validateCustomSleepTimerInput(customHours, customMinutes)) {
                            is SleepTimerValidationResult.Valid -> {
                                validationError = null
                                onSetMinutes(result.totalMinutes)
                                onDismiss()
                            }
                            is SleepTimerValidationResult.Invalid -> {
                                validationError = result.message
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sleep_set_custom_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Start Custom Timer",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (currentRemainingSec != null && currentRemainingSec > 0) {
                    OutlinedButton(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sleep_cancel_active_button"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Cancel Active Timer", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun AbSegmentSeekBarIndicators(
    pointAMs: Long?,
    pointBMs: Long?,
    durationMs: Long,
    isLoopActive: Boolean,
    onClearLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pointAMs == null && pointBMs == null) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Proportional visual marker bar showing A and B along the timeline
        if (durationMs > 0L) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val widthDp = maxWidth
                val aFrac = ((pointAMs ?: 0L).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val bFrac = pointBMs?.let { (it.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) }

                if (bFrac != null && bFrac > aFrac) {
                    Box(
                        modifier = Modifier
                            .offset(x = widthDp * aFrac)
                            .width(widthDp * (bFrac - aFrac))
                            .height(6.dp)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isLoopActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            )
                    )
                }

                if (pointAMs != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .offset(x = (widthDp * aFrac - 8.dp).coerceIn(0.dp, (widthDp - 16.dp).coerceAtLeast(0.dp)))
                            .align(Alignment.CenterStart)
                            .testTag("seekbar_marker_a")
                    ) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (pointBMs != null && bFrac != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .offset(x = (widthDp * bFrac - 8.dp).coerceIn(0.dp, (widthDp - 16.dp).coerceAtLeast(0.dp)))
                            .align(Alignment.CenterStart)
                            .testTag("seekbar_marker_b")
                    ) {
                        Text(
                            text = "B",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // Compact status badge row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Text(
                    text = when {
                        isLoopActive && pointAMs != null && pointBMs != null ->
                            "Loop: ${formatPlaybackDuration(pointAMs)} – ${formatPlaybackDuration(pointBMs)}"
                        pointAMs != null && pointBMs != null ->
                            "A: ${formatPlaybackDuration(pointAMs)} • B: ${formatPlaybackDuration(pointBMs)}"
                        pointAMs != null ->
                            "A: ${formatPlaybackDuration(pointAMs)} • Waiting to set B"
                        else -> "A–B Repeat"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("ab_loop_active_status_badge")
                )
            }

            TextButton(
                onClick = onClearLoop,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text(
                    text = "Clear Loop",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackOptionsSheet(
    isVideoMode: Boolean,
    playbackSpeed: Float,
    sleepTimerSeconds: Int?,
    pointAMs: Long?,
    pointBMs: Long?,
    isAbLoopActive: Boolean,
    abLoopStatusMessage: String,
    abLoopErrorMessage: String?,
    isMediaSeekable: Boolean,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onActivateAbLoop: () -> Unit,
    onClearAbLoop: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    // Video-specific callbacks
    aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    onCycleAspectRatio: () -> Unit = {},
    onLockControls: () -> Unit = {},
    // Audio-specific callbacks
    isShuffle: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeatMode: () -> Unit = {},
    isPlaying: Boolean = false,
    onPreviousTrack: () -> Unit = {},
    onTogglePlayPause: () -> Unit = {},
    onNextTrack: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isVideoMode) "Video Playback Options" else "Audio Playback Options",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Options")
                }
            }

            // A–B Segment Repeat Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isAbLoopActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "A–B Segment Repeat",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = if (isAbLoopActive) "ACTIVE" else "INACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isAbLoopActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            modifier = Modifier.testTag("ab_loop_state_label")
                        )
                    }

                    Text(
                        text = abLoopStatusMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isAbLoopActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.testTag("ab_loop_status_message")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "A: ${pointAMs?.let { formatPlaybackDuration(it) } ?: "--:--"}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (pointAMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.testTag("ab_point_a_timestamp")
                        )
                        Text(
                            text = "B: ${pointBMs?.let { formatPlaybackDuration(it) } ?: "--:--"}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (pointBMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.testTag("ab_point_b_timestamp")
                        )
                    }

                    if (abLoopErrorMessage != null) {
                        Text(
                            text = abLoopErrorMessage,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.testTag("ab_loop_error_text")
                        )
                    }

                    if (!isMediaSeekable) {
                        Text(
                            text = "A–B repeat is disabled because the current media is live, unseekable, or has an unknown duration.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSetPointA,
                            enabled = isMediaSeekable,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ab_set_a_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Set A", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onSetPointB,
                            enabled = isMediaSeekable && pointAMs != null,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ab_set_b_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Set B", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (pointAMs != null && pointBMs != null && !isAbLoopActive) {
                            Button(
                                onClick = onActivateAbLoop,
                                enabled = isMediaSeekable,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ab_activate_loop_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("Activate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onClearAbLoop,
                            enabled = pointAMs != null || pointBMs != null || isAbLoopActive,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ab_clear_loop_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Clear Loop", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Common Quick Action Rows: Speed, Sleep Timer, Equalizer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column {
                    OptionActionRow(
                        icon = Icons.Default.Speed,
                        title = "Playback Speed",
                        value = "${playbackSpeed}x",
                        testTag = "option_row_speed",
                        onClick = {
                            onDismiss()
                            onOpenSpeed()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    OptionActionRow(
                        icon = Icons.Default.Timer,
                        title = "Sleep Timer",
                        value = if (sleepTimerSeconds != null && sleepTimerSeconds > 0) {
                            formatSleepTimerCountdown(sleepTimerSeconds)
                        } else {
                            "Off"
                        },
                        testTag = "option_row_sleep_timer",
                        onClick = {
                            onDismiss()
                            onOpenSleepTimer()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    OptionActionRow(
                        icon = Icons.Default.GraphicEq,
                        title = "Audio Equalizer",
                        value = "Open",
                        testTag = "option_row_equalizer",
                        onClick = {
                            onDismiss()
                            onOpenEqualizer()
                        }
                    )

                    if (isVideoMode) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OptionActionRow(
                            icon = Icons.Default.AspectRatio,
                            title = "Aspect Ratio",
                            value = aspectRatioMode.label,
                            testTag = "option_row_aspect_ratio",
                            onClick = onCycleAspectRatio
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OptionActionRow(
                            icon = Icons.Default.LockOpen,
                            title = "Lock Screen Controls",
                            value = "Lock",
                            testTag = "option_row_lock_controls",
                            onClick = {
                                onDismiss()
                                onLockControls()
                            }
                        )
                    } else {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OptionActionRow(
                            icon = Icons.Default.Shuffle,
                            title = "Shuffle Queue",
                            value = if (isShuffle) "On" else "Off",
                            testTag = "option_row_shuffle",
                            onClick = onToggleShuffle
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OptionActionRow(
                            icon = when (repeatMode) {
                                RepeatMode.OFF -> Icons.Default.Repeat
                                RepeatMode.ALL -> Icons.Default.Repeat
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                            },
                            title = "Track Repeat Mode",
                            value = when (repeatMode) {
                                RepeatMode.OFF -> "Off"
                                RepeatMode.ALL -> "Repeat All"
                                RepeatMode.ONE -> "Repeat One"
                            },
                            testTag = "option_row_repeat_mode",
                            onClick = onToggleRepeatMode
                        )
                    }
                }
            }

            if (!isVideoMode) {
                // Audio Queue Transport Controls
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Queue Transport",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onPreviousTrack) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track")
                            }
                            IconButton(onClick = onTogglePlayPause) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onNextTrack) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next Track")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OptionActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun MediaInfoDialog(
    mediaItem: MediaItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Media Information", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow("Title", mediaItem.title)
                InfoRow("Artist", mediaItem.artist)
                if (mediaItem.album != "Unknown Album") {
                    InfoRow("Album", mediaItem.album)
                }
                InfoRow("Duration", mediaItem.durationFormatted)
                if (mediaItem.sizeFormatted.isNotEmpty()) {
                    InfoRow("File Size", mediaItem.sizeFormatted)
                }
                InfoRow("Resolution / Format", mediaItem.resolution)
                InfoRow("Location / URL", mediaItem.uri)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("OK", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
fun DeleteMediaConfirmationDialog(
    mediaItem: MediaItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isVideo = mediaItem.mediaType == MediaType.VIDEO
    val dialogTitle = if (isVideo) "Delete Video?" else "Delete Audio?"
    val dialogMessage = "Are you sure you want to permanently delete \"${mediaItem.title}\" from your device?"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = dialogTitle,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("delete_confirm_dialog_title")
            )
        },
        text = {
            Text(
                text = dialogMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("delete_confirm_dialog_message")
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("delete_confirm_cancel_button")
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("delete_confirm_delete_button")
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun AboutVlcDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(id = R.drawable.ic_playmuse_logo),
                contentDescription = "PLayMuse Logo",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("about_playmuse_logo")
            )
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PLayMuse",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("about_playmuse_name")
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("about_playmuse_version")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("about_close_button")
            ) {
                Text("Close", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
