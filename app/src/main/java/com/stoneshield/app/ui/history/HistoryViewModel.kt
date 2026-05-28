package com.stoneshield.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoneshield.app.data.local.EventEntity
import com.stoneshield.app.data.repository.TankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TankRepository
) : ViewModel() {
    private val _events = MutableStateFlow<List<EventEntity>>(emptyList())
    val events: StateFlow<List<EventEntity>> = _events.asStateFlow()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            _events.value = repository.getRecentEvents(200)
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteEvent(id)
            loadEvents()
        }
    }

    fun updateEvent(id: Long, value: Int, timestamp: Long) {
        viewModelScope.launch {
            repository.updateEvent(id, value, timestamp)
            loadEvents()
        }
    }
}
