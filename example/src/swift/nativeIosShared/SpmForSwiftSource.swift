import Foundation
import UIKit
import GoogleMaps

import CryptoSwift
import FirebaseDatabase
import HevSocks5Tunnel
#if canImport(registrydummy)
import registrydummy
#endif

// Force cinterop to include `platform.UIKit.UIView`
@objcMembers public class MyDummyView: UIView {}

@objcMembers public class TestClass: NSObject {
    public func getSomeValue() -> String {
        return "HelloTest!"
    }

    public func setView(view: UIView) {
        // store view
    }

    public func getView() -> UIView {
        return UIView()
    }

    public func setViewWithNSObject(view: NSObject) {
        // store view
    }

    public func getViewWithNSObject() -> NSObject {
        return UIView()
    }

    public func getValueFromCrypt() -> String {
        return "123".md5()
    }

    public func doAsyncStuff() async {
        print("Nothing")
    }

    public func cMethod() {
        hev_socks5_tunnel_quit()
    }
}


@objcMembers public class MySwiftDummyClass: NSObject {
    public func mySwiftDummyFunction() -> String {
        return "Hello from Swift!"
    }
}
