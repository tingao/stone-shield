package com.stoneshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stoneshield.app.ui.navigation.StoneShieldNavGraph
import com.stoneshield.app.ui.theme.StoneShieldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoneShieldTheme {
                StoneShieldNavGraph()
            }
        }
    }
}
