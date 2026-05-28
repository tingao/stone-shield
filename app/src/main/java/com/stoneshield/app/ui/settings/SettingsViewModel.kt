package com.stoneshield.app.ui.settings

import androidx.lifecycle.ViewModel
import com.stoneshield.app.data.local.UserPreferences
import com.stoneshield.app.data.repository.TankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val repository: TankRepository
) : ViewModel() {
    val alarmEnabled: Flow<Boolean> = prefs.alarmEnabled

    suspend fun toggleAlarm(enabled: Boolean) {
        prefs.setAlarmEnabled(enabled)
    }

    suspend fun clearAllData() {
        repository.clearAll()
    }
}
