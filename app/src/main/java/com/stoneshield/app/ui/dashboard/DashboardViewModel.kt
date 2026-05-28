package com.stoneshield.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stoneshield.app.data.repository.TankRepository
import com.stoneshield.app.data.repository.TankState
import com.stoneshield.app.domain.PeeColor
import com.stoneshield.app.sensor.TemperatureProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val repository: TankRepository,
    private val temperatureProvider: TemperatureProvider
) : AndroidViewModel(application) {

    private val _tankState = MutableStateFlow<TankState?>(null)
    val tankState: StateFlow<TankState?> = _tankState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val state = repository.calculateCurrentTank(
                temperatureCelsius = temperatureProvider.getEffectiveTemperature(),
                isPluggedIn = temperatureProvider.isPluggedIn(),
                chargeTimeMinutes = 0
            )
            _tankState.value = state
            _isLoading.value = false
        }
    }

    fun addWater(amount: Int) {
        viewModelScope.launch {
            repository.addWater(amount)
            refresh()
        }
    }

    fun addAlcohol() {
        viewModelScope.launch {
            repository.addAlcohol()
            refresh()
        }
    }

    fun addPee(volume: Int, color: PeeColor) {
        viewModelScope.launch {
            repository.addPee(volume, color)
            refresh()
        }
    }

    fun addSleep() {
        viewModelScope.launch {
            repository.addSleep()
            refresh()
        }
    }

    fun addWake() {
        viewModelScope.launch {
            repository.addWake()
            refresh()
        }
    }

    fun addSweat(level: Int) {
        viewModelScope.launch {
            repository.addSweat(level)
            refresh()
        }
    }
}
