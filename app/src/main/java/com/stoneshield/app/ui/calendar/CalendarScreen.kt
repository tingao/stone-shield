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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoneshield.app.data.local.EventEntity
import com.stoneshield.app.ui.theme.LightBlue
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
    val selectedDay by viewModel.selectedDay.collectAsState()
    val dayEvents by viewModel.dayEvents.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

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
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            val cal = Calendar.getInstance()
            val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            Text("${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEach {
                    Text(it, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(8.dp))

            val gridDays = remember { buildMonthGrid() }
            LazyVerticalGrid(columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().height(280.dp)) {
                itemsIndexed(gridDays, key = { idx, _ -> idx }) { _, gd ->
                    val summary = days.find { it.date == gd.dateStart }
                    val hasData = summary != null && summary.hasData
                    val bg = when {
                        !hasData || gd.dayNum == 0 -> Color.Transparent
                        summary!!.dangerMinutes > 120 -> Color(0xFFEF9A9A)
                        summary.dangerMinutes > 30 -> Color(0xFFFFE082)
                        summary.totalWater > 0 -> Color(0xFFA5D6A7)
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(bg).then(
                            if (hasData) Modifier.clickable { viewModel.selectDay(gd.dateStart) }
                            else if (gd.dayNum > 0) Modifier.clickable { viewModel.selectDay(gd.dateStart); showAddDialog = true }
                            else Modifier
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (gd.dayNum > 0) {
                            Text("${gd.dayNum}", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Day events list
            if (selectedDay != null && dayEvents.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Events", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    items(dayEvents, key = { it.id }) { event ->
                        DayEventRow(event)
                    }
                }
            } else if (selectedDay != null) {
                Spacer(Modifier.height(12.dp))
                Text("No events this day", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }

            // Add event dialog
            if (showAddDialog && selectedDay != null) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("Quick Add") },
                    text = { Text("Add event for this day?") },
                    confirmButton = {
                        TextButton({
                            viewModel.addWaterToDay(selectedDay!!)
                            showAddDialog = false
                        }) {
                            Icon(Icons.Default.WaterDrop, null, tint = LightBlue, modifier = Modifier.size(18.dp))
                            Text(" +500ml water", color = LightBlue)
                        }
                    },
                    dismissButton = { TextButton({ showAddDialog = false }) { Text("Cancel") } }
                )
            }
        }
    }
}

@Composable
private fun DayEventRow(event: EventEntity) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val label = when (event.type) {
        EventEntity.TYPE_WATER -> "💧 Water +${event.value}ml"
        EventEntity.TYPE_ALCOHOL -> "🍺 Alcohol"
        EventEntity.TYPE_PEE -> "🚽 Pee -${event.value}ml"
        EventEntity.TYPE_SLEEP -> "🌙 Sleep"
        EventEntity.TYPE_WAKE -> "☀️ Wake"
        EventEntity.TYPE_SWEAT -> "💦 Sweat"
        else -> "📝 ${event.type}"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(sdf.format(Date(event.timestamp)), fontSize = 12.sp, color = Color.Gray)
    }
}

private fun buildMonthGrid(): List<GridDay> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val offset = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
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
