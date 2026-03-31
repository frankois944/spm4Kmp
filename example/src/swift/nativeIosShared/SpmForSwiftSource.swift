import Foundation
import UIKit

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

    public func localFile() -> String {
        // explicit declation of Foundation.Bundle for backward compatibility (xcode < 26)
        guard let url = Foundation.Bundle.module.url(forResource: "bridgeString",
                                                     withExtension: "txt") else {
            assertionFailure("bridgeString.txt not found in Bundle.module")
            return ""
        }
        return (try? String(contentsOf: url)) ?? ""
    }
}


@objcMembers public class MySwiftDummyClass: NSObject {
    public func mySwiftDummyFunction() -> String {
        return "Hello from Swift!"
    }
}
