import Foundation

import CryptoSwift
import FirebaseDatabase

@objcMembers public class TestClass: NSObject {
    public func getSomeValue() -> String {
        return "HelloTest!"
    }

    public func getValueFromCrypt() -> String {
        return "123".md5()
    }
}


@objcMembers public class MySwiftDummyClass: NSObject {
    public func mySwiftDummyFunction() -> String {
        return "Hello from Swift!"
    }
}
