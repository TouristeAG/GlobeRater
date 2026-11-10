import Foundation

/// Amount formatting, decimals per currency and currency symbols (ported from WidgetCommon.kt).
public enum Money {
    private static let zeroDecimal: Set<String> = ["JPY", "KRW", "VND", "IDR", "CLP", "ISK", "HUF", "PYG", "UGX", "XAF", "XOF"]

    private static let symbols: [String: String] = [
        "USD": "$", "EUR": "€", "GBP": "£", "JPY": "¥", "CNY": "¥", "CHF": "Fr",
        "CAD": "$", "AUD": "$", "NZD": "$", "SGD": "$", "HKD": "$", "MXN": "$",
        "SEK": "kr", "NOK": "kr", "DKK": "kr", "ISK": "kr", "TRY": "₺", "INR": "₹",
        "BRL": "R$", "ZAR": "R", "KRW": "₩", "THB": "฿", "VND": "₫", "PHP": "₱",
        "ILS": "₪", "RUB": "₽", "UAH": "₴", "PLN": "zł", "CZK": "Kč", "HUF": "Ft",
        "IDR": "Rp", "MYR": "RM", "AED": "د.إ", "EGP": "£", "MAD": "DH", "NGN": "₦",
    ]

    public static func decimals(for currency: String) -> Int { zeroDecimal.contains(currency) ? 0 : 2 }

    public static func symbol(for currency: String) -> String { symbols[currency] ?? String(currency.prefix(1)) }

    /// "1,234.50" in English, "1 234,50" in French. Amounts of a million or more drop their cents.
    public static func number(_ amount: Double, currency: String, locale: Locale = .current) -> String {
        let digits = abs(amount) >= 1_000_000 ? 0 : decimals(for: currency)
        let f = NumberFormatter()
        f.locale = locale
        f.numberStyle = .decimal
        f.roundingMode = .halfUp
        f.minimumFractionDigits = digits
        f.maximumFractionDigits = digits
        return f.string(from: NSNumber(value: amount)) ?? String(amount)
    }

    public static func format(_ amount: Double, currency: String, locale: Locale = .current) -> String {
        "\(number(amount, currency: currency, locale: locale)) \(currency)"
    }
}
