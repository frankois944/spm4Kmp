import SwiftUI
import shared
import FirebaseAnalytics
import FirebaseCore

@main
struct iOSApp: App {
    
    init() {
        print(shared.Platform_iosKt.myNativeClass)
        shared.TestKt.configureFirebase()
        print(shared.TestKt.consentStatusGranted)
        Analytics.logEvent(AnalyticsEventSelectContent, parameters: [
          AnalyticsParameterItemID: "id-",
          AnalyticsParameterItemName: "title",
          AnalyticsParameterContentType: "cont",
        ])
    }
    
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
