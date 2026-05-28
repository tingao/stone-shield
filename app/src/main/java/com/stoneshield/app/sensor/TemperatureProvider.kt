package com.stoneshield.app.sensor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.stoneshield.app.domain.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemperatureProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getBatteryTemperature(): Double? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: return null
        return if (temp == -1) null else temp / 10.0
    }

    fun isPluggedIn(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        return bm.isCharging
    }

    fun getEffectiveTemperature(): Double {
        val rawTemp = getBatteryTemperature() ?: Constants.ROOM_TEMP
        if (!isPluggedIn()) return rawTemp
        return if (rawTemp >= 30.0) Constants.ROOM_TEMP else rawTemp
    }
}
