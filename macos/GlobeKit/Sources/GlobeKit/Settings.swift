import Foundation

public struct Place: Codable, Equatable, Hashable, Sendable {
    public var name: String
    public var label: String
    public var latitude: Double
    public var longitude: Double
    public var timeZoneID: String?

    public init(name: String, label: String, latitude: Double, longitude: Double, timeZoneID: String?) {
        self.name = name; self.label = label; self.latitude = latitude; self.longitude = longitude; self.timeZoneID = timeZoneID
    }
}

/// "Here" is either where the Mac is, or a city picked by the user.
public enum HereSetting: Codable, Equatable, Sendable {
    case device
    case fixed(Place)
}

public enum TemperatureUnit: String, Codable, CaseIterable, Sendable {
    case celsius, fahrenheit
}

public struct AppSettings: Codable, Equatable, Sendable {
    public var inputCurrencies: [String]
    public var outputCurrencies: [String]
    public var apiKey: String
    public var rateIntervalDays: Int
    public var budget: BudgetSettings
    public var home: Place?
    public var here: HereSetting
    public var units: TemperatureUnit
    public var showInMenuBar: Bool

    /// The user picks 1 to 3 keypad currencies and 1 to 5 currencies to convert to.
    public static let maxInputCurrencies = 3
    public static let maxOutputCurrencies = 5

    public mutating func addInputCurrency(from all: [String]) {
        Self.add(to: &inputCurrencies, max: Self.maxInputCurrencies, from: all)
    }

    public mutating func removeInputCurrency(at index: Int) { Self.remove(from: &inputCurrencies, at: index) }

    public mutating func addOutputCurrency(from all: [String]) {
        Self.add(to: &outputCurrencies, max: Self.maxOutputCurrencies, from: all)
    }

    public mutating func removeOutputCurrency(at index: Int) { Self.remove(from: &outputCurrencies, at: index) }

    /// Appends the first currency of `all` not already listed, unless the list is full.
    private static func add(to list: inout [String], max: Int, from all: [String]) {
        guard list.count < max, let next = all.first(where: { !list.contains($0) }) else { return }
        list.append(next)
    }

    /// Removes one currency, always keeping at least one.
    private static func remove(from list: inout [String], at index: Int) {
        guard list.count > 1, list.indices.contains(index) else { return }
        list.remove(at: index)
    }

    /// Same defaults as the Android app (AppSettings.kt).
    public static func defaults(now: Date) -> AppSettings {
        AppSettings(
            inputCurrencies: ["USD", "EUR", "GBP"],
            outputCurrencies: ["USD", "EUR", "GBP", "JPY", "CAD"],
            apiKey: "",
            rateIntervalDays: 1,
            budget: BudgetSettings(anchor: now),
            home: nil,
            here: .device,
            units: .celsius,
            showInMenuBar: true
        )
    }
}

/// Amount being typed, shared by every converter widget and the menu bar panel.
public struct PadState: Codable, Equatable, Sendable {
    public var text: String
    public var currency: String
    /// Amount of `text`, kept for the budget widget's "+" button; zeroed once added.
    public var lastInput: Double

    public init(text: String = "", currency: String, lastInput: Double = 0) {
        self.text = text; self.currency = currency; self.lastInput = lastInput
    }
}
