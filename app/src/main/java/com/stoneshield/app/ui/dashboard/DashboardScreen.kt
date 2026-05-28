package com.stoneshield.app.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.stoneshield.app.domain.Constants
import com.stoneshield.app.domain.PeeColor

private val GREEN = Color(0xFF43A047)
private val YELLOW = Color(0xFFFFA726)
private val RED = Color(0xFFD32F2F)
private val BLUE = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateSettings: () -> Unit,
    onNavigateHistory: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.showUsagePermission) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUsagePermission() },
            title = { Text("Usage Access Required") },
            text = { Text("Stone Shield needs Usage Access to detect when you sleep and calculate your hydration accurately.") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    viewModel.dismissUsagePermission()
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUsagePermission() }) { Text("Skip") }
            }
        )
    }

    if (uiState.showBedtimeCheck) {
        BedtimeCheckDialog(
            onDismiss = { viewModel.dismissBedtimeCheck() },
            onConfirm = { sweat -> viewModel.addSleep(sweat) }
        )
    }

    if (uiState.showMorningPrompt) {
        MorningPromptDialog(
            onDismiss = { viewModel.dismissMorningPrompt() },
            onDrink = { viewModel.morningDrink() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Stone Shield", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onNavigateHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading || uiState.tankState == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val state = uiState.tankState!!
            val zoneColor = when {
                state.currentMl <= Constants.DANGER_FLOOR -> RED
                state.currentMl <= Constants.SAFE_FLOOR -> YELLOW
                else -> GREEN
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(zoneColor.copy(alpha = 0.06f))
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                HydrationChart(currentMl = state.currentMl)

                Spacer(Modifier.height(12.dp))

                AnimatedTankGauge(currentMl = state.currentMl, zoneColor = zoneColor)

                Spacer(Modifier.height(4.dp))

                StatusText(state, zoneColor)

                Spacer(Modifier.height(12.dp))

                TankBar(
                    currentMl = state.currentMl,
                    maxMl = Constants.SATURATION_CAP,
                    safeFloor = Constants.SAFE_FLOOR,
                    dangerFloor = Constants.DANGER_FLOOR
                )

                Spacer(Modifier.height(16.dp))

                QuickActionButtons(
                    onWater = { viewModel.addWater(it) },
                    onAlcohol = { viewModel.addAlcohol() },
                    onPeeLogged = { vol, col -> viewModel.addPee(vol, col) },
                    onSleep = { viewModel.showBedtimeCheck() }
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HydrationChart(currentMl: Int) {
    val clamped = currentMl.toFloat().coerceIn(0f, 800f)
    val model = remember(currentMl) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(clamped * 0.7f, clamped * 0.85f, clamped)
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(160.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom()
            ),
            model = model,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun AnimatedTankGauge(currentMl: Int, zoneColor: Color) {
    val animatedValue by animateFloatAsState(
        targetValue = currentMl.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "gauge"
    )
    val animatedColor by animateColorAsState(
        targetValue = zoneColor,
        animationSpec = tween(durationMillis = 600),
        label = "gaugeColor"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${animatedValue.toInt()}",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = animatedColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = "mL",
            fontSize = 20.sp,
            color = animatedColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StatusText(state: com.stoneshield.app.data.repository.TankState, zoneColor: Color) {
    Text(
        text = when {
            state.currentMl <= Constants.DANGER_FLOOR -> "CRITICAL - Drink now!"
            state.currentMl <= Constants.SAFE_FLOOR -> "Warning - Hydrate soon"
            else -> "Hydrated"
        },
        style = MaterialTheme.typography.titleMedium,
        color = zoneColor,
        fontWeight = FontWeight.Bold
    )
    if (state.alcoholActive) {
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Alcohol active - diuretic multiplier",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE65100)
        )
    }
}

@Composable
private fun TankBar(currentMl: Int, maxMl: Int, safeFloor: Int, dangerFloor: Int) {
    val fraction = (currentMl.toFloat() / maxMl).coerceIn(0f, 1f)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RED.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(24.dp)
                    .background(
                        when {
                            currentMl <= dangerFloor -> RED
                            currentMl <= safeFloor -> YELLOW
                            else -> GREEN
                        },
                        RoundedCornerShape(12.dp)
                    )
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("0", fontSize = 10.sp, color = Color.Gray)
            Spacer(Modifier.weight(1f))
            Text("$safeFloor", fontSize = 10.sp, color = YELLOW)
            Spacer(Modifier.weight(1f))
            Text("$maxMl", fontSize = 10.sp, color = GREEN)
        }
    }
}

@Composable
private fun QuickActionButtons(
    onWater: (Int) -> Unit,
    onAlcohol: () -> Unit,
    onPeeLogged: (Int, PeeColor) -> Unit,
    onSleep: () -> Unit
) {
    var showPeeSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Quick Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Text("Water", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WaterBtn("+300", BLUE) { onWater(300) }
                WaterBtn("+500", Color(0xFF1976D2)) { onWater(500) }
                WaterBtn("+700", Color(0xFF2196F3)) { onWater(700) }
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionBtn("Alcohol", Color(0xFFE65100), onAlcohol)
                ActionBtn("Log Pee", Color(0xFF6A1B9A)) { showPeeSheet = true }
                ActionBtn("Sleep", Color(0xFF2E7D32), onSleep)
            }
        }
    }

    if (showPeeSheet) {
        PeeBottomSheet(
            onDismiss = { showPeeSheet = false },
            onPeeLogged = { vol, col ->
                onPeeLogged(vol, col)
                showPeeSheet = false
            }
        )
    }
}

@Composable
private fun WaterBtn(label: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.height(44.dp).width(88.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun ActionBtn(label: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.height(44.dp).width(100.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun BedtimeCheckDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var sweat by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bedtime Check") },
        text = {
            Column {
                Text("Did you sweat significantly today?")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { sweat = 0 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (sweat == 0) BLUE else Color.LightGray),
                        shape = RoundedCornerShape(8.dp)) { Text("No", color = if (sweat == 0) Color.White else Color.Black) }
                    Button(onClick = { sweat = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (sweat == 1) BLUE else Color.LightGray),
                        shape = RoundedCornerShape(8.dp)) { Text("Light", color = if (sweat == 1) Color.White else Color.Black) }
                    Button(onClick = { sweat = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (sweat == 2) BLUE else Color.LightGray),
                        shape = RoundedCornerShape(8.dp)) { Text("Heavy", color = if (sweat == 2) Color.White else Color.Black) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(sweat) }) { Text("Go to Sleep") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MorningPromptDialog(onDismiss: () -> Unit, onDrink: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("☀️ Wake Up & Flow!") },
        text = { Text("Good morning! Start your day hydrated.\nDrink 500ml now to replenish.") },
        confirmButton = { TextButton(onClick = onDrink) { Text("Drink 500ml") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Skip") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeeBottomSheet(onDismiss: () -> Unit, onPeeLogged: (Int, PeeColor) -> Unit) {
    var selectedColor by remember { mutableStateOf<PeeColor?>(null) }
    var selectedVolume by remember { mutableIntStateOf(200) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Log Pee", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Text("Color", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PeeColor.entries.forEach { color ->
                    val name = when (color) {
                        PeeColor.DARK_ORANGE -> "Dark\nOrange"
                        PeeColor.YELLOW -> "Yellow"
                        PeeColor.LIGHT_YELLOW -> "Light\nYellow"
                        PeeColor.CLEAR -> "Clear"
                    }
                    Button(
                        onClick = { selectedColor = color },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedColor == color) BLUE else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.height(52.dp)) {
                        Text(name, fontSize = 10.sp, textAlign = TextAlign.Center,
                            color = if (selectedColor == color) Color.White else Color.Black)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Volume: ${selectedVolume}ml")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100, 200, 300, 400).forEach { vol ->
                    Button(
                        onClick = { selectedVolume = vol },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedVolume == vol) BLUE else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("${vol}ml", color = if (selectedVolume == vol) Color.White else Color.Black)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(onClick = { selectedColor?.let { onPeeLogged(selectedVolume, it) } },
                enabled = selectedColor != null,
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Log", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
