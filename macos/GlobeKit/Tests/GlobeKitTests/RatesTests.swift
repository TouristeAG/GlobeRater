import Foundation
import Testing
@testable import GlobeKit

@Test func conversionGoesThroughUsd() {
    let r = Rates(usd: ["USD": 1, "EUR": 0.9, "CHF": 0.8], updatedAt: .now)
    #expect(abs(r.convert(100, from: "USD", to: "EUR")! - 90) < 1e-9)
    #expect(abs(r.convert(100, from: "CHF", to: "EUR")! - 112.5) < 1e-9)
    #expect(r.convert(100, from: "CHF", to: "THB") == nil)
    #expect(r.convert(5, from: "THB", to: "THB") == 5)
}

@Test func usdIsAlwaysKnown() {
    let r = Rates(usd: ["EUR": 0.9], updatedAt: .now)
    #expect(abs(r.convert(10, from: "USD", to: "EUR")! - 9) < 1e-9)
}

@Test func sampleRatesAreFlagged() {
    #expect(Rates.sample.isSample)
    #expect(Rates.sample.usd["EUR"] == 0.92)
    #expect(!Rates(usd: [:], updatedAt: .now).isSample)
}
