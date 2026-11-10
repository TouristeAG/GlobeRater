import Foundation

/// Exchange rates against the US dollar (the free Open Exchange Rates plan only offers USD as base).
public struct Rates: Codable, Equatable, Sendable {
    public var usd: [String: Double]
    /// nil for the built-in sample table.
    public var updatedAt: Date?

    public init(usd: [String: Double], updatedAt: Date?) {
        self.usd = usd
        self.updatedAt = updatedAt
    }

    public var isSample: Bool { updatedAt == nil }

    /// Returns nil when a rate is unknown, so callers never show a fake 1:1 conversion.
    public func convert(_ amount: Double, from: String, to: String) -> Double? {
        if from == to { return amount }
        guard let f = rate(from), let t = rate(to), f > 0 else { return nil }
        return amount / f * t
    }

    private func rate(_ code: String) -> Double? { code == "USD" ? 1 : usd[code] }

    /// Rough rates used before the user adds an API key (same table as Android).
    public static let sample = Rates(usd: [
        "USD": 1.0, "EUR": 0.92, "GBP": 0.79, "CHF": 0.88, "JPY": 150.0, "CAD": 1.37,
        "AUD": 1.53, "NZD": 1.66, "CNY": 7.2, "HKD": 7.8, "SGD": 1.35, "THB": 35.5,
        "INR": 83.0, "IDR": 15800.0, "VND": 25000.0, "KRW": 1350.0, "MXN": 17.0,
        "BRL": 5.0, "ZAR": 18.5, "SEK": 10.6, "NOK": 10.7, "DKK": 6.9, "PLN": 4.0,
        "CZK": 23.0, "HUF": 360.0, "TRY": 32.0, "AED": 3.67, "MAD": 10.0, "EGP": 48.0,
        "PHP": 56.0, "MYR": 4.7,
    ], updatedAt: nil)
}
