package com.orioooneee.lmuasister

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.orioooneee.lmuasister.analytics.AnalyticsEvent
import com.orioooneee.lmuasister.analytics.Telemetry
import com.orioooneee.lmuasister.data.steam.initSteamStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        initSteamStorage(applicationContext)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        logNotificationOpen(intent)

        setContent {
            App(startupEffects = { FcmTokenStartupEffect() })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        logNotificationOpen(intent)
    }

    private fun logNotificationOpen(intent: Intent?) {
        val targetIntent = intent ?: return
        val notificationType = targetIntent.getStringExtra(EXTRA_NOTIFICATION_TYPE)
            ?: targetIntent.getStringExtra(FCM_DATA_TYPE)
            ?: return
        val notificationId = targetIntent.getIntExtra(EXTRA_NOTIFICATION_ID, INTENT_ID_MISSING)
            .takeUnless { it == INTENT_ID_MISSING }
            ?: targetIntent.getStringExtra(FCM_NOTIFICATION_ID)?.toIntOrNull()
        Telemetry.log(AnalyticsEvent.PushNotificationOpened(notificationType, notificationId))
        targetIntent.removeExtra(EXTRA_NOTIFICATION_TYPE)
        targetIntent.removeExtra(EXTRA_NOTIFICATION_ID)
        targetIntent.removeExtra(FCM_DATA_TYPE)
        targetIntent.removeExtra(FCM_NOTIFICATION_ID)
    }

    private companion object {
        private const val FCM_DATA_TYPE = "type"
        private const val FCM_NOTIFICATION_ID = "notification_id"
        private const val INTENT_ID_MISSING = Int.MIN_VALUE
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
