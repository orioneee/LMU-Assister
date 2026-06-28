import Foundation
import Shared
import FirebaseAnalytics
import FirebaseCrashlytics
import FirebasePerformance

// Note: `Analytics` exists in BOTH Shared (our Kotlin protocol) and FirebaseAnalytics
// (Firebase's class), so every reference is module-qualified to avoid ambiguity.

enum TelemetryBridge {
    static func install() {
        Telemetry.shared.analytics = FirebaseAnalyticsSink()
        Telemetry.shared.crashReporter = FirebaseCrashSink()
        Telemetry.shared.performanceMonitor = FirebasePerformanceSink()
        Telemetry.shared.userProperty(key: UserProperties.shared.PLATFORM, value: "ios")
    }
}

private final class FirebaseAnalyticsSink: Shared.Analytics {
    func logEvent(event: AnalyticsEvent) {
        FirebaseAnalytics.Analytics.logEvent(event.name, parameters: normalize(event.params))
    }

    func logScreenView(screenName: String) {
        FirebaseAnalytics.Analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: screenName
        ])
    }

    func setUserId(id: String?) {
        FirebaseAnalytics.Analytics.setUserID(id)
    }

    func setUserProperty(key: String, value: String?) {
        FirebaseAnalytics.Analytics.setUserProperty(value, forName: key)
    }

    /// Firebase wants String/Number params; Kotlin booleans arrive as NSNumber, so
    /// turn them back into "true"/"false" to match the Android side.
    private func normalize(_ params: [String: Any]) -> [String: Any] {
        var out: [String: Any] = [:]
        for (k, v) in params {
            if let b = v as? KotlinBoolean {
                out[k] = b.boolValue ? "true" : "false"
            } else {
                out[k] = v
            }
        }
        return out
    }
}

private final class FirebaseCrashSink: CrashReporter {
    private let crash = Crashlytics.crashlytics()

    func recordException(throwable: KotlinThrowable, keys: [String: Any]) {
        for (k, v) in keys { setCustomKey(key: k, value: v) }
        let userInfo: [String: Any] = [
            NSLocalizedDescriptionKey: throwable.message ?? String(describing: type(of: throwable))
        ]
        let error = NSError(domain: "KotlinNonFatal", code: 0, userInfo: userInfo)
        crash.record(error: error)
    }

    func log(message: String) {
        crash.log(message)
    }

    func setCustomKey(key: String, value: Any?) {
        guard let value = value else { crash.setCustomValue("", forKey: key); return }
        if let b = value as? KotlinBoolean {
            crash.setCustomValue(b.boolValue, forKey: key)
        } else {
            crash.setCustomValue(value, forKey: key)
        }
    }

    func setUserId(id: String?) {
        crash.setUserID(id ?? "")
    }
}

private final class FirebasePerformanceSink: PerformanceMonitor {
    func startHttpMetric(url: String, method: String) -> HttpPerformanceMetric {
        guard let url = URL(string: url) else {
            return NoopHttpMetric()
        }
        guard let metric = HTTPMetric(url: url, httpMethod: method.firebaseHttpMethod) else {
            return NoopHttpMetric()
        }
        metric.start()
        return FirebaseHttpMetric(metric: metric)
    }
}

private final class FirebaseHttpMetric: HttpPerformanceMetric {
    private let metric: HTTPMetric

    init(metric: HTTPMetric) {
        self.metric = metric
    }

    func setHttpResponseCode(code: Int32) {
        metric.responseCode = Int(code)
    }

    func setResponseContentType(contentType: String?) {
        metric.responseContentType = contentType
    }

    func stop() {
        metric.stop()
    }
}

private final class NoopHttpMetric: HttpPerformanceMetric {
    func setHttpResponseCode(code: Int32) {}
    func setResponseContentType(contentType: String?) {}
    func stop() {}
}

private extension String {
    var firebaseHttpMethod: HTTPMethod {
        switch uppercased() {
        case "CONNECT": return .connect
        case "DELETE": return .delete
        case "GET": return .get
        case "HEAD": return .head
        case "OPTIONS": return .options
        case "PATCH": return .patch
        case "POST": return .post
        case "PUT": return .put
        case "TRACE": return .trace
        default: return .get
        }
    }
}
