package com.stoneshield.app.sensor

import android.content.Context
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemperatureProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getBatteryTemperature(): Double? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        val temp = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE)
        return if (temp == Int.MIN_VALUE) null else temp / 10.0
    }

    fun isPluggedIn(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        return bm.isCharging
    }

    fun getChargeTimeMinutes(): Long {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return 0
        val plugTime = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        return if (plugTime <= 0) 0 else System.currentTimeMillis() - plugTime
    }
}
