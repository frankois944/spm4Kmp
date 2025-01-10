import SwiftUI
import shared
import FirebaseAnalytics
import FirebaseCore

@main
struct iOSApp: App {
    
    init() {
        print(shared.Platform_iosKt.myNativeClass)
        shared.Platform_iosKt.configureFirebase()
        print(shared.Platform_iosKt.consentStatusGranted)
        print(shared.Platform_iosKt.localSourceDummyTest)
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
