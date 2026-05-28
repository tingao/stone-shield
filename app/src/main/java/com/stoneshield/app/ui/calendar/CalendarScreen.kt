package com.stoneshield.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoneshield.app.data.repository.DaySummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class GridDay(val dayNum: Int, val dateStart: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val days by viewModel.days.collectAsState()
    val selected by viewModel.selectedDay.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            val cal = Calendar.getInstance()
            val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            Text("${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEach {
                    Text(it, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(8.dp))

            val gridDays = remember { buildMonthGrid() }
            LazyVerticalGrid(columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(gridDays, key = { idx, _ -> idx }) { _, gd ->
                    val summary = days.find { it.date == gd.dateStart }
                    val bg = when {
                        summary == null -> Color.Transparent
                        summary.dangerMinutes > 120 -> Color(0xFFEF9A9A)
                        summary.dangerMinutes > 30 -> Color(0xFFFFE082)
                        summary.totalWater > 0 -> Color(0xFFA5D6A7)
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(bg).then(
                            if (summary != null) Modifier.clickable { viewModel.selectDay(gd.dateStart) } else Modifier
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (gd.dayNum > 0) "${gd.dayNum}" else "", fontSize = 13.sp)
                    }
                }
            }

            selected?.let { day ->
                Spacer(Modifier.height(16.dp))
                DayDetailCard(day)
            }
        }
    }
}

@Composable
private fun DayDetailCard(day: DaySummary) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(sdf.format(Date(day.date)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            DetailRow("Average tank", "${day.avgMl} mL")
            DetailRow("Total water", "${day.totalWater} mL")
            DetailRow("Min / Max", "${day.minMl} / ${day.maxMl} mL")
            DetailRow("Time in danger", "${day.dangerMinutes} min")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun buildMonthGrid(): List<GridDay> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cal2 = Calendar.getInstance()
    val items = mutableListOf<GridDay>()
    repeat(offset) { items.add(GridDay(0, 0L)) }
    for (d in 1..daysInMonth) {
        cal2.set(Calendar.DAY_OF_MONTH, d)
        cal2.set(Calendar.HOUR_OF_DAY, 0); cal2.set(Calendar.MINUTE, 0)
        cal2.set(Calendar.SECOND, 0); cal2.set(Calendar.MILLISECOND, 0)
        items.add(GridDay(d, cal2.timeInMillis))
    }
    return items
}
