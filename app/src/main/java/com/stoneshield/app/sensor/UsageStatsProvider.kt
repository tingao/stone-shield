package com.stoneshield.app.sensor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.stoneshield.app.domain.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SleepBlock(
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Long
)

@Singleton
class UsageStatsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun detectSleepBlock(): SleepBlock? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val lookback = now - Constants.NIGHT_LOOKBACK_HOURS * 60 * 60 * 1000L

        val usageEvents = usm.queryEvents(lookback, now) ?: return null

        var sleepStart: Long? = null
        val candidates = mutableListOf<SleepBlock>()

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            if (!usageEvents.getNextEvent(event)) continue

            when (event.eventType) {
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (sleepStart == null) sleepStart = event.timeStamp
                }
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    sleepStart?.let { start ->
                        val dur = (event.timeStamp - start) / 60_000
                        if (dur >= Constants.NIGHT_DETECT_MIN_HOURS * 60L) {
                            candidates.add(SleepBlock(start, event.timeStamp, dur))
                        }
                    }
                    sleepStart = null
                }
            }
        }

        sleepStart?.let { start ->
            val dur = (now - start) / 60_000
            if (dur >= Constants.NIGHT_DETECT_MIN_HOURS * 60L) {
                candidates.add(SleepBlock(start, now, dur))
            }
        }

        return candidates.maxByOrNull { it.durationMinutes }
    }
}
