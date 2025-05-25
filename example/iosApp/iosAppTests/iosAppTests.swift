//
//  iosAppTests.swift
//  iosAppTests
//
//  Created by Francois Dabonot on 12/05/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import Testing
@testable import iosApp


struct iosAppTests {

    @Test func examplePackageRes() async throws {
        await #expect(ContentView.testGetPackageResource(name: "N/A") != nil)
    }
    
    @Test func exampleFrameworkRes() async throws {
        await #expect(ContentView.testGetFrameworkResource(name: "N/A") != nil)
    }
    
}
