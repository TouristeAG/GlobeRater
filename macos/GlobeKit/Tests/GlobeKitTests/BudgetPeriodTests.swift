import Foundation
import Testing
@testable import GlobeKit

private let zurich: Calendar = {
    var c = Calendar(identifier: .gregorian)
    c.timeZone = TimeZone(identifier: "Europe/Zurich")!
    return c
}()

private func date(_ y: Int, _ m: Int, _ d: Int, _ h: Int = 0, _ min: Int = 0) -> Date {
    zurich.date(from: DateComponents(year: y, month: m, day: d, hour: h, minute: min))!
}

@Test(arguments: BudgetSpan.allCases)
func periodNeverDriftsIntoTheFuture(span: BudgetSpan) {
    let anchor = date(2025, 12, 28), now = date(2026, 11, 10, 12)
    let current = BudgetPeriod.currentStart(anchor: anchor, refreshHour: 0, span: span, now: now, calendar: zurich)
    let next = BudgetPeriod.nextStart(anchor: anchor, refreshHour: 0, span: span, now: now, calendar: zurich)
    #expect(current <= now)
    #expect(next > now)
}

@Test func monthEndClampsAndRecovers() {
    let anchor = date(2026, 1, 31), now = date(2026, 3, 15)
    #expect(BudgetPeriod.currentStart(anchor: anchor, refreshHour: 0, span: .monthly, now: now, calendar: zurich) == date(2026, 2, 28))
    #expect(BudgetPeriod.nextStart(anchor: anchor, refreshHour: 0, span: .monthly, now: now, calendar: zurich) == date(2026, 3, 31))
}

@Test func refreshHourIsApplied() {
    let anchor = date(2026, 11, 10, 15, 42), now = date(2026, 11, 11, 5)
    #expect(BudgetPeriod.currentStart(anchor: anchor, refreshHour: 6, span: .daily, now: now, calendar: zurich) == date(2026, 11, 10, 6))
    #expect(BudgetPeriod.nextStart(anchor: anchor, refreshHour: 6, span: .daily, now: now, calendar: zurich) == date(2026, 11, 11, 6))
}

@Test func dailyResetAcrossDSTStart() {
    // 29 March 2026: clocks jump from 02:00 to 03:00 in Zurich, so 02:00 does not exist that day.
    let anchor = date(2026, 3, 1), now = date(2026, 3, 29, 10)
    #expect(BudgetPeriod.currentStart(anchor: anchor, refreshHour: 2, span: .daily, now: now, calendar: zurich) == date(2026, 3, 29, 3))
    #expect(BudgetPeriod.nextStart(anchor: anchor, refreshHour: 2, span: .daily, now: now, calendar: zurich) == date(2026, 3, 30, 2))
}

@Test func anchorInTheFutureStillGivesAPastStart() {
    let anchor = date(2026, 12, 15), now = date(2026, 11, 10, 12)
    let current = BudgetPeriod.currentStart(anchor: anchor, refreshHour: 0, span: .monthly, now: now, calendar: zurich)
    #expect(current == date(2026, 10, 15))
}

@Test func displayLimitScales() {
    var b = BudgetSettings(anchor: .now)
    b.amount = 3000; b.span = .monthly; b.displaySpan = .weekly
    #expect(b.displayLimit == 700)
    b.displaySpan = .monthly
    #expect(b.displayLimit == 3000)
}
