package com.stoneshield.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.stoneshield.app.domain.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrationAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNextWarning(minutesUntilWarning: Long, minutesUntilCritical: Long) {
        val nextMinutes = if (minutesUntilWarning <= 0 && minutesUntilCritical <= 0) {
            return
        } else if (minutesUntilWarning <= 0) {
            minutesUntilCritical
        } else if (minutesUntilCritical <= 0) {
            minutesUntilWarning
        } else {
            minOf(minutesUntilWarning, minutesUntilCritical)
        }

        if (nextMinutes <= 0 || nextMinutes == Long.MAX_VALUE) return

        val triggerTime = System.currentTimeMillis() + (nextMinutes * 60_000)
        val intent = Intent(context, HydrationAlarmReceiver::class.java).apply {
            putExtra("current_ml", 0)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancelAlarm() {
        val intent = Intent(context, HydrationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
