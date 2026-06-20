package com.orioooneee.lmuasister.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Wires the global [Telemetry] facade to Firebase. Call once, early — from the
 * Android Application / Activity onCreate. Firebase itself auto-initializes from
 * google-services.json (via its ContentProvider), so this only swaps in the real
 * Analytics + Crashlytics sinks and sets the platform user-property.
 */
fun initTelemetry(context: Context) {
    Telemetry.analytics = FirebaseAnalyticsSink(context.applicationContext)
    Telemetry.crashReporter = FirebaseCrashSink()
    Telemetry.userProperty(UserProperties.PLATFORM, "android")
}

private class FirebaseAnalyticsSink(context: Context) : Analytics {
    private val fa = FirebaseAnalytics.getInstance(context)

    override fun logEvent(event: AnalyticsEvent) {
        fa.logEvent(event.name, event.params.toBundle())
    }

    override fun logScreenView(screenName: String) {
        fa.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName) },
        )
    }

    override fun setUserId(id: String?) = fa.setUserId(id)

    override fun setUserProperty(key: String, value: String?) = fa.setUserProperty(key, value)
}

private class FirebaseCrashSink : CrashReporter {
    private val crash = FirebaseCrashlytics.getInstance()

    override fun recordException(throwable: Throwable, keys: Map<String, Any?>) {
        keys.forEach { (k, v) -> setCustomKey(k, v) }
        crash.recordException(throwable)
    }

    override fun log(message: String) = crash.log(message)

    override fun setCustomKey(key: String, value: Any?) {
        when (value) {
            null -> crash.setCustomKey(key, "")
            is String -> crash.setCustomKey(key, value)
            is Boolean -> crash.setCustomKey(key, value)
            is Int -> crash.setCustomKey(key, value)
            is Long -> crash.setCustomKey(key, value)
            is Double -> crash.setCustomKey(key, value)
            is Float -> crash.setCustomKey(key, value)
            else -> crash.setCustomKey(key, value.toString())
        }
    }

    override fun setUserId(id: String?) = crash.setUserId(id ?: "")
}

/** Firebase only accepts String/Long/Double params, so booleans become "true"/"false". */
private fun Map<String, Any?>.toBundle(): Bundle = Bundle().apply {
    forEach { (k, v) ->
        when (v) {
            null -> {}
            is String -> putString(k, v)
            is Boolean -> putString(k, v.toString())
            is Int -> putLong(k, v.toLong())
            is Long -> putLong(k, v)
            is Double -> putDouble(k, v)
            is Float -> putDouble(k, v.toDouble())
            else -> putString(k, v.toString())
        }
    }
}
