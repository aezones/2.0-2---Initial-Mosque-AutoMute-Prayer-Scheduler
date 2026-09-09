package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY createdAt DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    fun getLocationById(id: Long): Flow<LocationEntity?>

    @Query("SELECT * FROM saved_locations WHERE isEnabled = 1")
    fun getActiveLocations(): Flow<List<LocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Long)

    @Query("UPDATE saved_locations SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateLocationEnabled(id: Long, isEnabled: Boolean)
}
