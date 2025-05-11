//
//  DummyFramwork.swift
//  DummyFramework
//
//  Created by Francois Dabonot on 06/11/2024.
//

import Foundation

@objc public class MyDummyFramework: NSObject {

    private let bundle = Bundle(identifier: "com.dummy.DummyFramework")!

    @objc public func printSomeValue() {
        print("Hello the work")
    }

    @objc public func getMyResource() -> String {
        guard let path = bundle.path(forResource: "IAmAResource", ofType: "txt"),
                let content = try? String(contentsOfFile: path, encoding: .utf8) else {
            fatalError("Cant retrieve resource IAmAResources.txt")
        }
        return content
    }

    @objc public func MyResourceIsMissing() -> String? {
        guard let path = bundle.path(forResource: "IAmNotAResource", ofType: "txt"),
                let content = try? String(contentsOfFile: path, encoding: .utf8) else {
            return nil
        }
        return content
    }
}
