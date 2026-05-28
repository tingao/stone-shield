package com.stoneshield.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val value: Int,
    val note: String? = null
) {
    companion object {
        const val TYPE_WATER = "water"
        const val TYPE_ALCOHOL = "alcohol"
        const val TYPE_PEE = "pee"
        const val TYPE_SLEEP = "sleep"
        const val TYPE_WAKE = "wake"
        const val TYPE_SWEAT = "sweat"
        const val TYPE_COLOR_SNAP = "color_snap"
    }
}
