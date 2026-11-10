import Foundation

/// Keypad text shared by the converter widgets and the menu bar panel.
/// The stored text always uses "," as decimal mark, like the Android app.
public enum KeypadInput {
    public static let maxIntegerDigits = 10

    public static func append(_ current: String, _ token: String) -> String {
        let base = current == "0" ? "" : current
        if token == "," || token == "." {
            if base.contains(",") { return base }
            return base.isEmpty ? "0," : base + ","
        }
        guard !token.isEmpty, token.allSatisfy(\.isASCIIDigit) else { return base }
        if let comma = base.firstIndex(of: ",") {
            let decimals = base.distance(from: comma, to: base.endIndex) - 1
            return (token.count > 1 || decimals >= 2) ? base : base + token
        }
        let next = String((base + token).drop(while: { $0 == "0" }))
        if next.isEmpty { return base }
        return next.count > maxIntegerDigits ? base : next
    }

    public static func backspace(_ current: String) -> String {
        current.isEmpty || current == "0" ? "" : String(current.dropLast())
    }

    public static func parse(_ text: String) -> Double {
        Double(text.replacingOccurrences(of: ",", with: ".")) ?? 0
    }

    /// Text shown to the user, with the locale's separators. `grouped: false` is for editable
    /// text fields (no thousands separators, empty stays empty).
    public static func display(_ text: String, grouped: Bool = true, locale: Locale = .current) -> String {
        let f = NumberFormatter()
        f.locale = locale
        f.numberStyle = .decimal
        if !grouped { return text.replacingOccurrences(of: ",", with: f.decimalSeparator) }
        if text.isEmpty { return "0" }
        let parts = text.split(separator: ",", maxSplits: 1, omittingEmptySubsequences: false)
        let intPart = parts[0].isEmpty ? "0" : String(parts[0])
        var groups: [String] = []
        var rest = Substring(intPart)
        while rest.count > 3 {
            groups.insert(String(rest.suffix(3)), at: 0)
            rest = rest.dropLast(3)
        }
        groups.insert(String(rest), at: 0)
        let grouped = groups.joined(separator: f.groupingSeparator)
        return parts.count == 2 ? grouped + f.decimalSeparator + parts[1] : grouped
    }

    /// Turns free keyboard typing into stored keypad text. When both "," and "." appear, the last
    /// one is the decimal mark. A lone mark that is the locale's thousands separator ("1,000" in
    /// English, "1.000" in German) is ignored; any other mark is the decimal one.
    public static func sanitize(_ typed: String, locale: Locale = .current) -> String {
        let marks = typed.filter { $0 == "," || $0 == "." }
        let decimalMark: Character?
        if Set(marks).count == 2 {
            decimalMark = marks.last
        } else if let mark = marks.first {
            let f = NumberFormatter()
            f.locale = locale
            f.numberStyle = .decimal
            let isGrouping = f.groupingSeparator == String(mark) && f.decimalSeparator != String(mark)
            decimalMark = isGrouping ? nil : mark
        } else {
            decimalMark = nil
        }
        return typed.reduce("") { text, ch in
            if ch.isASCIIDigit { return append(text, String(ch)) }
            if ch == decimalMark { return append(text, ",") }
            return text
        }
    }
}

extension Character {
    var isASCIIDigit: Bool { isASCII && isNumber }
}
