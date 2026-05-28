package com.stoneshield.app.ui.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stoneshield.app.data.local.EventEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    var editEvent by remember { mutableStateOf<EventEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No events yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    SwipeableEventCard(
                        event = event,
                        onDelete = { viewModel.deleteEvent(event.id) },
                        onEdit = { editEvent = event }
                    )
                }
            }
        }
    }

    editEvent?.let { event ->
        EditEventDialog(
            event = event,
            onDismiss = { editEvent = null },
            onSave = { newValue, newTime ->
                viewModel.updateEvent(event.id, newValue, newTime)
                editEvent = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEventCard(event: EventEntity, onDelete: () -> Unit, onEdit: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                SwipeToDismissBoxValue.StartToEnd -> { onEdit(); false }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isDelete = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val color by animateColorAsState(
                targetValue = if (isDelete) Color(0xFFD32F2F) else Color(0xFF1565C0),
                label = "bg"
            )
            Box(Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart) {
                Text(if (isDelete) "Delete" else "Edit", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    ) {
        EventCard(event)
    }
}

@Composable
private fun EventCard(event: EventEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()) }
    val (icon, _) = when (event.type) {
        EventEntity.TYPE_WATER -> "💧" to Color(0xFF1565C0)
        EventEntity.TYPE_ALCOHOL -> "🍺" to Color(0xFFE65100)
        EventEntity.TYPE_PEE -> "🚽" to Color(0xFF6A1B9A)
        EventEntity.TYPE_SLEEP -> "🌙" to Color(0xFF2E7D32)
        EventEntity.TYPE_WAKE -> "☀️" to Color(0xFFFFA726)
        EventEntity.TYPE_SWEAT -> "💦" to Color(0xFF795548)
        else -> "📝" to Color.Gray
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = MaterialTheme.typography.titleLarge.fontSize)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val label = when (event.type) {
                    EventEntity.TYPE_WATER -> "Water +${event.value}ml"
                    EventEntity.TYPE_ALCOHOL -> "Alcohol (120min diuretic)"
                    EventEntity.TYPE_PEE -> "Pee -${event.value}ml"
                    EventEntity.TYPE_SLEEP -> "Sleep"
                    EventEntity.TYPE_WAKE -> "Wake up"
                    EventEntity.TYPE_SWEAT -> "Sweat ${if (event.value == 0) "light" else "heavy"}"
                    EventEntity.TYPE_COLOR_SNAP -> "Color snap"
                    else -> event.type
                }
                Text(label, fontWeight = FontWeight.Medium)
                if (event.note != null) {
                    Text(event.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Text(dateFormat.format(Date(event.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEventDialog(event: EventEntity, onDismiss: () -> Unit, onSave: (Int, Long) -> Unit) {
    var newValue by remember { mutableStateOf(event.value.toString()) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    var newTime by remember { mutableStateOf(sdf.format(Date(event.timestamp))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column {
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Value (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Time: ${sdf.format(Date(event.timestamp))}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton({
                val v = newValue.toIntOrNull()
                if (v != null) onSave(v, event.timestamp)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}
