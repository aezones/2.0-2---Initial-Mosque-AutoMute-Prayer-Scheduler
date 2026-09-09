package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationEntity
import com.example.data.model.LocationType
import com.example.data.model.SilentAction
import com.example.services.LocationHelper
import com.example.viewmodel.LocationUiItem
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationCard(
    item: LocationUiItem,
    onToggleEnabled: (LocationEntity) -> Unit,
    onEdit: (LocationEntity) -> Unit,
    onDelete: (LocationEntity) -> Unit,
    onSimulateToggle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val location = item.entity
    val isInside = item.isInsideRadius

    val cardBorderColor = if (isInside) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val containerColor = if (isInside) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("location_card_${location.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(if (isInside) 1.5.dp else 1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInside) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Type Icon + Name + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when (location.type) {
                                LocationType.MOSQUE -> Color(0xFFEFF6FF)
                                LocationType.HOME -> Color(0xFFDCFCE7)
                                LocationType.OFFICE -> Color(0xFFE0F2FE)
                                LocationType.SCHOOL -> Color(0xFFFFEDD5)
                                LocationType.CUSTOM -> Color(0xFFF3E8FF)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (location.type) {
                            LocationType.MOSQUE -> Icons.Default.Mosque
                            LocationType.HOME -> Icons.Default.Home
                            LocationType.OFFICE -> Icons.Default.Business
                            LocationType.SCHOOL -> Icons.Default.School
                            LocationType.CUSTOM -> Icons.Default.LocationOn
                        },
                        contentDescription = location.type.displayName,
                        tint = when (location.type) {
                            LocationType.MOSQUE -> Color(0xFF2563EB)
                            LocationType.HOME -> Color(0xFF16A34A)
                            LocationType.OFFICE -> Color(0xFF0284C7)
                            LocationType.SCHOOL -> Color(0xFFEA580C)
                            LocationType.CUSTOM -> Color(0xFF7E22CE)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Type label
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = location.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Enabled / Disabled Switch
                Switch(
                    checked = location.isEnabled,
                    onCheckedChange = { onToggleEnabled(location) },
                    modifier = Modifier.testTag("switch_location_${location.id}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Address / Note if available
            if (location.address.isNotBlank()) {
                Text(
                    text = location.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Chips / Badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Action Chip
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = when (location.action) {
                        SilentAction.SILENT -> Color(0xFFFFEDD5)
                        SilentAction.VIBRATE -> Color(0xFFF3E8FF)
                        SilentAction.DND -> Color(0xFFFEE2E2)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (location.action) {
                                SilentAction.SILENT -> Icons.Default.VolumeOff
                                SilentAction.VIBRATE -> Icons.Default.Vibration
                                SilentAction.DND -> Icons.Default.NotificationsOff
                            },
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = when (location.action) {
                                SilentAction.SILENT -> Color(0xFF9A3412)
                                SilentAction.VIBRATE -> Color(0xFF6B21A8)
                                SilentAction.DND -> Color(0xFF991B1B)
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = location.action.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (location.action) {
                                SilentAction.SILENT -> Color(0xFF9A3412)
                                SilentAction.VIBRATE -> Color(0xFF6B21A8)
                                SilentAction.DND -> Color(0xFF991B1B)
                            }
                        )
                    }
                }

                // Radius Chip
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Radius: ${location.radiusMeters.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Coordinates Chip
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.4f, %.4f", location.latitude, location.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proximity & Geofence Status Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isInside) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isInside) Color(0xFF2563EB)
                                    else if (!location.isEnabled) Color.Gray
                                    else Color(0xFFF59E0B)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!location.isEnabled) {
                                "Rule is disabled"
                            } else if (isInside) {
                                "INSIDE ZONE - ${location.action.displayName} Active"
                            } else if (item.distanceMeters != null) {
                                "${LocationHelper.formatDistance(item.distanceMeters)} away"
                            } else {
                                "Tap GPS to calculate distance"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isInside) FontWeight.Bold else FontWeight.Medium,
                            color = if (isInside) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Edit, Delete, Simulate Zone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Test Simulate Button
                FilledTonalButton(
                    onClick = { onSimulateToggle(location.id) },
                    modifier = Modifier.testTag("simulate_button_${location.id}"),
                    shape = RoundedCornerShape(999.dp),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isInside) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isInside) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isInside) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isInside) "Exit Zone" else "Test Zone",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isInside) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onEdit(location) },
                        modifier = Modifier.testTag("edit_location_${location.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDelete(location) },
                        modifier = Modifier.testTag("delete_location_${location.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete location",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
