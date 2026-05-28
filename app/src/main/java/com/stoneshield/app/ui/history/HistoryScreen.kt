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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.remember
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
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No events yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    SwipeableEventCard(event, onDelete = { viewModel.deleteEvent(event.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEventCard(event: EventEntity, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFD32F2F)
                    else -> Color.Transparent
                }, label = "bg"
            )
            Box(modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    ) {
        EventCard(event)
    }
}

@Composable
private fun EventCard(event: EventEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()) }
    val (icon, color) = when (event.type) {
        EventEntity.TYPE_WATER -> "💧" to Color(0xFF1565C0)
        EventEntity.TYPE_ALCOHOL -> "🍺" to Color(0xFFE65100)
        EventEntity.TYPE_PEE -> "🚽" to Color(0xFF6A1B9A)
        EventEntity.TYPE_SLEEP -> "🌙" to Color(0xFF2E7D32)
        EventEntity.TYPE_WAKE -> "☀️" to Color(0xFFFFA726)
        EventEntity.TYPE_SWEAT -> "💦" to Color(0xFF795548)
        else -> "📝" to Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = MaterialTheme.typography.titleLarge.fontSize)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (event.type) {
                        EventEntity.TYPE_WATER -> "Water +${event.value}ml"
                        EventEntity.TYPE_ALCOHOL -> "Alcohol (120min diuretic)"
                        EventEntity.TYPE_PEE -> "Pee -${event.value}ml"
                        EventEntity.TYPE_SLEEP -> "Sleep"
                        EventEntity.TYPE_WAKE -> "Wake up"
                        EventEntity.TYPE_SWEAT -> "Sweat ${if (event.value == 0) "light" else "heavy"}"
                        EventEntity.TYPE_COLOR_SNAP -> "Color snap"
                        else -> event.type
                    },
                    fontWeight = FontWeight.Medium
                )
                if (event.note != null) {
                    Text(event.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Text(
                text = dateFormat.format(Date(event.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}


