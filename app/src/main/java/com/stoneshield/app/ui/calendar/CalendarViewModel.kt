package com.stoneshield.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoneshield.app.data.repository.DaySummary
import com.stoneshield.app.data.repository.TankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TankRepository
) : ViewModel() {
    private val _days = MutableStateFlow<List<DaySummary>>(emptyList())
    val days: StateFlow<List<DaySummary>> = _days.asStateFlow()

    private val _selectedDay = MutableStateFlow<DaySummary?>(null)
    val selectedDay: StateFlow<DaySummary?> = _selectedDay.asStateFlow()

    init { loadMonth() }

    private fun loadMonth() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val results = mutableListOf<DaySummary>()
            for (d in 0 until daysInMonth) {
                results.add(repository.getDaySummary(monthStart + d * 86_400_000L))
            }
            _days.value = results
        }
    }

    fun selectDay(dateStart: Long) {
        viewModelScope.launch {
            _selectedDay.value = repository.getDaySummary(dateStart)
        }
    }
}
