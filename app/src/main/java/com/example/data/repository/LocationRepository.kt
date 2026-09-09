package com.example.data.repository

import com.example.data.dao.LocationDao
import com.example.data.model.LocationEntity
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val locationDao: LocationDao) {

    val allLocations: Flow<List<LocationEntity>> = locationDao.getAllLocations()
    val activeLocations: Flow<List<LocationEntity>> = locationDao.getActiveLocations()

    fun getLocationById(id: Long): Flow<LocationEntity?> {
        return locationDao.getLocationById(id)
    }

    suspend fun insertLocation(location: LocationEntity): Long {
        return locationDao.insertLocation(location)
    }

    suspend fun updateLocation(location: LocationEntity) {
        locationDao.updateLocation(location)
    }

    suspend fun deleteLocation(location: LocationEntity) {
        locationDao.deleteLocation(location)
    }

    suspend fun deleteLocationById(id: Long) {
        locationDao.deleteLocationById(id)
    }

    suspend fun setLocationEnabled(id: Long, isEnabled: Boolean) {
        locationDao.updateLocationEnabled(id, isEnabled)
    }
}
