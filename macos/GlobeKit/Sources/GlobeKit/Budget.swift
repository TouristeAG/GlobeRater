import Foundation

public enum BudgetSpan: String, Codable, CaseIterable, Sendable {
    case daily, weekly, monthly, yearly

    /// Length used to scale the budget between periods (same values as Android).
    public var days: Int {
        switch self {
        case .daily: 1
        case .weekly: 7
        case .monthly: 30
        case .yearly: 365
        }
    }

    var component: Calendar.Component {
        switch self {
        case .daily: .day
        case .weekly: .weekOfYear
        case .monthly: .month
        case .yearly: .year
        }
    }
}

public struct BudgetSettings: Codable, Equatable, Sendable {
    public var enabled = true
    public var amount = 2000.0
    public var currency = "USD"
    /// Period the amount is entered for.
    public var span = BudgetSpan.monthly
    /// Period shown and reset on.
    public var displaySpan = BudgetSpan.monthly
    public var anchor: Date
    public var refreshHour = 0

    public init(anchor: Date) { self.anchor = anchor }

    /// Budget for the display period: 3000 a month shown weekly = 3000 × 7 / 30 = 700.
    public var displayLimit: Double { amount * Double(displaySpan.days) / Double(span.days) }
}

/// Calendar-based budget periods (port of BudgetRefreshCalculator.kt): real month and year
/// lengths, no drift, never a start in the future.
public enum BudgetPeriod {
    public static func start(index: Int, anchor: Date, refreshHour: Int, span: BudgetSpan, calendar: Calendar = .current) -> Date {
        let day = calendar.startOfDay(for: anchor)
        let shifted = calendar.date(byAdding: span.component, value: index, to: day) ?? day
        let hour = min(max(refreshHour, 0), 23)
        return calendar.date(bySettingHour: hour, minute: 0, second: 0, of: shifted, matchingPolicy: .nextTime) ?? shifted
    }

    public static func currentStart(anchor: Date, refreshHour: Int, span: BudgetSpan, now: Date, calendar: Calendar = .current) -> Date {
        start(index: index(anchor: anchor, refreshHour: refreshHour, span: span, now: now, calendar: calendar),
              anchor: anchor, refreshHour: refreshHour, span: span, calendar: calendar)
    }

    public static func nextStart(anchor: Date, refreshHour: Int, span: BudgetSpan, now: Date, calendar: Calendar = .current) -> Date {
        start(index: index(anchor: anchor, refreshHour: refreshHour, span: span, now: now, calendar: calendar) + 1,
              anchor: anchor, refreshHour: refreshHour, span: span, calendar: calendar)
    }

    /// Index of the period containing `now`: an estimate from the average length, corrected by walking the calendar.
    private static func index(anchor: Date, refreshHour: Int, span: BudgetSpan, now: Date, calendar: Calendar) -> Int {
        func s(_ n: Int) -> Date { start(index: n, anchor: anchor, refreshHour: refreshHour, span: span, calendar: calendar) }
        let length = Double(span.days) * 86_400
        var n = Int((now.timeIntervalSince(s(0)) / length).rounded(.down))
        while s(n) > now { n -= 1 }
        while s(n + 1) <= now { n += 1 }
        return n
    }
}
