//
//  DummyFramwork.swift
//  DummyFramework
//
//  Created by Francois Dabonot on 06/11/2024.
//

import Foundation

@objc public class MyDummyFramework: NSObject {
    
    @objc public override init() {}
    
    @objc public func printSomeValue() {
        print("Hello the work")
    }
}
