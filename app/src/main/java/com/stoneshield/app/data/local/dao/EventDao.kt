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

    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    @Query("DELETE FROM events")
    suspend fun clearAll()
}
