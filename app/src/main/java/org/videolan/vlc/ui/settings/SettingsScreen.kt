/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.model.VlcSettings
import org.videolan.vlc.theme.VlcOrangePrimary

@Composable
fun SettingsScreen(
    settings: VlcSettings,
    onSettingsChanged: (VlcSettings) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAbout: () -> Unit,
    onRefreshMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Audio & Equalizer
        SettingsSectionHeader(title = "AUDIO & SOUND")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                SettingsClickableItem(
                    title = "Audio Equalizer",
                    subtitle = "Adjust frequencies, presets, bass boost, and 3D virtualizer",
                    icon = Icons.Default.GraphicEq,
                    onClick = onOpenEqualizer
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                SettingsSwitchItem(
                    title = "Background Audio Playback",
                    subtitle = "Keep audio playing when app is minimized or screen is locked",
                    icon = Icons.Default.Headphones,
                    checked = settings.backgroundPlayback,
                    onCheckedChange = { onSettingsChanged(settings.copy(backgroundPlayback = it)) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                SettingsSwitchItem(
                    title = "Audio Boost",
                    subtitle = "Allow volume up to 200% for quiet recordings",
                    icon = Icons.Default.VolumeUp,
                    checked = settings.audioBoost,
                    onCheckedChange = { onSettingsChanged(settings.copy(audioBoost = it)) }
                )
            }
        }

        // Section: Video Playback & Controls
        SettingsSectionHeader(title = "VIDEO PLAYBACK")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                SettingsSwitchItem(
                    title = "Hardware Acceleration",
                    subtitle = "Use GPU hardware decoding for smooth 4K/60fps playback",
                    icon = Icons.Default.Speed,
                    checked = settings.hardwareAcceleration,
                    onCheckedChange = { onSettingsChanged(settings.copy(hardwareAcceleration = it)) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                SettingsSwitchItem(
                    title = "Playback Gestures",
                    subtitle = "Vertical swipe for brightness/volume and double-tap for seek",
                    icon = Icons.Default.TouchApp,
                    checked = settings.gesturesEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(gesturesEnabled = it)) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                SettingsSwitchItem(
                    title = "Resume Where Left Off",
                    subtitle = "Automatically ask or restore playback position for long videos",
                    icon = Icons.Default.History,
                    checked = settings.resumePlayback,
                    onCheckedChange = { onSettingsChanged(settings.copy(resumePlayback = it)) }
                )
            }
        }

        // Section: Library & Actions
        SettingsSectionHeader(title = "MEDIA LIBRARY")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                SettingsClickableItem(
                    title = "Rescan Media Library",
                    subtitle = "Refresh device storage for newly added videos and music files",
                    icon = Icons.Default.Refresh,
                    onClick = onRefreshMedia
                )
            }
        }

        // Section: About & Attribution
        SettingsSectionHeader(title = "ABOUT PLAYMUSE")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                SettingsClickableItem(
                    title = "About PLayMuse",
                    subtitle = "Developer: Naveed Mushtaq • Copyright © 2026 DarkSkullCorporation",
                    icon = Icons.Default.Info,
                    onClick = onOpenAbout
                )

               
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            color = VlcOrangePrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = VlcOrangePrimary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = VlcOrangePrimary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VlcOrangePrimary
            )
        )
    }
}
