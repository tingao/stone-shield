package com.stoneshield.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoneshield.app.data.local.UserPreferences
import com.stoneshield.app.data.repository.TankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val repository: TankRepository
) : ViewModel() {
    val alarmEnabled: StateFlow<Boolean> = prefs.alarmEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val waterButtons: StateFlow<List<Int>> = prefs.waterButtons
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(300, 500, 700))

    fun toggleAlarm(enabled: Boolean) {
        viewModelScope.launch { prefs.setAlarmEnabled(enabled) }
    }

    fun setWaterButtons(buttons: List<Int>) {
        viewModelScope.launch { prefs.setWaterButtons(buttons) }
    }

    fun clearAllData() {
        viewModelScope.launch { repository.clearAll() }
    }
}
