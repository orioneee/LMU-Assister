import FirebaseMessaging
import Shared
import UIKit
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        let pushEnabled = (Bundle.main.object(forInfoDictionaryKey: "LMUPushNotificationsEnabled") as? NSString)?.boolValue ?? false
        IosFcmTokenRegistrar.shared.configurePushNotifications(enabled: pushEnabled)
        Messaging.messaging().isAutoInitEnabled = pushEnabled
        guard pushEnabled else { return true }

        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        // Registration itself does not show the permission prompt. The Compose UI
        // asks at the moment the user enables device push.
        application.registerForRemoteNotifications()
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        // SwiftUI apps must explicitly associate the APNs token with Firebase.
        Messaging.messaging().apnsToken = deviceToken
        Messaging.messaging().token { token, error in
            if let token {
                IosFcmTokenRegistrar.shared.updateFcmToken(token: token)
            } else if let error {
                IosFcmTokenRegistrar.shared.reportFcmTokenFailure(reason: error.localizedDescription)
            }
        }
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        IosFcmTokenRegistrar.shared.reportFcmTokenFailure(reason: error.localizedDescription)
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        IosFcmTokenRegistrar.shared.updateFcmToken(token: fcmToken)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let content = notification.request.content
        let type = notificationType(from: content.userInfo)
        IosFcmTokenRegistrar.shared.logNotificationReceived(
            notificationType: type,
            hasBody: !content.body.isEmpty
        )
        IosFcmTokenRegistrar.shared.logNotificationDisplayed(notificationType: type)
        completionHandler([.banner, .list, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        IosFcmTokenRegistrar.shared.logNotificationOpened(
            notificationType: notificationType(from: userInfo),
            notificationId: userInfo["notification_id"] as? String
        )
        completionHandler()
    }

    private func notificationType(from userInfo: [AnyHashable: Any]) -> String {
        userInfo["type"] as? String ?? "unknown"
    }
}
