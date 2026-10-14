import SwiftUI
import FirebaseCore
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // Firebase is optional, as on Android: GoogleService-Info.plist is not in git, and the
        // "Copy GoogleService-Info.plist" build phase bundles it only when it is present.
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
            CrashlyticsBridgeKt.installCrashlytics(bridge: FirebaseCrashlyticsBridge())
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
