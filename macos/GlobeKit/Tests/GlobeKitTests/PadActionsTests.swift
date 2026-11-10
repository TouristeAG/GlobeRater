import Foundation
import Testing
@testable import GlobeKit

private let now = Date()

@Test func keysEditTheSharedAmount() {
    let store = makeStore()
    for t in ["1", "2", ",", "5"] { PadActions.key(t, store: store) }
    #expect(store.pad.text == "12,5" && store.pad.lastInput == 12.5)
    PadActions.key("<", store: store)
    #expect(store.pad.text == "12," && store.pad.lastInput == 12)
    PadActions.key("C", store: store)
    #expect(store.pad.text == "" && store.pad.lastInput == 0)
}

@Test func selectingACurrencyKeepsTheAmount() {
    let store = makeStore()
    PadActions.key("5", store: store)
    PadActions.selectCurrency("EUR", store: store)
    #expect(store.pad.currency == "EUR" && store.pad.text == "5")
}

@Test func plusAddsTheTypedAmountAndClearsThePad() {
    let store = makeStore()
    PadActions.selectCurrency("EUR", store: store)
    PadActions.key("4", store: store); PadActions.key("0", store: store)
    #expect(PadActions.addPadToBudget(store: store, now: now))
    #expect(store.ledger.expenses().map(\.amount) == [40])
    #expect(store.ledger.expenses().first?.currency == "EUR")
    #expect(store.pad.text == "" && store.pad.lastInput == 0)
}

@Test func plusDoesNothingOnZeroOrDisabledBudget() {
    let store = makeStore()
    #expect(!PadActions.addPadToBudget(store: store, now: now))
    store.settings.budget.enabled = false
    PadActions.key("9", store: store)
    #expect(!PadActions.addPadToBudget(store: store, now: now))
    #expect(store.ledger.expenses().isEmpty && store.pad.text == "9")
}

@Test func budgetWidgetPlusAddsTheLastTypedAmountOnce() {
    let store = makeStore()
    PadActions.key("7", store: store)
    #expect(PadActions.addLastInput(store: store, now: now))
    #expect(!PadActions.addLastInput(store: store, now: now))
    #expect(store.ledger.expenses().map(\.amount) == [7])
}

@Test func typedAmountGoesToTheBudget() {
    let store = makeStore()
    #expect(PadActions.addTyped("12.5", currency: "GBP", store: store, now: now))
    #expect(!PadActions.addTyped("0", currency: "GBP", store: store, now: now))
    #expect(store.ledger.expenses().map(\.amount) == [12.5])
}
