import Foundation

/// What the keypad keys and "+" buttons do, shared by the widget intents, the app and the menu bar panel.
public enum PadActions {
    /// Tokens: "0"…"9", "000", "," or ".", "C" (clear), "<" (backspace).
    public static func key(_ token: String, store: SharedStore) {
        var pad = store.pad
        switch token {
        case "C": pad.text = ""
        case "<": pad.text = KeypadInput.backspace(pad.text)
        default: pad.text = KeypadInput.append(pad.text, token)
        }
        pad.lastInput = KeypadInput.parse(pad.text)
        store.pad = pad
    }

    /// Replaces the amount with free keyboard typing (app window, menu bar panel).
    public static func setTyped(_ typed: String, store: SharedStore) {
        var pad = store.pad
        pad.text = KeypadInput.sanitize(typed)
        pad.lastInput = KeypadInput.parse(pad.text)
        store.pad = pad
    }

    public static func selectCurrency(_ code: String, store: SharedStore) {
        var pad = store.pad
        pad.currency = code
        store.pad = pad
    }

    /// Removes a keypad currency; if the keypad was on it, moves to the first one left (amount kept).
    public static func removeInputCurrency(at index: Int, store: SharedStore) {
        store.updateSettings { $0.removeInputCurrency(at: index) }
        keepCurrencyInList(store: store)
    }

    /// After the keypad currencies changed: if the keypad's currency is no longer one of them,
    /// moves it to the first one (the amount is kept).
    public static func keepCurrencyInList(store: SharedStore) {
        let inputs = store.settings.inputCurrencies
        if !inputs.contains(store.pad.currency), let first = inputs.first {
            selectCurrency(first, store: store)
        }
    }

    /// Converter "+": adds the typed amount to the budget and clears the keypad.
    @discardableResult
    public static func addPadToBudget(store: SharedStore, now: Date) -> Bool {
        var pad = store.pad
        guard add(KeypadInput.parse(pad.text), currency: pad.currency, store: store, now: now) else { return false }
        pad.text = ""
        pad.lastInput = 0
        store.pad = pad
        return true
    }

    /// Budget widget "+": adds what was last typed in a converter, once.
    @discardableResult
    public static func addLastInput(store: SharedStore, now: Date) -> Bool {
        var pad = store.pad
        guard add(pad.lastInput, currency: pad.currency, store: store, now: now) else { return false }
        pad.lastInput = 0
        store.pad = pad
        return true
    }

    /// "Add an expense" sheet.
    @discardableResult
    public static func addTyped(_ typed: String, currency: String, store: SharedStore, now: Date) -> Bool {
        guard add(KeypadInput.parse(KeypadInput.sanitize(typed)), currency: currency, store: store, now: now) else { return false }
        store.changed()
        return true
    }

    private static func add(_ amount: Double, currency: String, store: SharedStore, now: Date) -> Bool {
        let budget = store.settings.budget
        guard amount > 0, budget.enabled else { return false }
        do { try store.ledger.add(amount, currency: currency, at: now, budget: budget) } catch { return false }
        return true
    }
}
