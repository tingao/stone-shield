package com.stoneshield.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stoneshield.app.data.local.UserPreferences
import com.stoneshield.app.data.repository.ChartPoint
import com.stoneshield.app.data.repository.TankRepository
import com.stoneshield.app.data.repository.TankState
import com.stoneshield.app.domain.PeeColor
import com.stoneshield.app.scheduler.ChargeTimeTracker
import com.stoneshield.app.scheduler.HydrationAlarmScheduler
import com.stoneshield.app.sensor.TemperatureProvider
import com.stoneshield.app.sensor.UsageStatsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val tankState: TankState? = null,
    val chartData: List<ChartPoint> = emptyList(),
    val isLoading: Boolean = true,
    val showBedtimeCheck: Boolean = false,
    val showMorningPrompt: Boolean = false,
    val showUsagePermission: Boolean = false,
    val showNotificationPermission: Boolean = false,
    val showExactAlarmPermission: Boolean = false,
    val waterButtons: List<Int> = listOf(300, 500, 700),
    val hasEvents: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val repository: TankRepository,
    private val temperatureProvider: TemperatureProvider,
    private val usageStatsProvider: UsageStatsProvider,
    private val alarmScheduler: HydrationAlarmScheduler,
    private val chargeTimeTracker: ChargeTimeTracker,
    private val prefs: UserPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        chargeTimeTracker.register()
        loadWaterButtons()
        checkPermissionsSafe()
        runNightProtocol()
    }

    private fun loadWaterButtons() {
        viewModelScope.launch {
            prefs.waterButtons.collect { saved ->
                if (saved.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(waterButtons = saved)
                }
            }
        }
    }

    private fun checkPermissionsSafe() {
        val app = getApplication<Application>()
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                val nm = app.getSystemService(android.app.NotificationManager::class.java)
                if (nm != null && !nm.areNotificationsEnabled()) {
                    _uiState.value = _uiState.value.copy(showNotificationPermission = true)
                }
            }
        } catch (_: Exception) {}
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                val am = app.getSystemService(android.app.AlarmManager::class.java)
                if (am != null && !am.canScheduleExactAlarms()) {
                    _uiState.value = _uiState.value.copy(showExactAlarmPermission = true)
                }
            }
        } catch (_: Exception) {}
        try {
            val usm = app.getSystemService(android.app.usage.UsageStatsManager::class.java)
            if (usm != null) {
                val now = System.currentTimeMillis()
                val stats = usm.queryUsageStats(
                    android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                    now - 86_400_000, now
                )
                if (stats == null || stats.isEmpty()) {
                    _uiState.value = _uiState.value.copy(showUsagePermission = true)
                }
            }
        } catch (_: Exception) {}
    }

    private fun runNightProtocol() {
        viewModelScope.launch {
            try {
                val block = usageStatsProvider.detectSleepBlock()
                if (block != null) {
                    val recentSleep = repository.hasRecentSleepEvent(block.startTime)
                    if (!recentSleep) {
                        repository.addSleepEventAt(block.startTime)
                        val tank = refreshTank()
                        if (tank != null && tank.currentMl < 400) {
                            _uiState.value = _uiState.value.copy(showMorningPrompt = true)
                        }
                    }
                }
            } catch (_: Exception) {}
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            val tank = refreshTank()
            val alarmsEnabled = prefs.alarmEnabled.first()
            if (tank != null && alarmsEnabled) {
                try {
                    val alerts = repository.calculateAlerts(tank)
                    alarmScheduler.scheduleNextWarning(alerts.warningMinutes, alerts.criticalMinutes)
                } catch (_: Exception) {}
            } else if (!alarmsEnabled) {
                alarmScheduler.cancelAlarm()
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun refreshTank(): TankState? {
        val chargeStart = prefs.chargeStartTime.first()
        val chargeMinutes = chargeTimeTracker.getChargeTimeMinutes(chargeStart)
        val temp = temperatureProvider.getEffectiveTemperature()
        val plugged = temperatureProvider.isPluggedIn()
        val state = repository.calculateCurrentTank(temp, plugged, chargeMinutes)
        val chart = repository.calculateChartData(
            temperatureCelsius = temp, isPluggedIn = plugged, chargeTimeMinutes = chargeMinutes
        )
        val hasEvents = repository.getRecentEvents(1).isNotEmpty()
        _uiState.value = _uiState.value.copy(tankState = state, chartData = chart, hasEvents = hasEvents)
        return state
    }

    fun addWater(amount: Int) { viewModelScope.launch {
        val current = _uiState.value.tankState?.currentMl ?: 0
        if (current >= com.stoneshield.app.domain.Constants.SATURATION_CAP) {
            _uiState.value = _uiState.value.copy(message = "Already full (${current}ml) — drinking more will only make you pee more")
            return@launch
        }
        if (current + amount > com.stoneshield.app.domain.Constants.SATURATION_CAP) {
            _uiState.value = _uiState.value.copy(message = "Capped at ${com.stoneshield.app.domain.Constants.SATURATION_CAP}ml — excess will be peed out")
        }
        repository.addWater(amount)
        if (current < com.stoneshield.app.domain.Constants.SATURATION_CAP) {
            _uiState.value = _uiState.value.copy(message = "+${amount}ml water")
        }
        refresh()
    } }
    fun addAlcohol() { viewModelScope.launch { repository.addAlcohol(); _uiState.value = _uiState.value.copy(message = "Alcohol logged (120min diuretic)"); refresh() } }
    fun addPee(volume: Int, color: PeeColor) { viewModelScope.launch { repository.addPee(volume, color); _uiState.value = _uiState.value.copy(message = "Pee logged: ${color.name}"); refresh() } }
    fun addSleep(sweatLevel: Int) { viewModelScope.launch { if (sweatLevel > 0) repository.addSweat(sweatLevel); repository.addSleep(); _uiState.value = _uiState.value.copy(showBedtimeCheck = false, message = "Good night!"); refresh() } }
    fun addWake() { viewModelScope.launch { repository.addWake(); _uiState.value = _uiState.value.copy(showMorningPrompt = false, message = "Good morning!"); refresh() } }
    fun showBedtimeCheck() { _uiState.value = _uiState.value.copy(showBedtimeCheck = true) }
    fun dismissBedtimeCheck() { _uiState.value = _uiState.value.copy(showBedtimeCheck = false) }
    fun dismissMorningPrompt() { _uiState.value = _uiState.value.copy(showMorningPrompt = false) }
    fun morningDrink() { addWater(500); addWake() }
    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }
    fun dismissUsagePermission() { _uiState.value = _uiState.value.copy(showUsagePermission = false) }
    fun dismissNotificationPermission() { _uiState.value = _uiState.value.copy(showNotificationPermission = false) }
    fun dismissExactAlarmPermission() { _uiState.value = _uiState.value.copy(showExactAlarmPermission = false) }
}
