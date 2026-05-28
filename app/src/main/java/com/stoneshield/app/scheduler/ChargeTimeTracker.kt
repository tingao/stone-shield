package com.stoneshield.app.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.stoneshield.app.data.local.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargeTimeTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    scope.launch { prefs.setChargeStartTime(System.currentTimeMillis()) }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    scope.launch { prefs.setChargeStartTime(0L) }
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
    }

    fun getChargeTimeMinutes(chargeStartTime: Long): Long {
        if (chargeStartTime <= 0) return 0
        return (System.currentTimeMillis() - chargeStartTime) / 60_000
    }
}
