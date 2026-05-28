package com.stoneshield.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.stoneshield.app.data.local.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getEventsSince(since: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getEventsSinceSync(since: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp ASC")
    suspend fun getEventsForDaySync(startOfDay: Long, endOfDay: Long): List<EventEntity>

    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    @Query("UPDATE events SET value = :value WHERE id = :id")
    suspend fun updateEventValue(id: Long, value: Int)

    @Query("UPDATE events SET timestamp = :timestamp WHERE id = :id")
    suspend fun updateEventTimestamp(id: Long, timestamp: Long)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEvent(id: Long)

    @Query("DELETE FROM events")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM events WHERE type = 'sleep' AND timestamp >= :since AND timestamp <= :until")
    suspend fun hasSleepEventInRange(since: Long, until: Long): Int

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEventsSync(limit: Int): List<EventEntity>
}
