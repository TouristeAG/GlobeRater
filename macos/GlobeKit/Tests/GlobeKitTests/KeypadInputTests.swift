import Foundation
import Testing
@testable import GlobeKit

private let fr = Locale(identifier: "fr_FR")
private let us = Locale(identifier: "en_US")
private func type(_ tokens: String...) -> String { tokens.reduce("") { KeypadInput.append($0, $1) } }

@Test func keypadBuildsNumbers() {
    #expect(type("1", "2", "5", "0") == "1250")
    #expect(type("1", "2", ",", "5") == "12,5")
    #expect(type("5", "000") == "5000")
}

@Test func keypadRejectsInvalidInput() {
    #expect(type("1", ",", ",", "2") == "1,2")
    #expect(type("1", ",", "2", "5", "9") == "1,25")
    #expect(type("0", "0") == "")
    #expect(type(",", "5") == "0,5")
    #expect(type("1", ",", "000", "5") == "1,5")
    #expect(type("1234567890", "1") == "1234567890")
    #expect(type("1", ".", "5") == "1,5")
}

@Test func keypadParsesAndDisplays() {
    #expect(KeypadInput.parse("12,5") == 12.5)
    #expect(KeypadInput.parse("") == 0)
    #expect(KeypadInput.backspace("5") == "")
    #expect(KeypadInput.backspace("12,5") == "12,")
    #expect(KeypadInput.display("1234,5", locale: us) == "1,234.5")
    #expect(KeypadInput.display("", locale: us) == "0")
    let sep = groupingSeparator(for: fr)
    #expect(KeypadInput.display("1234,5", locale: fr) == "1\(sep)234,5")
}

@Test func keyboardTypingAcceptsBothDecimalMarks() {
    #expect(KeypadInput.sanitize("12.5") == "12,5")
    #expect(KeypadInput.sanitize("12,5") == "12,5")
    #expect(KeypadInput.sanitize("1 234,50") == "1234,50")
    #expect(KeypadInput.sanitize("1,234.50") == "1,23")   // the first mark is the decimal one
    #expect(KeypadInput.sanitize("abc") == "")
}

private func groupingSeparator(for locale: Locale) -> String {
    let f = NumberFormatter(); f.locale = locale; f.numberStyle = .decimal
    return f.groupingSeparator
}
