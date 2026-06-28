package com.orioooneee.lmuasister.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.HttpMetric

fun initTelemetry(context: Context) {
    Telemetry.analytics = FirebaseAnalyticsSink(context.applicationContext)
    Telemetry.crashReporter = FirebaseCrashSink()
    Telemetry.performanceMonitor = FirebasePerformanceSink()
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

private class FirebasePerformanceSink : PerformanceMonitor {
    override fun startHttpMetric(url: String, method: String): HttpPerformanceMetric {
        return runCatching {
            val metric = FirebasePerformance.getInstance().newHttpMetric(url, method)
            metric.start()
            FirebaseHttpPerformanceMetric(metric)
        }.getOrElse {
            NoopPerformanceMonitor.startHttpMetric(url, method)
        }
    }
}

private class FirebaseHttpPerformanceMetric(private val metric: HttpMetric) : HttpPerformanceMetric {
    override fun setHttpResponseCode(code: Int) {
        metric.setHttpResponseCode(code)
    }

    override fun setResponseContentType(contentType: String?) {
        if (!contentType.isNullOrBlank()) metric.setResponseContentType(contentType)
    }

    override fun stop() {
        metric.stop()
    }
}

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
