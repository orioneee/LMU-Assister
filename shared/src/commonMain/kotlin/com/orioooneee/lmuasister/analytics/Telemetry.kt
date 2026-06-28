package com.orioooneee.lmuasister.analytics

/**
 * Platform-agnostic analytics sink. Implemented per platform:
 *   - Android: Firebase Analytics (see androidMain/FirebaseTelemetry.kt)
 *   - iOS:     Firebase Analytics via a Swift bridge that conforms to this protocol
 *   - Desktop: no-op
 */
interface Analytics {
    fun logEvent(event: AnalyticsEvent)
    fun logScreenView(screenName: String)
    fun setUserId(id: String?)
    fun setUserProperty(key: String, value: String?)
}

/**
 * Platform-agnostic crash / non-fatal reporter. On Android/iOS this is Firebase
 * Crashlytics; on desktop it's a no-op. Use it for "silent" failures (empty screens,
 * failed network calls) that never crash the app but still lose users.
 */
interface CrashReporter {
    fun recordException(throwable: Throwable, keys: Map<String, Any?> = emptyMap())

    /** Breadcrumb log — attached to the next crash/non-fatal report. */
    fun log(message: String)

    /** Sticky key shown on every subsequent report (e.g. current_screen, is_logged_in). */
    fun setCustomKey(key: String, value: Any?)

    fun setUserId(id: String?)
}

/**
 * Platform-agnostic Firebase Performance facade. Android and iOS emit real HTTP
 * metrics; desktop keeps the same instrumentation calls as no-ops.
 */
interface PerformanceMonitor {
    fun startHttpMetric(url: String, method: String): HttpPerformanceMetric
}

interface HttpPerformanceMetric {
    fun setHttpResponseCode(code: Int)
    fun setResponseContentType(contentType: String?)
    fun stop()
}

internal object NoopAnalytics : Analytics {
    override fun logEvent(event: AnalyticsEvent) {}
    override fun logScreenView(screenName: String) {}
    override fun setUserId(id: String?) {}
    override fun setUserProperty(key: String, value: String?) {}
}

internal object NoopCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable, keys: Map<String, Any?>) {}
    override fun log(message: String) {}
    override fun setCustomKey(key: String, value: Any?) {}
    override fun setUserId(id: String?) {}
}

internal object NoopPerformanceMonitor : PerformanceMonitor {
    override fun startHttpMetric(url: String, method: String): HttpPerformanceMetric = NoopHttpPerformanceMetric
}

private object NoopHttpPerformanceMetric : HttpPerformanceMetric {
    override fun setHttpResponseCode(code: Int) {}
    override fun setResponseContentType(contentType: String?) {}
    override fun stop() {}
}

/**
 * A non-fatal we raise ourselves to record a failure that has no Throwable of its own
 * (e.g. a login that came back as a "reason" string). Shows up in Crashlytics as a
 * grouped non-fatal so we can see where users drop off.
 */
class TelemetryError(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Global telemetry facade. Each platform injects its real implementation at startup
 * (Android: initTelemetry(context); iOS: from Swift after FirebaseApp.configure();
 * desktop: left as no-op). Everything in commonMain just calls [Telemetry].
 */
object Telemetry {
    var analytics: Analytics = NoopAnalytics
    var crashReporter: CrashReporter = NoopCrashReporter
    var performanceMonitor: PerformanceMonitor = NoopPerformanceMonitor

    fun log(event: AnalyticsEvent) {
        analytics.logEvent(event)
        crashReporter.log("event: ${event.name}")
    }

    fun screen(name: String) {
        analytics.logScreenView(name)
        crashReporter.setCustomKey("current_screen", name)
    }

    /** Same anonymous id for Analytics + Crashlytics so a crash can be tied to a session. */
    fun setUserId(id: String?) {
        analytics.setUserId(id)
        crashReporter.setUserId(id)
    }

    fun userProperty(key: String, value: String?) {
        analytics.setUserProperty(key, value)
        crashReporter.setCustomKey(key, value ?: "")
    }

    /** Record a non-fatal with ad-hoc context, e.g. recordError(e, "stage" to "profile_load"). */
    fun recordError(throwable: Throwable, vararg keys: Pair<String, Any?>) {
        crashReporter.recordException(throwable, keys.toMap())
    }

    fun breadcrumb(message: String) {
        crashReporter.log(message)
    }
}
