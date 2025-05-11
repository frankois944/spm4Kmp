// The Swift Programming Language
// https://docs.swift.org/swift-book

import Foundation

@objcMembers public class LocalSourceDummy: NSObject {
    public func test() -> String {
        return "TEST DUMMY FRAMEWORK"
    }

    @objc public func getMyInternalResource() -> String {
        guard let path = Bundle.module.path(forResource: "IAmAResource", ofType: "txt"),
                let content = try? String(contentsOfFile: path, encoding: .utf8) else {
            fatalError("Cant retrieve resource IAmAResources.txt")
        }
        return content
    }

    @objc public func getSomeResource(name: String) -> String? {
        guard let path = Bundle.module.path(forResource: name, ofType: nil),
                let content = try? String(contentsOfFile: path, encoding: .utf8) else {
            return nil
        }
        return content
    }
}
