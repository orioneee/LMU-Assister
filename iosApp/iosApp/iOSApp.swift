import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        // Auto-reads GoogleService-Info.plist, then points the shared Telemetry
        // facade at the Firebase iOS SDK (Analytics + Crashlytics).
        FirebaseApp.configure()
        TelemetryBridge.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
