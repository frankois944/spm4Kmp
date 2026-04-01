import Foundation
import CryptoKit
import UIKit
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
        let data = Data("123".utf8)
        let digest = Insecure.MD5.hash(data: data)
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    public func doAsyncStuff() async {
        print("Nothing")
    }

    public func cMethod() {
        hev_socks5_tunnel_quit()
    }

    public func localFile() -> String {
        // Explicit use of Foundation.Bundle via alias to avoid CryptoSwift's Bundle shadowing on older Xcode
        guard let url = Bundle.module.url(forResource: "bridgeString", withExtension: "txt") else {
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
