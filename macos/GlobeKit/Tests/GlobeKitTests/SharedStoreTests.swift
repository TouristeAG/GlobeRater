import Foundation
import Testing
@testable import GlobeKit

@Test func emptyStoreGivesAndroidDefaults() {
    let s = makeStore().settings
    #expect(s.inputCurrencies == ["USD", "EUR", "GBP"])
    #expect(s.outputCurrencies == ["USD", "EUR", "GBP", "JPY", "CAD"])
    #expect(s.budget.amount == 2000 && s.budget.currency == "USD" && s.budget.span == .monthly)
    #expect(s.rateIntervalDays == 1 && s.units == .celsius && s.apiKey.isEmpty)
    #expect(s.home == nil && s.here == .device && s.showInMenuBar)
}

@Test func defaultsAreStableAcrossReads() {
    let store = makeStore()
    #expect(store.settings.budget.anchor == store.settings.budget.anchor)
}

@Test func valuesAreSharedBetweenStoreInstances() {
    let suite = UUID().uuidString, dir = temporaryDirectory()
    let a = makeStore(directory: dir, suite: suite)
    a.settings.units = .fahrenheit
    a.pad = PadState(text: "12,5", currency: "EUR", lastInput: 12.5)
    a.rates = Rates(usd: ["EUR": 0.9], updatedAt: Date(timeIntervalSince1970: 100))
    let b = makeStore(directory: dir, suite: suite)
    #expect(b.settings.units == .fahrenheit)
    #expect(b.pad.text == "12,5" && b.pad.currency == "EUR")
    #expect(b.rates.usd["EUR"] == 0.9)
}

@Test func missingRatesFallBackToSample() {
    #expect(makeStore().rates.isSample)
}
