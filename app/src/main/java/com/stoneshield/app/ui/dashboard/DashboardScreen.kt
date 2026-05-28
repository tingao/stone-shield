package com.stoneshield.app.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.stoneshield.app.data.repository.ChartPoint
import com.stoneshield.app.domain.Constants
import com.stoneshield.app.domain.PeeColor
import com.stoneshield.app.ui.theme.DarkDanger
import com.stoneshield.app.ui.theme.DarkSafe
import com.stoneshield.app.ui.theme.DarkWarn
import com.stoneshield.app.ui.theme.LightDanger
import com.stoneshield.app.ui.theme.LightSafe
import com.stoneshield.app.ui.theme.LightWarn

private val TEAL = Color(0xFF00897B)
private val WATER_BLUE = Color(0xFF0277BD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateSettings: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateCalendar: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = isSystemInDarkTheme()
    val safeC = if (isDark) DarkSafe else LightSafe
    val warnC = if (isDark) DarkWarn else LightWarn
    val dangerC = if (isDark) DarkDanger else LightDanger

    LaunchedEffect(uiState.message) {
        uiState.message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (uiState.showUsagePermission) {
        AlertDialog(onDismissRequest = { viewModel.dismissUsagePermission() }, title = { Text("Usage Access") },
            text = { Text("To detect sleep automatically.\nSettings → Special app access → Usage access → Stone Shield") },
            confirmButton = { TextButton({ context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); viewModel.dismissUsagePermission() }) { Text("Open Settings") } },
            dismissButton = { TextButton({ viewModel.dismissUsagePermission() }) { Text("Skip") } })
    }
    if (uiState.showNotificationPermission) {
        AlertDialog(onDismissRequest = { viewModel.dismissNotificationPermission() }, title = { Text("Notifications") },
            text = { Text("For dehydration warnings.\nSettings → Notifications → Stone Shield → Allow") },
            confirmButton = { TextButton({ context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${context.packageName}") }); viewModel.dismissNotificationPermission() }) { Text("Open") } },
            dismissButton = { TextButton({ viewModel.dismissNotificationPermission() }) { Text("Skip") } })
    }
    if (uiState.showExactAlarmPermission) {
        AlertDialog(onDismissRequest = { viewModel.dismissExactAlarmPermission() }, title = { Text("Exact Alarms") },
            text = { Text("For timely warnings even in Doze mode.\nSettings → Apps → Stone Shield → Allow exact alarms") },
            confirmButton = { TextButton({ context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${context.packageName}") }); viewModel.dismissExactAlarmPermission() }) { Text("Open") } },
            dismissButton = { TextButton({ viewModel.dismissExactAlarmPermission() }) { Text("Skip") } })
    }
    if (uiState.showBedtimeCheck) BedtimeCheckDialog({ viewModel.dismissBedtimeCheck() }) { viewModel.addSleep(it) }
    if (uiState.showMorningPrompt) MorningPromptDialog({ viewModel.dismissMorningPrompt() }) { viewModel.morningDrink() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(title = { Text("Stone Shield", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    IconButton(onClick = onNavigateCalendar) { Icon(Icons.Default.DateRange, "Calendar") }
                    IconButton(onClick = onNavigateHistory) { Icon(Icons.Default.History, "History") }
                    IconButton(onClick = onNavigateSettings) { Icon(Icons.Default.Settings, "Settings") }
                })
        }
    ) { padding ->
        if (uiState.isLoading || uiState.tankState == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val state = uiState.tankState!!
            val zoneColor = when {
                state.currentMl <= Constants.DANGER_FLOOR -> dangerC
                state.currentMl <= Constants.SAFE_FLOOR -> warnC
                else -> safeC
            }
            val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FA)

            Column(
                modifier = Modifier.fillMaxSize().background(bgColor).padding(padding).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.elevatedCardElevation(4.dp)
                ) {
                    if (uiState.hasEvents) {
                        HydrationChart(uiState.chartData, isDark)
                    } else {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            Text("No data yet — add water or log events", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                AnimatedTankGauge(if (uiState.hasEvents) state.currentMl else 800, zoneColor)
                Spacer(Modifier.height(4.dp))
                if (uiState.hasEvents) {
                    StatusText(state, zoneColor)
                    Spacer(Modifier.height(16.dp))
                    GradientTankBar(state.currentMl, Constants.SATURATION_CAP, Constants.SAFE_FLOOR, Constants.DANGER_FLOOR, isDark)
                } else {
                    Text("Log your first drink to start", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(24.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.elevatedCardElevation(4.dp)
                ) {
                    QuickActionButtons(
                        waterButtons = uiState.waterButtons,
                        onWater = { viewModel.addWater(it) },
                        onAlcohol = { viewModel.addAlcohol() },
                        onPeeLogged = { v, c -> viewModel.addPee(v, c) },
                        onSleep = { viewModel.showBedtimeCheck() }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HydrationChart(chartData: List<ChartPoint>, isDark: Boolean) {
    if (chartData.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text("Add events to see chart", color = Color.Gray)
        }
        return
    }
    val minTime = chartData.minOf { it.timestamp }
    val maxTime = chartData.maxOf { it.timestamp }
    val range = (maxTime - minTime).coerceAtLeast(1L)
    val lineColor = if (isDark) Color(0xFF4FC3F7) else Color(0xFF0277BD)
    val zoomState = rememberVicoZoomState(zoomEnabled = true)

    val model = remember(chartData.hashCode()) {
        val xs = chartData.map { ((it.timestamp - minTime).toFloat()) }
        val ys = chartData.map { it.volume.toFloat() }
        CartesianChartModel(LineCartesianLayerModel.build { series(x = xs, y = ys) })
    }

    Box(Modifier.fillMaxWidth().height(160.dp).padding(4.dp)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(0.5f).fillMaxWidth().background(
                if (isDark) DarkSafe.copy(alpha = 0.08f) else LightSafe.copy(alpha = 0.1f)))
            Box(Modifier.weight(0.25f).fillMaxWidth().background(
                if (isDark) DarkWarn.copy(alpha = 0.08f) else LightWarn.copy(alpha = 0.1f)))
            Box(Modifier.weight(0.25f).fillMaxWidth().background(
                if (isDark) DarkDanger.copy(alpha = 0.08f) else LightDanger.copy(alpha = 0.1f)))
        }
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(valueFormatter = { _, v, _ -> "${v.toInt()}" }),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = { _, v, _ ->
                    val ts = minTime + (v.toDouble() * range).toLong()
                    val c = java.util.Calendar.getInstance().apply { timeInMillis = ts }
                    "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
                })
            ),
            model = model,
            zoomState = zoomState,
            modifier = Modifier.fillMaxSize().padding(top = 4.dp)
        )
    }
}

@Composable
private fun AnimatedTankGauge(currentMl: Int, zoneColor: Color) {
    val a by animateFloatAsState(currentMl.toFloat(), tween(600), label = "g")
    val c by animateColorAsState(zoneColor, tween(600), label = "c")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${a.toInt()}",
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = c,
            textAlign = TextAlign.Center
        )
        Text("mL", fontSize = 20.sp, color = c.copy(alpha = 0.7f))
    }
}

@Composable
private fun StatusText(state: com.stoneshield.app.data.repository.TankState, zoneColor: Color) {
    val text = when {
        state.currentMl <= Constants.DANGER_FLOOR -> "Critical — drink now!"
        state.currentMl <= Constants.SAFE_FLOOR -> "Warning — hydrate soon"
        else -> "Hydrated"
    }
    Text(text, style = MaterialTheme.typography.titleMedium, color = zoneColor, fontWeight = FontWeight.Bold)
    if (state.alcoholActive) {
        Spacer(Modifier.height(2.dp))
        Text("Alcohol active — diuretic multiplier", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
    }
}

@Composable
private fun GradientTankBar(currentMl: Int, maxMl: Int, safeFloor: Int, dangerFloor: Int, isDark: Boolean) {
    val f = (currentMl.toFloat() / maxMl).coerceIn(0f, 1f)
    val d = if (isDark) DarkDanger else LightDanger
    val w = if (isDark) DarkWarn else LightWarn
    val g = if (isDark) DarkSafe else LightSafe
    val gradBg = Brush.horizontalGradient(listOf(d, w, g).map { it.copy(alpha = 0.15f) })
    val gradFill = Brush.horizontalGradient(listOf(d, w, g))
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(28.dp)) {
            Box(Modifier.fillMaxSize().background(gradBg, RoundedCornerShape(14.dp)))
            Box(Modifier.fillMaxWidth(f).height(28.dp).background(gradFill, RoundedCornerShape(14.dp)))
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("0", fontSize = 10.sp, color = Color.Gray)
            Spacer(Modifier.weight(1f)); Text("$safeFloor", fontSize = 10.sp, color = w)
            Spacer(Modifier.weight(1f)); Text("$maxMl", fontSize = 10.sp, color = g)
        }
    }
}

@Composable
private fun QuickActionButtons(
    waterButtons: List<Int>,
    onWater: (Int) -> Unit,
    onAlcohol: () -> Unit,
    onPeeLogged: (Int, PeeColor) -> Unit,
    onSleep: () -> Unit
) {
    var showPeeSheet by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Quick Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Water", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        val btns = waterButtons.take(5)
        val btnFraction = if (btns.isNotEmpty()) 1f / btns.size else 1f
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (amount in btns) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(btnFraction).clickable { onWater(amount) },
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.elevatedCardElevation(2.dp)
                ) {
                    Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WaterDrop, null, tint = WATER_BLUE, modifier = Modifier.size(22.dp))
                        Text("+$amount", fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard("Alcohol", Icons.Default.WineBar, Color(0xFFE65100), onAlcohol)
            ActionCard("Log Pee", Icons.Default.Wc, Color(0xFF7B1FA2)) { showPeeSheet = true }
            ActionCard("Sleep", Icons.Default.Bedtime, Color(0xFF2E7D32), onSleep)
        }
    }
    if (showPeeSheet) {
        PeeBottomSheet({ showPeeSheet = false }) { v, c -> onPeeLogged(v, c); showPeeSheet = false }
    }
}

@Composable
private fun ActionCard(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun BedtimeCheckDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var sweat by remember { mutableIntStateOf(0) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Bedtime Check") }, text = {
        Column {
            Text("Did you sweat significantly today?")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("No", sweat == 0) { sweat = 0 }
                Btn("Light", sweat == 1) { sweat = 1 }
                Btn("Heavy", sweat == 2) { sweat = 2 }
            }
        }
    }, confirmButton = { TextButton({ onConfirm(sweat) }) { Text("Go to Sleep") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
private fun Btn(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (selected) WATER_BLUE else Color.LightGray),
        shape = RoundedCornerShape(8.dp)) {
        Text(text, color = if (selected) Color.White else Color.Black)
    }
}

@Composable
private fun MorningPromptDialog(onDismiss: () -> Unit, onDrink: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Wake Up!") },
        text = { Text("Good morning! Start your day hydrated.\nDrink 500ml now.") },
        confirmButton = { TextButton(onDrink) { Text("Drink 500ml") } },
        dismissButton = { TextButton(onDismiss) { Text("Skip") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeeBottomSheet(onDismiss: () -> Unit, onPeeLogged: (Int, PeeColor) -> Unit) {
    var selectedColor by remember { mutableStateOf<PeeColor?>(null) }
    var selectedVolume by remember { mutableIntStateOf(200) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Log Pee", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Color", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PeeColor.entries.forEach { c ->
                    val n = when (c) {
                        PeeColor.DARK_ORANGE -> "Dark\nOrange"
                        PeeColor.YELLOW -> "Yellow"
                        PeeColor.LIGHT_YELLOW -> "Light\nYellow"
                        PeeColor.CLEAR -> "Clear"
                    }
                    Button({ selectedColor = c },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedColor == c) WATER_BLUE else Color.LightGray),
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.height(52.dp)) {
                        Text(n, fontSize = 10.sp, textAlign = TextAlign.Center,
                            color = if (selectedColor == c) Color.White else Color.Black)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Volume: ${selectedVolume}ml")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100, 200, 300, 400).forEach { vol ->
                    Button({ selectedVolume = vol },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedVolume == vol) WATER_BLUE else Color.LightGray),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("${vol}ml", color = if (selectedVolume == vol) Color.White else Color.Black)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button({ selectedColor?.let { onPeeLogged(selectedVolume, it) } },
                enabled = selectedColor != null,
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Log", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
