import Foundation

import CryptoSwift
import Glibc

@_cdecl("mySwiftDummyFunction")
public func mySwiftDummyFunction() -> String {
    return "Hello from Linux Swift!"
}

@_cdecl("getValueFromCrypt")
public func getValueFromCrypt() -> String {
    return "123".md5()
}

