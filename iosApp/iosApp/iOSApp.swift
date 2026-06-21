import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        TelemetryBridge.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
