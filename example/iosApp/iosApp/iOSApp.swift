import SwiftUI
import shared
import FirebaseAnalytics

@main
struct iOSApp: App {
    
    init() {
        print(shared.Platform_iosKt.myNativeClass)
        shared.TestKt.configureFirebase()
        print(shared.TestKt.consentStatusGranted)
    }
    
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
