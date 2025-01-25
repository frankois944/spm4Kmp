//
//  DummyFramwork.swift
//  DummyFramework
//
//  Created by Francois Dabonot on 06/11/2024.
//

import Foundation
internal import CryptoSwift
internal import LaunchDarkly

@objc public class MyDummyFramework: NSObject {
    
    @objc public override init() {}
    
    @objc public func printSomeValue() {
        print("Hello the work")
    }
}
