package com.stoneshield.app.data.repository

import com.stoneshield.app.data.local.EventEntity
import com.stoneshield.app.data.local.dao.EventDao
import com.stoneshield.app.domain.BodyState
import com.stoneshield.app.domain.Constants
import com.stoneshield.app.domain.HydrationMath
import com.stoneshield.app.domain.PeeColor
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import javax.inject.Inject
import javax.inject.Singleton

data class TankState(
    val currentMl: Int,
    val bodyState: BodyState,
    val alcoholActive: Boolean,
    val alcoholUntil: Long,
    val effectiveRate: Double,
    val lastEventTime: Long,
    val lastVolume: Int
)

data class AlertInfo(
    val warningMinutes: Long,
    val criticalMinutes: Long
)

data class ChartPoint(
    val timestamp: Long,
    val volume: Int,
    val isEvent: Boolean = false
)

data class DaySummary(
    val date: Long,
    val hasData: Boolean,
    val avgMl: Int = 0,
    val totalWater: Int = 0,
    val minMl: Int = 0,
    val maxMl: Int = 0,
    val dangerMinutes: Int = 0
)

@Singleton
class TankRepository @Inject constructor(
    private val eventDao: EventDao
) {
    fun getAllEvents(): Flow<List<EventEntity>> = eventDao.getAllEvents()

    fun getEventsSince(since: Long): Flow<List<EventEntity>> = eventDao.getEventsSince(since)

    suspend fun addWater(amount: Int) {
        eventDao.insertEvent(
            EventEntity(
                timestamp = System.currentTimeMillis(),
                type = EventEntity.TYPE_WATER,
                value = amount
            )
        )
    }

    suspend fun addAlcohol() {
        eventDao.insertEvent(
            EventEntity(
                timestamp = System.currentTimeMillis(),
                type = EventEntity.TYPE_ALCOHOL,
                value = 0
            )
        )
    }

    suspend fun addPee(volume: Int, color: PeeColor) {
        eventDao.insertEvent(
            EventEntity(
                timestamp = System.currentTimeMillis(),
                type = EventEntity.TYPE_PEE,
                value = volume,
                note = color.name
            )
        )
        eventDao.insertEvent(
            EventEntity(
                timestamp = System.currentTimeMillis(),
                type = EventEntity.TYPE_COLOR_SNAP,
                value = color.ordinal
            )
        )
    }

    suspend fun addSleep() {
        eventDao.insertEvent(
            EventEntity(
                timestamp = System.currentTimeMillis(),
                type = EventEntity.TYPE_SLEEP,
                value = 0
            )
        )
    }

    suspend fun addWake() {
        eventDao.insertEvent(
            EventEntity(
                timestamp = System.currentTimeMillis(),
                type = EventEntity.TYPE_WAKE,
                value = 0
            )
        )
    }

    suspend fun addSweat(level: Int) {
        eventDao.insertEvent(
            EventEntity(
                timestamp = System.currentTimeMillis(),
                type = EventEntity.TYPE_SWEAT,
                value = level
            )
        )
    }

    suspend fun calculateCurrentTank(
        temperatureCelsius: Double?,
        isPluggedIn: Boolean,
        chargeTimeMinutes: Long
    ): TankState {
        val now = System.currentTimeMillis()
        val events = eventDao.getEventsSinceSync(now - 24 * 60 * 60 * 1000)

        var lastVolume = Constants.SATURATION_CAP
        var lastEventTime = now - 24 * 60 * 60 * 1000
        var bodyState = BodyState.AWAKE
        var alcoholActive = false
        var alcoholUntil = 0L

        for (event in events) {
            val elapsed = (event.timestamp - lastEventTime) / 60_000
            val effectiveRate = HydrationMath.calculateEffectiveRate(
                state = bodyState,
                temperatureCelsius = temperatureCelsius,
                alcoholActive = alcoholActive,
                isPluggedIn = isPluggedIn,
                chargeTimeMinutes = chargeTimeMinutes
            )

            lastVolume = HydrationMath.calculateCurrentTank(
                lastVolume = lastVolume,
                inputs = 0,
                outputs = 0,
                elapsedMinutes = max(0, elapsed),
                effectiveRate = effectiveRate
            )

            when (event.type) {
                EventEntity.TYPE_WATER -> lastVolume += event.value
                EventEntity.TYPE_ALCOHOL -> {
                    alcoholActive = true
                    alcoholUntil = event.timestamp + Constants.ALCOHOL_DURATION_MIN * 60_000
                }
                EventEntity.TYPE_PEE -> lastVolume -= event.value
                EventEntity.TYPE_COLOR_SNAP -> {
                    val color = PeeColor.entries[event.value]
                    lastVolume = HydrationMath.applyColorSnap(lastVolume, color)
                }
                EventEntity.TYPE_SLEEP -> bodyState = BodyState.SLEEP
                EventEntity.TYPE_WAKE -> bodyState = BodyState.AWAKE
                EventEntity.TYPE_SWEAT -> {
                    lastVolume += if (event.value == 0) Constants.SWEAT_PENALTY_LIGHT else Constants.SWEAT_PENALTY_HEAVY
                }
            }

            lastVolume = lastVolume.coerceIn(0, Constants.SATURATION_CAP)
            lastEventTime = event.timestamp
        }

        val elapsedNow = (now - lastEventTime) / 60_000
        alcoholActive = alcoholActive && now < alcoholUntil

        val finalRate = HydrationMath.calculateEffectiveRate(
            state = bodyState,
            temperatureCelsius = temperatureCelsius,
            alcoholActive = alcoholActive,
            isPluggedIn = isPluggedIn,
            chargeTimeMinutes = chargeTimeMinutes
        )

        val currentMl = HydrationMath.calculateCurrentTank(
            lastVolume = lastVolume,
            inputs = 0,
            outputs = 0,
            elapsedMinutes = max(0, elapsedNow),
            effectiveRate = finalRate
        )

        return TankState(
            currentMl = currentMl,
            bodyState = bodyState,
            alcoholActive = alcoholActive,
            alcoholUntil = alcoholUntil,
            effectiveRate = finalRate,
            lastEventTime = now,
            lastVolume = currentMl
        )
    }

    fun calculateAlerts(tankState: TankState): AlertInfo {
        return AlertInfo(
            warningMinutes = HydrationMath.minutesUntilThreshold(
                tankState.currentMl, Constants.SAFE_FLOOR, tankState.effectiveRate
            ),
            criticalMinutes = HydrationMath.minutesUntilThreshold(
                tankState.currentMl, Constants.DANGER_FLOOR, tankState.effectiveRate
            )
        )
    }

    suspend fun calculateChartData(
        temperatureCelsius: Double?,
        isPluggedIn: Boolean,
        chargeTimeMinutes: Long
    ): List<ChartPoint> {
        val now = System.currentTimeMillis()
        val lookback = now - 24 * 60 * 60 * 1000L
        val events = eventDao.getEventsSinceSync(lookback)
        val sampleIntervalMs = 5 * 60 * 1000L
        val points = mutableListOf<ChartPoint>()

        // Determine start time: earliest event or 3h ago
        val effectiveStart = if (events.isNotEmpty()) events.first().timestamp else now - 3 * 60 * 60 * 1000L

        var lastVolume = Constants.SATURATION_CAP
        var lastEventTime = effectiveStart
        var bodyState = BodyState.AWAKE
        var alcoholActive = false
        var alcoholUntil = 0L

        fun computeRate() = HydrationMath.calculateEffectiveRate(
            bodyState, temperatureCelsius, alcoholActive, isPluggedIn, chargeTimeMinutes
        )

        fun simulate(toTime: Long) {
            var cursor = lastEventTime + sampleIntervalMs
            while (cursor <= toTime) {
                val elapsed = (cursor - lastEventTime) / 60_000
                val rate = computeRate()
                lastVolume = HydrationMath.calculateCurrentTank(lastVolume, 0, 0, max(0, elapsed), rate)
                lastEventTime = cursor
                points.add(ChartPoint(cursor, lastVolume))
                cursor += sampleIntervalMs
            }
        }

        for (event in events) {
            val elapsed = (event.timestamp - lastEventTime) / 60_000
            val rate = computeRate()
            lastVolume = HydrationMath.calculateCurrentTank(lastVolume, 0, 0, max(0, elapsed), rate)
            lastEventTime = event.timestamp

            when (event.type) {
                EventEntity.TYPE_WATER -> lastVolume += event.value
                EventEntity.TYPE_ALCOHOL -> { alcoholActive = true; alcoholUntil = event.timestamp + Constants.ALCOHOL_DURATION_MIN * 60_000 }
                EventEntity.TYPE_PEE -> lastVolume -= event.value
                EventEntity.TYPE_COLOR_SNAP -> lastVolume = HydrationMath.applyColorSnap(lastVolume, PeeColor.entries[event.value])
                EventEntity.TYPE_SLEEP -> bodyState = BodyState.SLEEP
                EventEntity.TYPE_WAKE -> bodyState = BodyState.AWAKE
                EventEntity.TYPE_SWEAT -> lastVolume += if (event.value == 0) Constants.SWEAT_PENALTY_LIGHT else Constants.SWEAT_PENALTY_HEAVY
            }
            lastVolume = lastVolume.coerceIn(0, Constants.SATURATION_CAP)
            points.add(ChartPoint(event.timestamp, lastVolume, isEvent = true))
        }

        val finalElapsed = (now - lastEventTime) / 60_000
        alcoholActive = alcoholActive && now < alcoholUntil
        val finalRate = computeRate()
        lastVolume = HydrationMath.calculateCurrentTank(lastVolume, 0, 0, max(0, finalElapsed), finalRate)
        points.add(ChartPoint(now, lastVolume))

        // Future projection: simulate until tank runs out or 24h from start, whichever comes first
        val timeToEmpty = if (finalRate > 0) (lastVolume / finalRate * 60 * 1000).toLong()
            else 6 * 60 * 60 * 1000L
        val projectionEnd = minOf(now + timeToEmpty, effectiveStart + 24 * 60 * 60 * 1000L)
        if (projectionEnd > now) simulate(projectionEnd)

        return points
    }

    suspend fun getDaySummary(dayStart: Long): DaySummary {
        val dayEnd = dayStart + 86_400_000
        val events = eventDao.getEventsForDaySync(dayStart, dayEnd)
        val hasData = events.isNotEmpty()
        if (!hasData) return DaySummary(dayStart, false)
        val chart = calculateChartData(null, false, 0L)
        val avg = if (chart.isNotEmpty()) chart.map { it.volume }.average().toInt() else 0
        val totalWater = events.filter { it.type == EventEntity.TYPE_WATER }.sumOf { it.value }
        val min = chart.minOfOrNull { it.volume } ?: 0
        val max = chart.maxOfOrNull { it.volume } ?: 0
        val dangerMin = chart.count { it.volume <= Constants.DANGER_FLOOR } * 30
        return DaySummary(dayStart, true, avg, totalWater, min, max, dangerMin)
    }

    suspend fun hasRecentSleepEvent(since: Long): Boolean {
        return eventDao.hasSleepEventInRange(since - 60_000, System.currentTimeMillis()) > 0
    }

    suspend fun addWaterAt(amount: Int, timestamp: Long) {
        eventDao.insertEvent(EventEntity(timestamp = timestamp, type = EventEntity.TYPE_WATER, value = amount))
    }

    suspend fun addSleepEventAt(timestamp: Long) {
        eventDao.insertEvent(
            EventEntity(
                timestamp = timestamp,
                type = EventEntity.TYPE_SLEEP,
                value = 0
            )
        )
    }

    suspend fun deleteEvent(id: Long) {
        eventDao.deleteEvent(id)
    }

    suspend fun updateEvent(id: Long, value: Int, timestamp: Long) {
        eventDao.updateEventValue(id, value)
        eventDao.updateEventTimestamp(id, timestamp)
    }

    suspend fun getDayEvents(dayStart: Long): List<EventEntity> {
        return eventDao.getEventsForDaySync(dayStart, dayStart + 86_400_000)
    }

    suspend fun getRecentEvents(limit: Int = 100): List<EventEntity> {
        return eventDao.getRecentEventsSync(limit)
    }

    suspend fun clearAll() {
        eventDao.clearAll()
    }
}
