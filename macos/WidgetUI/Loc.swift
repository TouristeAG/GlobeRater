import Foundation
import GlobeKit

/// Localized string from Localizable.xcstrings, looked up by key (shared with the Android translations).
func L(_ key: String) -> String {
    Bundle.main.localizedString(forKey: key, value: nil, table: nil)
}

func L(_ key: String, _ args: any CVarArg...) -> String {
    String(format: L(key), locale: .current, arguments: args)
}

enum Formats {
    static func periodName(_ span: BudgetSpan) -> String {
        switch span {
        case .daily: L("period_today")
        case .weekly: L("period_week")
        case .monthly: L("period_month")
        case .yearly: L("period_year")
        }
    }

    static func shortDate(_ date: Date, zone: TimeZone = .current) -> String {
        var style = Date.FormatStyle.dateTime.day().month(.abbreviated)
        style.timeZone = zone
        return date.formatted(style)
    }

    static func weekdayDate(_ date: Date, zone: TimeZone) -> String {
        var style = Date.FormatStyle.dateTime.weekday(.abbreviated).day().month(.abbreviated)
        style.timeZone = zone
        return date.formatted(style)
    }

    static func leftOrOver(_ b: BudgetSummary) -> String {
        b.remaining >= 0 ? L("budget_left", Money.format(b.remaining, currency: b.currency))
                         : L("budget_over", Money.format(-b.remaining, currency: b.currency))
    }

    static func decimalSeparator() -> String { Locale.current.decimalSeparator ?? "." }
}
