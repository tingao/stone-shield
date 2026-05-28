package com.stoneshield.app.domain

import kotlin.math.max
import kotlin.math.min

object HydrationMath {

    fun calculateEffectiveRate(
        state: BodyState,
        temperatureCelsius: Double?,
        alcoholActive: Boolean,
        isPluggedIn: Boolean,
        chargeTimeMinutes: Long
    ): Double {
        val baseRate = when (state) {
            BodyState.AWAKE -> Constants.BASE_DECAY_AWAKE
            BodyState.SLEEP -> Constants.BASE_DECAY_SLEEP
        }

        val effectiveTemp = if (isPluggedIn && chargeTimeMinutes > 60) {
            Constants.ROOM_TEMP
        } else {
            temperatureCelsius ?: Constants.ROOM_TEMP
        }

        val tempAdjustment = if (effectiveTemp > Constants.TEMP_THRESHOLD) {
            (effectiveTemp - Constants.TEMP_THRESHOLD) * Constants.TEMP_FACTOR_PER_C
        } else {
            0.0
        }

        val tempRate = baseRate + tempAdjustment

        return if (alcoholActive) {
            tempRate * Constants.ALCOHOL_MULTIPLIER
        } else {
            tempRate
        }
    }

    fun calculateCurrentTank(
        lastVolume: Int,
        inputs: Int,
        outputs: Int,
        elapsedMinutes: Long,
        effectiveRate: Double
    ): Int {
        val volume = lastVolume + inputs - outputs - (elapsedMinutes * effectiveRate).toInt()
        return volume.coerceIn(0, Constants.SATURATION_CAP)
    }

    fun applyColorSnap(currentVolume: Int, color: PeeColor): Int {
        return when (color) {
            PeeColor.DARK_ORANGE -> Constants.DARK_ORANGE_FORCE
            PeeColor.YELLOW -> min(currentVolume, Constants.YELLOW_FORCE_CAP)
            PeeColor.LIGHT_YELLOW -> currentVolume
            PeeColor.CLEAR -> Constants.CLEAR_FORCE
        }
    }

    fun minutesUntilThreshold(
        currentVolume: Int,
        threshold: Int,
        effectiveRate: Double
    ): Long {
        if (currentVolume <= threshold) return 0L
        if (effectiveRate <= 0.0) return Long.MAX_VALUE
        val deficit = currentVolume - threshold
        return (deficit / effectiveRate).toLong()
    }

    fun calculateNightSurvivalTarget(
        sleepDurationHours: Int,
        effectiveRate: Double
    ): Int {
        val loss = (sleepDurationHours * 60 * effectiveRate).toInt()
        return max(Constants.NIGHT_SURVIVAL_MIN, loss + Constants.DANGER_FLOOR)
    }

    fun estimateVolumeAfterSleep(
        sleepVolume: Int,
        sleepMinutes: Long,
        effectiveRate: Double
    ): Int {
        val loss = (sleepMinutes * effectiveRate).toInt()
        return max(0, sleepVolume - loss)
    }
}
