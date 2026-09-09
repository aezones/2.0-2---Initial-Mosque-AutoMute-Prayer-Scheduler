package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: LocationType,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val action: SilentAction = SilentAction.SILENT,
    val isEnabled: Boolean = true,
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
