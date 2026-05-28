package com.stoneshield.app.ui.dashboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.stoneshield.app.domain.Constants
import com.stoneshield.app.domain.PeeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val tankState by viewModel.tankState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showPeeSheet by remember { mutableStateOf(false) }

    Scaffold { padding ->
        if (isLoading || tankState == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val state = tankState!!
            val bgColor = Color(viewModel.getBackgroundColor(state.currentMl))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor.copy(alpha = 0.1f))
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HydrationChart(currentMl = state.currentMl)

                Spacer(Modifier.height(16.dp))

                TankGauge(currentMl = state.currentMl, bgColor = bgColor)

                Spacer(Modifier.height(8.dp))

                Text(
                    text = when {
                        state.currentMl <= 200 -> "CRITICAL - Drink now!"
                        state.currentMl <= 400 -> "Warning - Hydrate soon"
                        else -> "Hydrated"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = bgColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(24.dp))

                QuickActionButtons(
                    onWater = { viewModel.addWater(it) },
                    onAlcohol = { viewModel.addAlcohol() },
                    onPee = { showPeeSheet = true }
                )

                Spacer(Modifier.height(16.dp))
            }

            if (showPeeSheet) {
                PeeBottomSheet(
                    onDismiss = { showPeeSheet = false },
                    onPeeLogged = { volume, color ->
                        viewModel.addPee(volume, color)
                        showPeeSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HydrationChart(currentMl: Int) {
    val modelProducer = remember { CartesianChartModelProducer() }

    androidx.compose.runtime.LaunchedEffect(currentMl) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    listOf(
                        (System.currentTimeMillis() - 86_400_000) to (currentMl * 0.7).toFloat(),
                        (System.currentTimeMillis() - 43_200_000) to (currentMl * 0.85).toFloat(),
                        System.currentTimeMillis() to currentMl.toFloat()
                    )
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis()
            ),
            modelProducer = modelProducer,
            modifier = Modifier.padding(8.dp)
        )
    }

    Spacer(Modifier.height(8.dp))
}

@Composable
private fun TankGauge(currentMl: Int, bgColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$currentMl",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = bgColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = "mL",
            fontSize = 20.sp,
            color = bgColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickActionButtons(
    onWater: (Int) -> Unit,
    onAlcohol: () -> Unit,
    onPee: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Water",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WaterButton("+300", 300, Color(0xFF1565C0)) { onWater(300) }
                WaterButton("+500", 500, Color(0xFF1976D2)) { onWater(500) }
                WaterButton("+700", 700, Color(0xFF2196F3)) { onWater(700) }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    label = "Alcohol",
                    color = Color(0xFFE65100),
                    onClick = onAlcohol
                )
                ActionButton(
                    label = "Log Pee",
                    color = Color(0xFF6A1B9A),
                    onClick = onPee
                )
            }
        }
    }
}

@Composable
private fun WaterButton(label: String, amount: Int, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(48.dp).width(96.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(48.dp).width(140.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeeBottomSheet(
    onDismiss: () -> Unit,
    onPeeLogged: (Int, PeeColor) -> Unit
) {
    var selectedColor by remember { mutableStateOf<PeeColor?>(null) }
    var selectedVolume by remember { mutableStateOf(200) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Log Pee",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Text("Color", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeeColor.entries.forEach { color ->
                    val colorName = when (color) {
                        PeeColor.DARK_ORANGE -> "Dark\nOrange"
                        PeeColor.YELLOW -> "Yellow"
                        PeeColor.LIGHT_YELLOW -> "Light\nYellow"
                        PeeColor.CLEAR -> "Clear"
                    }
                    Button(
                        onClick = { selectedColor = color },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedColor == color) Color(0xFF1565C0)
                            else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            text = colorName,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = if (selectedColor == color) Color.White else Color.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Volume: ${selectedVolume}ml", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100, 200, 300, 400).forEach { vol ->
                    Button(
                        onClick = { selectedVolume = vol },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedVolume == vol) Color(0xFF1565C0)
                            else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${vol}ml",
                            color = if (selectedVolume == vol) Color.White else Color.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    selectedColor?.let { onPeeLogged(selectedVolume, it) }
                },
                enabled = selectedColor != null,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
