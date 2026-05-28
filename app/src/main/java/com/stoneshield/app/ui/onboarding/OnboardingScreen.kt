package com.stoneshield.app.ui.onboarding

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

private val BLUE = Color(0xFF1565C0)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(0) }

    val pages = listOf(
        Triple("💧", "Track Your Hydration",
            "Stone Shield calculates your body water level in real-time. Log water, food, and pee to stay in the safe zone."),
        Triple("🎯", "Stay in the Green",
            "Your tank fills when you drink and drains over time. Stay above 400ml (Green) to avoid dehydration risk."),
        Triple("🌙", "Sleep Detection",
            "The app detects your sleep automatically using phone usage stats. Wake up to a morning hydration prompt."),
        Triple("⏰", "Smart Alarms",
            "Notifications warn you before hitting critical levels. The app respects Do Not Disturb during nighttime.")
    )

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val page = pages[step]

        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(BLUE.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.first, fontSize = 48.sp)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = page.second,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.third,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(48.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == step) 24.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (i == step) BLUE else Color.LightGray)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (step < pages.size - 1) step++
                else {
                    viewModel.completeOnboarding()
                    onComplete()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BLUE)
        ) {
            Text(
                if (step < pages.size - 1) "Next" else "Start",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
