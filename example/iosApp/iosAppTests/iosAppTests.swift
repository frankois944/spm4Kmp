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

    @Test func example() async throws {
        let greet: () = await ContentView.testGreet()
    }

}
