package com.stoneshield.app.domain

object Constants {
    const val BASE_DECAY_AWAKE = 0.8
    const val BASE_DECAY_SLEEP = 0.5
    const val SWEAT_PENALTY_LIGHT = -200
    const val SWEAT_PENALTY_HEAVY = -400
    const val TEMP_THRESHOLD = 21.0
    const val TEMP_FACTOR_PER_C = 0.05
    const val ALCOHOL_MULTIPLIER = 1.5
    const val ALCOHOL_DURATION_MIN = 120
    const val SATURATION_CAP = 800
    const val SAFE_FLOOR = 400
    const val DANGER_FLOOR = 200
    const val NIGHT_SURVIVAL_MIN = 650
    const val DARK_ORANGE_FORCE = 0
    const val YELLOW_FORCE_CAP = 300
    const val CLEAR_FORCE = 800
    const val NIGHT_DETECT_MIN_HOURS = 4
    const val NIGHT_LOOKBACK_HOURS = 12
    const val CHARGE_HEAT_HOURS = 1
    const val ROOM_TEMP = 21.0
}

enum class PeeColor { DARK_ORANGE, YELLOW, LIGHT_YELLOW, CLEAR }
enum class SweatLevel { NONE, LIGHT, HEAVY }
enum class BodyState { AWAKE, SLEEP }
