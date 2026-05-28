package com.stoneshield.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stoneshield.app.data.local.UserPreferences
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
    val isLoading: Boolean = true,
    val showBedtimeCheck: Boolean = false,
    val showMorningPrompt: Boolean = false,
    val showUsagePermission: Boolean = false,
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
        checkUsagePermission()
        runNightProtocol()
    }

    private fun checkUsagePermission() {
        viewModelScope.launch {
            val usm = getApplication<Application>()
                .getSystemService(android.app.usage.UsageStatsManager::class.java)
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
        }
    }

    private fun runNightProtocol() {
        viewModelScope.launch {
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
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            val tank = refreshTank()
            if (tank != null) {
                val alarmsEnabled = prefs.alarmEnabled.first()
                if (alarmsEnabled) {
                    val alerts = repository.calculateAlerts(tank)
                    alarmScheduler.scheduleNextWarning(alerts.warningMinutes, alerts.criticalMinutes)
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun refreshTank(): TankState? {
        val chargeStart = prefs.chargeStartTime.first()
        val chargeMinutes = chargeTimeTracker.getChargeTimeMinutes(chargeStart)
        val state = repository.calculateCurrentTank(
            temperatureCelsius = temperatureProvider.getEffectiveTemperature(),
            isPluggedIn = temperatureProvider.isPluggedIn(),
            chargeTimeMinutes = chargeMinutes
        )
        _uiState.value = _uiState.value.copy(tankState = state)
        return state
    }

    fun addWater(amount: Int) {
        viewModelScope.launch {
            repository.addWater(amount)
            _uiState.value = _uiState.value.copy(message = "+${amount}ml water")
            refresh()
        }
    }

    fun addAlcohol() {
        viewModelScope.launch {
            repository.addAlcohol()
            _uiState.value = _uiState.value.copy(message = "Alcohol logged (120min diuretic)")
            refresh()
        }
    }

    fun addPee(volume: Int, color: PeeColor) {
        viewModelScope.launch {
            repository.addPee(volume, color)
            _uiState.value = _uiState.value.copy(message = "Pee logged: ${color.name}")
            refresh()
        }
    }

    fun addSleep(sweatLevel: Int) {
        viewModelScope.launch {
            if (sweatLevel > 0) repository.addSweat(sweatLevel)
            repository.addSleep()
            _uiState.value = _uiState.value.copy(
                showBedtimeCheck = false,
                message = "Good night! Sleeping..."
            )
            refresh()
        }
    }

    fun addWake() {
        viewModelScope.launch {
            repository.addWake()
            _uiState.value = _uiState.value.copy(
                showMorningPrompt = false,
                message = "Good morning! Hydrate!"
            )
            refresh()
        }
    }

    fun showBedtimeCheck() {
        _uiState.value = _uiState.value.copy(showBedtimeCheck = true)
    }

    fun dismissBedtimeCheck() {
        _uiState.value = _uiState.value.copy(showBedtimeCheck = false)
    }

    fun dismissMorningPrompt() {
        _uiState.value = _uiState.value.copy(showMorningPrompt = false)
    }

    fun morningDrink() {
        addWater(500)
        addWake()
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun showUsagePermissionDialog() {
        _uiState.value = _uiState.value.copy(showUsagePermission = true)
    }

    fun dismissUsagePermission() {
        _uiState.value = _uiState.value.copy(showUsagePermission = false)
    }
}
