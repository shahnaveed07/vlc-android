/*
 * Copyright © 2026 DarkSkullCorporation
 * Developer: Naveed Mushtaq
 *
 * Forked from VLC for Android — Copyright © 2011-2026 VLC authors and VideoLAN
 * Licensed under the GNU General Public License v2 or later (GPLv2+).
 */
package org.videolan.vlc.ui.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.videolan.vlc.model.EqualizerPreset
import org.videolan.vlc.player.VlcPlayerController
import org.videolan.vlc.theme.VlcOrangeAccent
import org.videolan.vlc.theme.VlcOrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    playerController: VlcPlayerController,
    presets: List<EqualizerPreset>,
    selectedPresetName: String,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled by playerController.equalizerEnabled.collectAsState()
    val bands by playerController.equalizerBands.collectAsState()
    val bassBoost by playerController.bassBoostLevel.collectAsState()
    val virtualizer by playerController.virtualizerLevel.collectAsState()

    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    var presetDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Audio Equalizer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { playerController.setEqualizerEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VlcOrangePrimary
                        )
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Presets Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Preset",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = selectedPresetName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = VlcOrangePrimary
                            )
                        )
                    }

                    Box {
                        Button(
                            onClick = { presetDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VlcOrangePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Select Preset", color = Color.White)
                        }

                        DropdownMenu(
                            expanded = presetDropdownExpanded,
                            onDismissRequest = { presetDropdownExpanded = false }
                        ) {
                            presets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.name) },
                                    onClick = {
                                        presetDropdownExpanded = false
                                        onSelectPreset(preset)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Graphic Equalizer Sliders
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Frequency Bands (-12dB to +12dB)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    bandLabels.forEachIndexed { index, label ->
                        val currentVal = bands.getOrElse(index) { 0 }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "${if (currentVal > 0) "+$currentVal" else "$currentVal"} dB",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (currentVal != 0) VlcOrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Slider(
                                value = currentVal.toFloat(),
                                onValueChange = { playerController.setEqualizerBand(index, it.toInt()) },
                                valueRange = -12f..12f,
                                steps = 23,
                                enabled = enabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = VlcOrangePrimary,
                                    activeTrackColor = VlcOrangePrimary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            }

            // Bass Boost & Virtualizer
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sound Effects",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    // Bass Boost
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bass Boost", style = MaterialTheme.typography.bodyMedium)
                            Text("${bassBoost / 10}%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = VlcOrangePrimary))
                        }
                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { playerController.setBassBoost(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = enabled,
                            colors = SliderDefaults.colors(
                                thumbColor = VlcOrangePrimary,
                                activeTrackColor = VlcOrangePrimary
                            )
                        )
                    }

                    // Virtualizer
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("3D Virtualizer / Surround", style = MaterialTheme.typography.bodyMedium)
                            Text("${virtualizer / 10}%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = VlcOrangePrimary))
                        }
                        Slider(
                            value = virtualizer.toFloat(),
                            onValueChange = { playerController.setVirtualizer(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = enabled,
                            colors = SliderDefaults.colors(
                                thumbColor = VlcOrangePrimary,
                                activeTrackColor = VlcOrangePrimary
                            )
                        )
                    }
                }
            }

            // Reset Button
            OutlinedButton(
                onClick = {
                    val flat = presets.find { it.name == "Flat" } ?: EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0))
                    onSelectPreset(flat)
                    playerController.setBassBoost(0)
                    playerController.setVirtualizer(0)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset to Flat")
            }
        }
    }
}
