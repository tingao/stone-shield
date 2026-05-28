package com.stoneshield.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stoneshield.app.data.local.dao.EventDao

@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
