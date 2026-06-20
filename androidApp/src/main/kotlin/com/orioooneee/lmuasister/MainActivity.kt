package com.orioooneee.lmuasister

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.orioooneee.lmuasister.analytics.initTelemetry
import com.orioooneee.lmuasister.data.steam.initSteamStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Wire Firebase Analytics + Crashlytics into the shared Telemetry facade.
        initTelemetry(applicationContext)

        // Wire the encrypted token store before any Steam login can run.
        initSteamStorage(applicationContext)

        // Transparent status + navigation bars with light (white) icons in both,
        // to match the dark app background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}