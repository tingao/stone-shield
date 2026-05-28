package com.stoneshield.app.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.stoneshield.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HydrationAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: HydrationAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val currentMl = intent.getIntExtra("current_ml", 0)
        showNotification(context, currentMl)
    }

    private fun showNotification(context: Context, currentMl: Int) {
        val channelId = "hydration_alerts"
        val channel = NotificationChannel(
            channelId, "Hydration Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for dehydration risk"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Stone Shield Alert")
            .setContentText("Your hydration level is at ${currentMl}ml. Time to drink!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(1001, notification)
    }
}
