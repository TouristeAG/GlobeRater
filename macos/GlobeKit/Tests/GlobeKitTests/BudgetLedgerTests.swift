import Foundation
import Testing
@testable import GlobeKit

private let rates = Rates(usd: ["USD": 1, "EUR": 0.9], updatedAt: .now)
private func ledger(_ dir: URL = temporaryDirectory()) -> BudgetLedger { BudgetLedger(fileURL: dir.appendingPathComponent("ledger.json")) }

@Test func ledgerSumsCurrentPeriodOnly() throws {
    let l = ledger(), budget = monthlyBudget(), now = Date()
    let start = BudgetPeriod.currentStart(anchor: budget.anchor, refreshHour: 0, span: .monthly, now: now)
    try l.add(40, currency: "USD", at: start.addingTimeInterval(-3600), budget: budget)
    try l.add(10, currency: "USD", at: now, budget: budget)
    try l.add(5, currency: "USD", at: now, budget: budget)
    #expect(l.spent(budget: budget, rates: rates, now: now) == 15)
}

@Test func addPurgesExpensesOlderThanAYear() throws {
    let l = ledger(), budget = monthlyBudget(), now = Date()
    try l.add(40, currency: "USD", at: now.addingTimeInterval(-400 * 86_400), budget: budget)
    try l.add(10, currency: "USD", at: now, budget: budget)
    #expect(l.expenses().map(\.amount) == [10])
}

@Test func unknownRateKeepsAmountAsTyped() throws {
    let l = ledger(), budget = monthlyBudget()
    try l.add(500, currency: "THB", at: .now, budget: budget)
    #expect(l.spent(budget: budget, rates: rates, now: .now) == 500)
}

@Test func budgetCurrencyChangeConvertsAtReadTime() throws {
    let l = ledger()
    try l.add(100, currency: "EUR", at: .now, budget: monthlyBudget(currency: "EUR"))
    let spent = l.spent(budget: monthlyBudget(currency: "USD"), rates: rates, now: .now)
    #expect(abs(spent - 111.111) < 0.01)
}

@Test func concurrentWritersDoNotLoseExpenses() async throws {
    let dir = temporaryDirectory(), budget = monthlyBudget()
    let a = ledger(dir), b = ledger(dir)
    await withTaskGroup(of: Void.self) { group in
        for i in 0..<100 {
            let l = i.isMultiple(of: 2) ? a : b
            group.addTask { try? l.add(1, currency: "USD", at: .now, budget: budget) }
        }
    }
    #expect(a.expenses().count == 100)
}

@Test func resetNowZeroesCurrentPeriod() throws {
    let l = ledger(), budget = monthlyBudget(), now = Date()
    try l.add(50, currency: "USD", at: now.addingTimeInterval(-10), budget: budget)
    try l.resetNow(at: now.addingTimeInterval(-5))
    try l.add(20, currency: "USD", at: now, budget: budget)
    #expect(l.spent(budget: budget, rates: rates, now: now) == 20)
}

@Test func corruptFileIsSetAside() throws {
    let dir = temporaryDirectory()
    try Data("{oops".utf8).write(to: dir.appendingPathComponent("ledger.json"))
    let l = ledger(dir)
    #expect(l.expenses().isEmpty)
    let names = try FileManager.default.contentsOfDirectory(atPath: dir.path)
    #expect(names.contains { $0.hasPrefix("ledger.corrupt-") })
    try l.add(3, currency: "USD", at: .now, budget: monthlyBudget())
    #expect(l.expenses().count == 1)
}

@Test func summaryClampsAndFlagsOverspend() throws {
    let l = ledger(), budget = monthlyBudget(amount: 100)
    try l.add(130, currency: "USD", at: .now, budget: budget)
    let s = BudgetSummary.make(ledger: l, budget: budget, rates: rates, now: .now)
    #expect(s.fraction == 1 && s.isOver && s.remaining == -30 && s.limit == 100 && s.currency == "USD")
    #expect(s.nextReset > .now && s.periodStart <= .now)
}

@Test func summaryOfEmptyLedger() {
    let s = BudgetSummary.make(ledger: ledger(), budget: monthlyBudget(amount: 100), rates: rates, now: .now)
    #expect(s.spent == 0 && s.fraction == 0 && !s.isOver && s.remaining == 100)
}

@Test func switchingTheDisplayPeriodKeepsOlderExpenses() throws {
    let l = ledger(), now = Date()
    var monthly = monthlyBudget()
    let earlier = now.addingTimeInterval(-3 * 86_400)
    let monthStart = BudgetPeriod.currentStart(anchor: monthly.anchor, refreshHour: 0, span: .monthly, now: now)
    try l.add(30, currency: "USD", at: max(earlier, monthStart), budget: monthly)
    var daily = monthly
    daily.displaySpan = .daily
    try l.add(5, currency: "USD", at: now, budget: daily)
    monthly.displaySpan = .monthly
    #expect(l.spent(budget: monthly, rates: rates, now: now) == 35)
}
