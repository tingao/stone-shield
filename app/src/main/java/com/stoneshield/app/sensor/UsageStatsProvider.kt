package com.stoneshield.app.sensor

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
        val lookback = now - Constants.NIGHT_LOOKBACK_HOURS * 60 * 60 * 1000

        val usageStats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, lookback, now
        ) ?: return null

        if (usageStats.isEmpty()) return null

        usageStats.sortBy { it.firstTimeStamp }

        var longestScreenOff: SleepBlock? = null
        var lastScreenOffStart: Long? = null

        for (stats in usageStats) {
            if (stats.lastTimeUsed < lookback) continue

            val isScreenOff = stats.lastTimeUsed > 0 &&
                stats.totalTimeInForeground == 0L

            if (isScreenOff) {
                if (lastScreenOffStart == null) {
                    lastScreenOffStart = stats.firstTimeStamp
                }
            } else {
                lastScreenOffStart?.let { start ->
                    val end = stats.firstTimeStamp
                    val dur = (end - start) / 60_000
                    if (dur > Constants.NIGHT_DETECT_MIN_HOURS * 60) {
                        val current = SleepBlock(start, end, dur)
                        if (longestScreenOff == null || dur > longestScreenOff.durationMinutes) {
                            longestScreenOff = current
                        }
                    }
                }
                lastScreenOffStart = null
            }
        }

        lastScreenOffStart?.let { start ->
            val dur = (now - start) / 60_000
            if (dur > Constants.NIGHT_DETECT_MIN_HOURS * 60) {
                val current = SleepBlock(start, now, dur)
                if (longestScreenOff == null || dur > longestScreenOff.durationMinutes) {
                    longestScreenOff = current
                }
            }
        }

        return longestScreenOff
    }
}
