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

    suspend fun clearAll() {
        eventDao.clearAll()
    }
}
