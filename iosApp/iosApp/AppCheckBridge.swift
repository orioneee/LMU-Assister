import Foundation
import FirebaseCore
import FirebaseAppCheck
import Shared

enum FirebaseAppCheckBridge {
    static func configureBeforeFirebase() {
#if DEBUG
        FirebaseAppCheck.AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
#else
        FirebaseAppCheck.AppCheck.setAppCheckProviderFactory(ProductionAppCheckProviderFactory())
#endif
    }

    static func installKotlinBridge() {
        IosAppCheck.shared.tokenSource = FirebaseIosAppCheckTokenSource()
    }
}

private final class ProductionAppCheckProviderFactory: NSObject, FirebaseAppCheck.AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> FirebaseAppCheck.AppCheckProvider? {
        if #available(iOS 14.0, *) {
            return AppAttestProvider(app: app)
        }
        return DeviceCheckProvider(app: app)
    }
}

private final class FirebaseIosAppCheckTokenSource: IosAppCheckTokenSource {
    func fetch(onComplete: @escaping (String?) -> Void) {
        FirebaseAppCheck.AppCheck.appCheck().token(forcingRefresh: false) { token, error in
            guard error == nil else {
                onComplete(nil)
                return
            }
            onComplete(token?.token)
        }
    }
}
