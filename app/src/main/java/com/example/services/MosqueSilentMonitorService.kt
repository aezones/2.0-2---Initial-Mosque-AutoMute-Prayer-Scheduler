package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.model.LocationEntity
import com.example.data.model.LocationType
import com.example.data.model.SilentAction
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * MosqueSilentMonitorService
 * Continuous background location scanner that automatically detects when a user
 * enters within the radius of any registered Mosque (or designated silent zones)
 * and switches the phone to Silent / Vibrate / DND mode instantly.
 */
class MosqueSilentMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var database: AppDatabase

    private var activeZoneEntity: LocationEntity? = null
    private var previousRingerMode: Int = AudioManager.RINGER_MODE_NORMAL

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            serviceScope.launch {
                evaluateLocation(location.latitude, location.longitude)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        database = AppDatabase.getDatabase(this)

        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification()
        startLocationUpdates()
        return START_STICKY
    }

    private fun startForegroundNotification() {
        try {
            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID_MONITOR)
                .setContentTitle("Auto Mosque Silent Active")
                .setContentText("Automatically detecting nearby mosques to silence device upon entry.")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID_FOREGROUND,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID_FOREGROUND,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                }
            } else {
                startForeground(NOTIFICATION_ID_FOREGROUND, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground notification: ${e.message}")
        }
    }

    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000L)
                .setMinUpdateIntervalMillis(8000L)
                .setMinUpdateDistanceMeters(15f)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Mosque silent monitor started listening for location updates.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing location permissions for background monitor: ${e.message}")
        }
    }

    private suspend fun evaluateLocation(currentLat: Double, currentLng: Double) {
        val allLocations = database.locationDao().getAllLocations().first()
        val enabledLocations = allLocations.filter { it.isEnabled }

        var matchingZone: LocationEntity? = null
        var minDistance = Float.MAX_VALUE

        for (loc in enabledLocations) {
            val dist = LocationHelper.calculateDistanceMeters(
                currentLat,
                currentLng,
                loc.latitude,
                loc.longitude
            )
            if (dist <= loc.radiusMeters && dist < minDistance) {
                minDistance = dist
                matchingZone = loc
            }
        }

        if (matchingZone != null && activeZoneEntity?.id != matchingZone.id) {
            // User just entered a Mosque / Silent Zone!
            enterZone(matchingZone)
        } else if (matchingZone == null && activeZoneEntity != null) {
            // User exited the Mosque / Silent Zone!
            exitZone()
        }
    }

    private fun enterZone(zone: LocationEntity) {
        activeZoneEntity = zone
        previousRingerMode = audioManager.ringerMode

        when (zone.action) {
            SilentAction.SILENT -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
            SilentAction.VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
            SilentAction.DND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                } else {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
            }
        }

        showEventNotification(
            title = "Entered Mosque (${zone.name})",
            message = "Phone automatically silenced. Respectful worship mode active."
        )
        Log.i(TAG, "Entered zone: ${zone.name}. Set sound mode to ${zone.action.displayName}")
    }

    private fun exitZone() {
        val prevName = activeZoneEntity?.name ?: "Mosque"
        activeZoneEntity = null

        // Restore previous ringer mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        audioManager.ringerMode = previousRingerMode

        showEventNotification(
            title = "Exited $prevName",
            message = "Phone sound mode automatically restored to normal."
        )
        Log.i(TAG, "Exited zone: $prevName. Restored ringer mode to $previousRingerMode")
    }

    private fun showEventNotification(title: String, message: String) {
        val eventNotification = NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), eventNotification)
    }

    private fun stopMonitoring() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                CHANNEL_ID_MONITOR,
                "Mosque Silent Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status while auto-deducting mosque locations"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Mosque Silent Trigger Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when phone is automatically silenced inside mosque"
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "MosqueSilentService"
        const val CHANNEL_ID_MONITOR = "mosque_silent_monitor_channel"
        const val CHANNEL_ID_ALERTS = "mosque_silent_alerts_channel"
        const val NOTIFICATION_ID_FOREGROUND = 1001

        const val ACTION_START_SERVICE = "com.example.services.START_MOSQUE_MONITOR"
        const val ACTION_STOP_SERVICE = "com.example.services.STOP_MOSQUE_MONITOR"

        fun start(context: Context) {
            try {
                if (!LocationHelper(context).hasLocationPermission()) {
                    Log.w(TAG, "Cannot start MosqueSilentMonitorService: Location permission not granted yet.")
                    return
                }
                val intent = Intent(context, MosqueSilentMonitorService::class.java).apply {
                    action = ACTION_START_SERVICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MosqueSilentMonitorService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, MosqueSilentMonitorService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MosqueSilentMonitorService: ${e.message}")
            }
        }
    }
}
