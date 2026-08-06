import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        FirebaseApp.configure()
        TelemetryBridge.install()
        FeatureFlagsBridge.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
