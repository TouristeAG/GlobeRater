import Foundation
import Security
#if canImport(WidgetKit)
import WidgetKit
#endif

/// Settings, rates, keypad and weather shared by the app and the widget extension through the App Group.
public final class SharedStore: @unchecked Sendable {
    public static let changedNotification = "ch.tageoa.globerater.changed"

    /// Store of the App Group named by the `GRAppGroup` Info.plist key. Without that entitlement
    /// (unsigned or ad-hoc build) it falls back to the process's own defaults and Application Support,
    /// so the app runs but does not share data with the widgets.
    public static let shared: SharedStore = {
        let group = appGroup(declared: Bundle.main.object(forInfoDictionaryKey: "GRAppGroup") as? String,
                             entitled: entitledGroups())
        if let group,
           let container = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group),
           let defaults = UserDefaults(suiteName: group) {
            return SharedStore(defaults: defaults, directory: container)
        }
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("GlobeRater", isDirectory: true)
        try? FileManager.default.createDirectory(at: support, withIntermediateDirectories: true)
        return SharedStore(defaults: .standard, directory: support)
    }()

    /// The App Group declared in Info.plist, only if the running binary is really entitled to it
    /// (on macOS `containerURL` answers even for groups the app may not use).
    static func appGroup(declared: String?, entitled: [String]?) -> String? {
        guard let declared, !declared.isEmpty, !declared.hasPrefix("."), !declared.contains("$("),
              entitled?.contains(declared) == true else { return nil }
        return declared
    }

    private static func entitledGroups() -> [String]? {
        guard let task = SecTaskCreateFromSelf(nil) else { return nil }
        return SecTaskCopyValueForEntitlement(task, "com.apple.security.application-groups" as CFString, nil) as? [String]
    }

    private let defaults: UserDefaults
    private let directory: URL
    private let notify: Bool
    public let ledger: BudgetLedger

    public init(defaults: UserDefaults, directory: URL, notify: Bool = true) {
        self.defaults = defaults
        self.directory = directory
        self.notify = notify
        self.ledger = BudgetLedger(fileURL: directory.appendingPathComponent("ledger.json"))
    }

    public var settings: AppSettings {
        get {
            if let s: AppSettings = load("settings") { return s }
            let s = AppSettings.defaults(now: Date())
            save(s, "settings")   // first read: keep the budget anchor stable
            return s
        }
        set { save(newValue, "settings"); changed() }
    }

    /// Applies `change` and saves only if it really changed something; returns whether it did.
    /// SwiftUI writes bindings back with unchanged values (e.g. `MenuBarExtra(isInserted:)` on every
    /// scene update): saving those would notify, reload the views and loop forever.
    @discardableResult
    public func updateSettings(_ change: (inout AppSettings) -> Void) -> Bool {
        let old = settings
        var new = old
        change(&new)
        guard new != old else { return false }
        settings = new
        return true
    }

    public var rates: Rates {
        get { load("rates") ?? .sample }
        set { save(newValue, "rates"); changed() }
    }

    public var pad: PadState {
        get { load("pad") ?? PadState(currency: settings.inputCurrencies.first ?? "USD") }
        set { save(newValue, "pad"); changed() }
    }

    public func weather(_ slot: WeatherSlot) -> PlaceWeather? { load("weather.\(slot.rawValue)") }

    public func setWeather(_ weather: PlaceWeather?, for slot: WeatherSlot) {
        save(weather, "weather.\(slot.rawValue)")
        changed()
    }

    /// Last known position of the Mac, written by the app and read by the widget extension.
    public var lastDevicePlace: Place? {
        get { load("lastDevicePlace") }
        set { save(newValue, "lastDevicePlace") }
    }

    /// Tells the other process (and the widgets) that something changed.
    public func changed() {
        guard notify else { return }
        CFNotificationCenterPostNotification(
            CFNotificationCenterGetDarwinNotifyCenter(),
            CFNotificationName(Self.changedNotification as CFString), nil, nil, true)
        #if canImport(WidgetKit)
        WidgetCenter.shared.reloadAllTimelines()
        #endif
    }

    func load<T: Decodable>(_ key: String) -> T? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }

    func save<T: Encodable>(_ value: T?, _ key: String) {
        if let value, let data = try? JSONEncoder().encode(value) {
            defaults.set(data, forKey: key)
        } else {
            defaults.removeObject(forKey: key)
        }
    }
}
