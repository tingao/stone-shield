package com.stoneshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.stoneshield.app.data.local.UserPreferences
import com.stoneshield.app.ui.navigation.Routes
import com.stoneshield.app.ui.navigation.StoneShieldNavGraph
import com.stoneshield.app.ui.theme.StoneShieldTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var prefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoneShieldTheme {
                val onboardingDone by prefs.onboardingDone.collectAsState(initial = null)
                if (onboardingDone != null) {
                    val start = if (onboardingDone == true) Routes.DASHBOARD else Routes.ONBOARDING
                    StoneShieldNavGraph(startDestination = start)
                }
            }
        }
    }
}
