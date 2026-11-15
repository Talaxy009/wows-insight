package com.talaxy.wowsinsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.talaxy.wowsinsight.ui.navigation.WoWsInsightApp
import com.talaxy.wowsinsight.ui.theme.WoWsInsightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as WoWsInsightApplication).container
        setContent {
            WoWsInsightTheme {
                WoWsInsightApp(container = container)
            }
        }
    }
}
