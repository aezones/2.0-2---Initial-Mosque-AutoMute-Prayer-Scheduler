package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.LocationType
import com.example.data.model.SilentAction
import com.example.data.repository.LocationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PrayerTimeItem(
    val name: String,
    val arabicName: String,
    val timeFormatted: String,
    val isNext: Boolean = false,
    val isPassed: Boolean = false,
    val isSilentEnabled: Boolean = true,
    val action: SilentAction = SilentAction.SILENT,
    val isAlarmEnabled: Boolean = false,
    val alarmMinutesBefore: Int = 10,
    val alarmSoundType: String = "Gentle Tone"
)

data class HomeAutomationState(
    val isMosqueModeEnabled: Boolean = true,
    val isMasterAutomationEnabled: Boolean = true,
    val currentRingerStatus: String = "Normal (Ringer ON)",
    val activeRuleName: String? = null,
    val nextEventDescription: String = "Next: Asr in 42 mins"
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LocationRepository

    private val _isMosqueModeEnabled = MutableStateFlow(true)
    val isMosqueModeEnabled: StateFlow<Boolean> = _isMosqueModeEnabled.asStateFlow()

    private val _isMasterAutomationEnabled = MutableStateFlow(true)
    val isMasterAutomationEnabled: StateFlow<Boolean> = _isMasterAutomationEnabled.asStateFlow()

    private val _currentTimeString = MutableStateFlow("")
    val currentTimeString: StateFlow<String> = _currentTimeString.asStateFlow()

    private val _prayerTimes = MutableStateFlow<List<PrayerTimeItem>>(emptyList())
    val prayerTimes: StateFlow<List<PrayerTimeItem>> = _prayerTimes.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = LocationRepository(db.locationDao())

        initPrayerTimes()
        startClockTicker()
    }

    val totalMosquesCount: StateFlow<Int> = repository.allLocations.map { list ->
        list.count { it.type == LocationType.MOSQUE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalActiveRulesCount: StateFlow<Int> = repository.allLocations.map { list ->
        list.count { it.isEnabled }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun updateServiceState(enabled: Boolean) {
        try {
            if (enabled) {
                com.example.services.MosqueSilentMonitorService.start(getApplication())
            } else {
                com.example.services.MosqueSilentMonitorService.stop(getApplication())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initPrayerTimes() {
        val list = listOf(
            PrayerTimeItem("Fajr", "الفجر", "05:12 AM", isPassed = true, isSilentEnabled = true),
            PrayerTimeItem("Sunrise", "الشروق", "06:34 AM", isPassed = true, isSilentEnabled = false),
            PrayerTimeItem("Dhuhr", "الظهر", "12:45 PM", isPassed = true, isSilentEnabled = true),
            PrayerTimeItem("Asr", "العصر", "04:15 PM", isNext = true, isSilentEnabled = true),
            PrayerTimeItem("Maghrib", "المغرب", "06:58 PM", isSilentEnabled = true),
            PrayerTimeItem("Isha", "العشاء", "08:25 PM", isSilentEnabled = true)
        )
        _prayerTimes.value = list
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            val format = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            while (true) {
                _currentTimeString.value = format.format(Date())
                delay(1000)
            }
        }
    }

    fun toggleMosqueMode() {
        val next = !_isMosqueModeEnabled.value
        _isMosqueModeEnabled.value = next
        updateServiceState(next && _isMasterAutomationEnabled.value)
    }

    fun toggleMasterAutomation() {
        val next = !_isMasterAutomationEnabled.value
        _isMasterAutomationEnabled.value = next
        updateServiceState(next && _isMosqueModeEnabled.value)
    }

    fun togglePrayerSilent(name: String) {
        _prayerTimes.value = _prayerTimes.value.map { item ->
            if (item.name == name) {
                item.copy(isSilentEnabled = !item.isSilentEnabled)
            } else {
                item
            }
        }
    }

    fun togglePrayerAlarm(name: String) {
        _prayerTimes.value = _prayerTimes.value.map { item ->
            if (item.name == name) {
                item.copy(isAlarmEnabled = !item.isAlarmEnabled)
            } else {
                item
            }
        }
    }

    fun updatePrayerAlarmSettings(name: String, isEnabled: Boolean, minutesBefore: Int, soundType: String) {
        _prayerTimes.value = _prayerTimes.value.map { item ->
            if (item.name == name) {
                item.copy(
                    isAlarmEnabled = isEnabled,
                    alarmMinutesBefore = minutesBefore,
                    alarmSoundType = soundType
                )
            } else {
                item
            }
        }
    }
}
