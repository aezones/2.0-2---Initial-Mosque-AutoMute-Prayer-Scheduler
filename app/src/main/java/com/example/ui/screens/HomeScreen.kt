package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PrayerTimeCard
import com.example.ui.components.StatusCard
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.LocationViewModel
import com.example.viewmodel.PrayerTimeItem
import java.util.Locale

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    locationViewModel: LocationViewModel,
    onNavigateToLocations: () -> Unit,
    onNavigateToAddLocation: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToPrayerTimes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMasterActive by homeViewModel.isMasterAutomationEnabled.collectAsStateWithLifecycle()
    val isMosqueMode by homeViewModel.isMosqueModeEnabled.collectAsStateWithLifecycle()
    val currentTime by homeViewModel.currentTimeString.collectAsStateWithLifecycle()
    val prayerTimes by homeViewModel.prayerTimes.collectAsStateWithLifecycle()
    val totalMosques by homeViewModel.totalMosquesCount.collectAsStateWithLifecycle()
    val totalActiveRules by homeViewModel.totalActiveRulesCount.collectAsStateWithLifecycle()
    val currentGps by locationViewModel.currentGpsLocation.collectAsStateWithLifecycle()

    var configuringPrayerForAlarm by remember { mutableStateOf<PrayerTimeItem?>(null) }
    var alarmDialogEnabled by remember { mutableStateOf(true) }
    var alarmDialogMinutesBefore by remember { mutableIntStateOf(10) }
    var alarmDialogSoundType by remember { mutableStateOf("Gentle Tone") }

    val soundOptions = listOf("Gentle Tone", "Adhan Melody", "Vibrate Only", "Loud Alarm")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Master Status Card
            StatusCard(
                isMasterActive = isMasterActive,
                isMosqueModeEnabled = isMosqueMode,
                onToggleMaster = { homeViewModel.toggleMasterAutomation() },
                onToggleMosqueMode = { homeViewModel.toggleMosqueMode() },
                activeRulesCount = totalActiveRules,
                mosquesCount = totalMosques,
                currentTimeString = currentTime
            )
        }

        // Live GPS Status banner
        item {
            val gpsData = currentGps
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Current GPS Position",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (gpsData != null) {
                                    String.format(
                                        Locale.getDefault(),
                                        "%.4f, %.4f (±%.0fm)",
                                        gpsData.latitude,
                                        gpsData.longitude,
                                        gpsData.accuracyMeters
                                    )
                                } else {
                                    "Location not fetched yet"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { locationViewModel.refreshCurrentGps() },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("refresh_gps_home_button")
                    ) {
                        Text("Detect GPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Navigation Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    onClick = onNavigateToAddLocation,
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_location_button")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLocationAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Add Location",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Card(
                    onClick = onNavigateToLocations,
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_view_locations_button")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Saved ($totalActiveRules)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Prayer Times Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today's Prayer Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onNavigateToPrayerTimes,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text("Configure", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Prayer Time Cards List
        items(
            items = prayerTimes,
            key = { it.name }
        ) { item ->
            PrayerTimeCard(
                item = item,
                onToggleSilent = { homeViewModel.togglePrayerSilent(it) },
                onToggleAlarm = { homeViewModel.togglePrayerAlarm(it) },
                onConfigureAlarm = {
                    configuringPrayerForAlarm = it
                    alarmDialogEnabled = it.isAlarmEnabled
                    alarmDialogMinutesBefore = it.alarmMinutesBefore
                    alarmDialogSoundType = it.alarmSoundType
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Pre-Prayer Alarm Configuration Dialog
    configuringPrayerForAlarm?.let { prayer ->
        AlertDialog(
            onDismissRequest = { configuringPrayerForAlarm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${prayer.name} Alarm Settings", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Enable Pre-Prayer Alarm",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = alarmDialogEnabled,
                            onCheckedChange = { alarmDialogEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (alarmDialogEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Remind me before:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$alarmDialogMinutesBefore mins before",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(5, 10, 15, 20, 30).forEach { mins ->
                                    val isSelected = alarmDialogMinutesBefore == mins
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { alarmDialogMinutesBefore = mins }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "${mins}m",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "Alarm Sound",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                soundOptions.forEach { sound ->
                                    val isSelected = alarmDialogSoundType == sound
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { alarmDialogSoundType = sound }
                                    ) {
                                        Text(
                                            text = sound,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        homeViewModel.updatePrayerAlarmSettings(
                            name = prayer.name,
                            isEnabled = alarmDialogEnabled,
                            minutesBefore = alarmDialogMinutesBefore,
                            soundType = alarmDialogSoundType
                        )
                        configuringPrayerForAlarm = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { configuringPrayerForAlarm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
