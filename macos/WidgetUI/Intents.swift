import AppIntents
import GlobeKit
import WidgetKit

struct AccentConfiguration: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "mac_accent"
    @Parameter(title: "mac_accent", default: .system) var accent: AccentChoice
    init() {}
}

struct BudgetConfiguration: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "group_budget"
    @Parameter(title: "mac_accent", default: .system) var accent: AccentChoice
    @Parameter(title: "mac_style", default: .fill) var style: BudgetStyle
    init() {}
}

struct KeyIntent: AppIntent {
    static let title: LocalizedStringResource = "mac_key"
    static let isDiscoverable = false
    @Parameter(title: "mac_key") var token: String
    init() {}
    init(_ token: String) { self.token = token }
    func perform() async throws -> some IntentResult {
        PadActions.key(token, store: .shared)
        return .result()
    }
}

struct SelectCurrencyIntent: AppIntent {
    static let title: LocalizedStringResource = "change_currency"
    static let isDiscoverable = false
    @Parameter(title: "change_currency") var code: String
    init() {}
    init(_ code: String) { self.code = code }
    func perform() async throws -> some IntentResult {
        PadActions.selectCurrency(code, store: .shared)
        return .result()
    }
}

struct AddPadToBudgetIntent: AppIntent {
    static let title: LocalizedStringResource = "mac_add_to_budget"
    static let isDiscoverable = false
    init() {}
    func perform() async throws -> some IntentResult {
        PadActions.addPadToBudget(store: .shared, now: .now)
        return .result()
    }
}

struct AddLastInputIntent: AppIntent {
    static let title: LocalizedStringResource = "mac_add_to_budget"
    static let isDiscoverable = false
    init() {}
    func perform() async throws -> some IntentResult {
        PadActions.addLastInput(store: .shared, now: .now)
        return .result()
    }
}
