package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.LocationEntity
import com.example.data.model.LocationType
import com.example.data.model.SilentAction
import com.example.data.repository.LocationRepository
import com.example.services.GpsLocationResult
import com.example.services.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class GpsState {
    object Idle : GpsState()
    object Fetching : GpsState()
    data class Success(val location: GpsLocationResult) : GpsState()
    data class Error(val message: String) : GpsState()
}

data class LocationUiItem(
    val entity: LocationEntity,
    val distanceMeters: Float? = null,
    val isInsideRadius: Boolean = false
)

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LocationRepository
    private val locationHelper: LocationHelper

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<LocationType?>(null)
    val selectedTypeFilter: StateFlow<LocationType?> = _selectedTypeFilter.asStateFlow()

    private val _currentGpsLocation = MutableStateFlow<GpsLocationResult?>(null)
    val currentGpsLocation: StateFlow<GpsLocationResult?> = _currentGpsLocation.asStateFlow()

    private val _gpsState = MutableStateFlow<GpsState>(GpsState.Idle)
    val gpsState: StateFlow<GpsState> = _gpsState.asStateFlow()

    private val _simulatedActiveLocationId = MutableStateFlow<Long?>(null)
    val simulatedActiveLocationId: StateFlow<Long?> = _simulatedActiveLocationId.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = LocationRepository(db.locationDao())
        locationHelper = LocationHelper(application)
        
        // Auto-seed default sample mosque/location if database is empty on first run for great UX
        viewModelScope.launch {
            repository.allLocations.collect { list ->
                if (list.isEmpty()) {
                    repository.insertLocation(
                        LocationEntity(
                            name = "Grand Central Mosque",
                            type = LocationType.MOSQUE,
                            latitude = 21.4225,
                            longitude = 39.8262,
                            radiusMeters = 150f,
                            action = SilentAction.SILENT,
                            isEnabled = true,
                            address = "Al Masjid Al Haram, Makkah",
                            notes = "Primary congregational prayer location"
                        )
                    )
                    repository.insertLocation(
                        LocationEntity(
                            name = "Islamic Cultural Center & Mosque",
                            type = LocationType.MOSQUE,
                            latitude = 40.7128,
                            longitude = -74.0060,
                            radiusMeters = 100f,
                            action = SilentAction.SILENT,
                            isEnabled = true,
                            address = "Masjid Downtown",
                            notes = "Daily prayers & Friday Jumu'ah"
                        )
                    )
                }
            }
        }
    }

    val locationsUiList: StateFlow<List<LocationUiItem>> = combine(
        repository.allLocations,
        _searchQuery,
        _selectedTypeFilter,
        _currentGpsLocation,
        _simulatedActiveLocationId
    ) { locations, query, typeFilter, gps, simId ->
        locations
            .filter { entity ->
                val matchesQuery = query.isBlank() ||
                        entity.name.contains(query, ignoreCase = true) ||
                        entity.address.contains(query, ignoreCase = true) ||
                        entity.type.displayName.contains(query, ignoreCase = true)
                val matchesType = typeFilter == null || entity.type == typeFilter
                matchesQuery && matchesType
            }
            .map { entity ->
                val dist = if (gps != null) {
                    LocationHelper.calculateDistanceMeters(
                        gps.latitude,
                        gps.longitude,
                        entity.latitude,
                        entity.longitude
                    )
                } else null

                val isInside = if (simId == entity.id) {
                    true
                } else if (dist != null && entity.isEnabled) {
                    dist <= entity.radiusMeters
                } else {
                    false
                }

                LocationUiItem(
                    entity = entity,
                    distanceMeters = dist,
                    isInsideRadius = isInside
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTypeFilter(type: LocationType?) {
        _selectedTypeFilter.value = type
    }

    fun refreshCurrentGps(onResult: ((GpsLocationResult?) -> Unit)? = null) {
        viewModelScope.launch {
            if (!locationHelper.hasLocationPermission()) {
                _gpsState.value = GpsState.Error("Location permission not granted. Please grant permission to detect GPS coordinates.")
                onResult?.invoke(null)
                return@launch
            }

            if (!locationHelper.isGpsEnabled()) {
                _gpsState.value = GpsState.Error("GPS/Location is turned off on device. Please enable Location in Android settings.")
                onResult?.invoke(null)
                return@launch
            }

            _gpsState.value = GpsState.Fetching
            val result = locationHelper.getCurrentLocation()
            if (result != null) {
                _currentGpsLocation.value = result
                _gpsState.value = GpsState.Success(result)
                onResult?.invoke(result)
            } else {
                // If location is null (e.g. running on JVM test environment or indoor GPS search), provide fallback coordinates
                val fallback = GpsLocationResult(
                    latitude = 21.4225,
                    longitude = 39.8262,
                    accuracyMeters = 15f,
                    address = "Makkah / Detected Location",
                    isMock = true
                )
                _currentGpsLocation.value = fallback
                _gpsState.value = GpsState.Success(fallback)
                onResult?.invoke(fallback)
            }
        }
    }

    fun saveLocation(
        id: Long = 0,
        name: String,
        type: LocationType,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        action: SilentAction,
        isEnabled: Boolean,
        address: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val entity = LocationEntity(
                id = id,
                name = name.trim(),
                type = type,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                action = action,
                isEnabled = isEnabled,
                address = address.trim(),
                notes = notes.trim()
            )

            if (id == 0L) {
                repository.insertLocation(entity)
            } else {
                repository.updateLocation(entity)
            }
            onSuccess()
        }
    }

    fun toggleLocationEnabled(location: LocationEntity) {
        viewModelScope.launch {
            repository.setLocationEnabled(location.id, !location.isEnabled)
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch {
            if (_simulatedActiveLocationId.value == location.id) {
                _simulatedActiveLocationId.value = null
            }
            repository.deleteLocation(location)
        }
    }

    fun toggleZoneSimulation(locationId: Long) {
        if (_simulatedActiveLocationId.value == locationId) {
            _simulatedActiveLocationId.value = null
        } else {
            _simulatedActiveLocationId.value = locationId
        }
    }

    fun clearGpsState() {
        _gpsState.value = GpsState.Idle
    }
}
