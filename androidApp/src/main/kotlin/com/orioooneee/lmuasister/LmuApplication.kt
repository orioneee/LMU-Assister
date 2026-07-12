package com.orioooneee.lmuasister

import android.app.Application
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
import com.orioooneee.lmuasister.security.SecurityGate

class LmuApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        SecurityShutdown.install(this)
        startRuntimeProtection()

        Firebase.initialize(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            },
        )
    }

    private fun startRuntimeProtection() {
        SecurityGate.resetForChecks()

        val config = TalsecConfig.Builder(
            EXPECTED_PACKAGE_NAME,
            EXPECTED_SIGNING_CERTIFICATE_HASHES_BASE64,
        )
            .supportedAlternativeStores(SUPPORTED_ALTERNATIVE_STORES)
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

            override fun onMalwareDetected(suspiciousApps: List<SuspiciousAppInfo>) {
                if (suspiciousApps.isNotEmpty()) closeForSecurity()
            }

            override fun onObfuscationIssuesDetected() {
                if (!BuildConfig.DEBUG) closeForSecurity()
            }
        }

        val executionListener = object : ThreatListener.RaspExecutionState() {
            override fun onAllChecksFinished() {
                SecurityGate.allow()
            }
        }

        runCatching {
            ThreatListener(threatListener, null, executionListener).registerListener(this)
            Talsec.start(this, config, TalsecMode.BACKGROUND)
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

    private companion object {
        private const val EXPECTED_PACKAGE_NAME = "com.orioooneee.lmuasister"
        private val EXPECTED_SIGNING_CERTIFICATE_HASHES_BASE64 = arrayOf(
            "BMkxr7/lYtlnEfWhizdSYVk6S7uuPzJLhMbPVGPDwiQ=",
            "OREZ2ytaIhLxhvlOt+VtwyLrkYdiW/Tcc3snGf3GIDY=",
        )
        private val SUPPORTED_ALTERNATIVE_STORES = emptyArray<String>()
    }
}
