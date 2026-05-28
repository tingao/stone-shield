package com.stoneshield.app.data.repository

import com.stoneshield.app.data.local.dao.EventDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TankRepository @Inject constructor(
    private val eventDao: EventDao
) {
    fun getAllEvents() = eventDao.getAllEvents()
}
