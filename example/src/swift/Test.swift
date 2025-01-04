import Foundation

import CryptoSwift

@objcMembers public class TestClass: NSObject {
    public func getSomeValue() -> String {
        return "HelloTest!"
    }

    public func getValueFromCrypt() -> String {
        return "123".md5()
    }
}
