import Foundation
import UIKit

import CryptoSwift
import FirebaseDatabase

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
}


@objcMembers public class MySwiftDummyClass: NSObject {
    public func mySwiftDummyFunction() -> String {
        return "Hello from Swift!"
    }
}
