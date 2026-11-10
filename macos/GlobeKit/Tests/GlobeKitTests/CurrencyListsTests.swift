import Foundation
import Testing
@testable import GlobeKit

@Test func outputCurrencyCanBeRemovedDownToOne() {
    var s = AppSettings.defaults(now: .now)
    s.removeOutputCurrency(at: 1)
    #expect(s.outputCurrencies == ["USD", "GBP", "JPY", "CAD"])
    for _ in 0..<5 { s.removeOutputCurrency(at: 0) }
    #expect(s.outputCurrencies == ["CAD"])
}

@Test func addingPicksTheFirstUnusedCurrencyAndStopsAtFive() {
    var s = AppSettings.defaults(now: .now)
    s.outputCurrencies = ["EUR"]
    s.addOutputCurrency(from: ["EUR", "CHF", "USD"])
    #expect(s.outputCurrencies == ["EUR", "CHF"])
    s.outputCurrencies = ["USD", "EUR", "GBP", "JPY", "CAD"]
    s.addOutputCurrency(from: ["CHF"])
    #expect(s.outputCurrencies.count == AppSettings.maxOutputCurrencies)
}

@Test func fewerOutputCurrenciesGiveFewerConversions() {
    var snapshot = WidgetSnapshot.sample
    snapshot.settings.outputCurrencies = ["JPY"]
    snapshot.pad.currency = "USD"
    #expect(snapshot.conversions(limit: 5).map(\.code) == ["JPY"])
}

@Test func inputCurrenciesGoFromOneToThree() {
    var s = AppSettings.defaults(now: .now)
    s.addInputCurrency(from: ["CHF"])
    #expect(s.inputCurrencies == ["USD", "EUR", "GBP"])
    s.removeInputCurrency(at: 0); s.removeInputCurrency(at: 0); s.removeInputCurrency(at: 0)
    #expect(s.inputCurrencies == ["GBP"])
    s.addInputCurrency(from: ["GBP", "CHF"])
    #expect(s.inputCurrencies == ["GBP", "CHF"])
}

@Test func removingTheKeypadCurrencySwitchesTheKeypadToARemainingOne() {
    let store = makeStore()
    PadActions.selectCurrency("EUR", store: store)
    PadActions.key("7", store: store)
    PadActions.removeInputCurrency(at: 1, store: store)
    #expect(store.settings.inputCurrencies == ["USD", "GBP"])
    #expect(store.pad.currency == "USD" && store.pad.text == "7")
}

@Test func removingAnotherCurrencyKeepsTheKeypadCurrency() {
    let store = makeStore()
    PadActions.selectCurrency("GBP", store: store)
    PadActions.removeInputCurrency(at: 0, store: store)
    #expect(store.pad.currency == "GBP")
}

@Test func replacingTheKeypadCurrencyMovesTheKeypadToTheFirstOne() {
    let store = makeStore()
    PadActions.selectCurrency("EUR", store: store)
    store.updateSettings { $0.inputCurrencies[1] = "CHF" }
    PadActions.keepCurrencyInList(store: store)
    #expect(store.pad.currency == "USD")
}
