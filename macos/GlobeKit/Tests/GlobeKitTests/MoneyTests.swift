import Foundation
import Testing
@testable import GlobeKit

private let us = Locale(identifier: "en_US")

@Test func moneyFormatting() {
    #expect(Money.format(1234.5, currency: "EUR", locale: us) == "1,234.50 EUR")
    #expect(Money.format(15000.6, currency: "JPY", locale: us) == "15,001 JPY")
    #expect(Money.format(0, currency: "CHF", locale: us) == "0.00 CHF")
    #expect(Money.format(1_500_000, currency: "IDR", locale: us) == "1,500,000 IDR")
    #expect(Money.format(1_234_567.8, currency: "EUR", locale: us) == "1,234,568 EUR")
}

@Test func currencyDecimalsAndSymbols() {
    #expect(Money.decimals(for: "JPY") == 0)
    #expect(Money.decimals(for: "EUR") == 2)
    #expect(Money.symbol(for: "EUR") == "€")
    #expect(Money.symbol(for: "XYZ") == "X")
}
