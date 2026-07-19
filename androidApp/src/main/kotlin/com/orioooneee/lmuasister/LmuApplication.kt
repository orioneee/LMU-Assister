package com.orioooneee.lmuasister

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.aheaditec.talsec_security.security.api.SuspiciousAppInfo
import com.aheaditec.talsec_security.security.api.Talsec
import com.aheaditec.talsec_security.security.api.TalsecConfig
import com.aheaditec.talsec_security.security.api.TalsecMode
import com.aheaditec.talsec_security.security.api.ThreatListener
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import com.orioooneee.lmuasister.analytics.initTelemetry
import com.orioooneee.lmuasister.security.SecurityGate
import kotlin.time.Duration.Companion.seconds

class LmuApplication: Application() {
    private val securityGateHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        SecurityShutdown.install(this)
        createNotificationChannels()

        Firebase.initialize(this)
        initTelemetry(applicationContext)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            },
        )

        startRuntimeProtection()
    }

    private fun startRuntimeProtection() {
        SecurityGate.resetForChecks()

        val config = TalsecConfig.Builder(
            EXPECTED_PACKAGE_NAME,
            EXPECTED_SIGNING_CERTIFICATE_HASHES_BASE64,
        )
            .supportedAlternativeStores(SUPPORTED_ALTERNATIVE_STORES)
            .blacklistedPackageNames(NO_BLACKLISTED_PACKAGES)
            .blacklistedHashes(NO_BLACKLISTED_HASHES)
            .suspiciousPermissions(NO_SUSPICIOUS_PERMISSIONS)
            .prod(!BuildConfig.DEBUG)
            .killOnBypass(true)
            .build()

        val threatListener = object : ThreatListener.ThreatDetected() {
            override fun onRootDetected() = closeForSecurity()
            override fun onHookDetected() = closeForSecurity()
            override fun onTamperDetected() = closeForSecurity()

            override fun onDebuggerDetected() {
                if (!BuildConfig.DEBUG) closeForSecurity()
            }

            override fun onMalwareDetected(suspiciousApps: List<SuspiciousAppInfo>) = Unit

            override fun onObfuscationIssuesDetected() {
                if (!BuildConfig.DEBUG) closeForSecurity()
            }
        }

        runCatching {
            ThreatListener(threatListener).registerListener(this)
            Talsec.start(this, config, TalsecMode.BACKGROUND)
            securityGateHandler.postDelayed(
                { SecurityGate.allow() },
                RASP_STARTUP_GRACE_MS.inWholeMilliseconds,
            )
        }.onFailure {
            if (BuildConfig.DEBUG) {
                SecurityGate.allow()
            } else {
                closeForSecurity()
            }
        }
    }

    private fun closeForSecurity() {
        SecurityShutdown.close(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                getString(R.string.schedule_notification_channel_id),
                getString(R.string.schedule_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableVibration(true)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                getString(R.string.schedule_updated_channel_id),
                getString(R.string.schedule_updated_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableVibration(true)
            },
        )
    }

    private companion object {
        private val RASP_STARTUP_GRACE_MS = 1.5.seconds
        private const val EXPECTED_PACKAGE_NAME = "com.orioooneee.lmuasister"
        private val EXPECTED_SIGNING_CERTIFICATE_HASHES_BASE64 = arrayOf(
            "BMkxr7/lYtlnEfWhizdSYVk6S7uuPzJLhMbPVGPDwiQ=",
            "OREZ2ytaIhLxhvlOt+VtwyLrkYdiW/Tcc3snGf3GIDY=",
        )
        private val SUPPORTED_ALTERNATIVE_STORES = emptyArray<String>()
        private val NO_BLACKLISTED_PACKAGES = emptyArray<String>()
        private val NO_BLACKLISTED_HASHES = emptyArray<String>()
        private val NO_SUSPICIOUS_PERMISSIONS = emptyArray<Array<String>>()
    }
}
