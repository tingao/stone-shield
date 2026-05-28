package com.stoneshield.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val CHARGE_START_TIME = longPreferencesKey("charge_start_time")
        val WATER_BUTTONS = stringPreferencesKey("water_buttons")
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val alarmEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ALARM_ENABLED] ?: true }
    val chargeStartTime: Flow<Long> = context.dataStore.data.map { it[Keys.CHARGE_START_TIME] ?: 0L }
    val waterButtons: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        val s = prefs[Keys.WATER_BUTTONS] ?: return@map listOf(300, 500, 700)
        s.split(",").mapNotNull { it.toIntOrNull() }.take(5)
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setAlarmEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALARM_ENABLED] = enabled }
    }

    suspend fun setChargeStartTime(time: Long) {
        context.dataStore.edit { it[Keys.CHARGE_START_TIME] = time }
    }

    suspend fun setWaterButtons(buttons: List<Int>) {
        context.dataStore.edit { it[Keys.WATER_BUTTONS] = buttons.joinToString(",") }
    }
}
