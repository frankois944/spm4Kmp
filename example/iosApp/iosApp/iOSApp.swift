import SwiftUI
import shared

@main
struct iOSApp: App {
    
    init() {
        print(shared.Platform_iosKt.myNativeClass)
    }
    
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
