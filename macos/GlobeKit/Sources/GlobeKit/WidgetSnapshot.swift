import Foundation

public struct ClockPlace: Equatable, Sendable {
    public var name: String
    public var zone: TimeZone
    public init(name: String, zone: TimeZone) { self.name = name; self.zone = zone }
}

public struct Conversion: Equatable, Sendable {
    public var code: String
    /// nil when the rate is unknown.
    public var amount: Double?
}

/// Everything a widget (or the app's gallery) needs to draw itself, read once from the store.
public struct WidgetSnapshot: Sendable {
    public var settings: AppSettings
    public var rates: Rates
    public var pad: PadState
    public var budget: BudgetSummary
    public var home: PlaceWeather?
    public var here: PlaceWeather?
    public var homeClock: ClockPlace?
    public var hereClock: ClockPlace
    public var now: Date

    public var amount: Double { KeypadInput.parse(pad.text) }

    /// Output currencies other than the one being typed, in the user's order.
    public func conversions(limit: Int) -> [Conversion] {
        settings.outputCurrencies
            .filter { $0 != pad.currency }
            .prefix(limit)
            .map { Conversion(code: $0, amount: rates.convert(amount, from: pad.currency, to: $0)) }
    }

    public static func load(_ store: SharedStore, now: Date) -> WidgetSnapshot {
        let settings = store.settings, rates = store.rates
        let home = store.weather(.home), here = store.weather(.here)
        return WidgetSnapshot(
            settings: settings, rates: rates, pad: store.pad,
            budget: BudgetSummary.make(ledger: store.ledger, budget: settings.budget, rates: rates, now: now),
            home: home, here: here,
            homeClock: settings.home.flatMap { clock($0, fallbackZone: home?.timeZoneID) },
            hereClock: hereClock(settings.here, device: store.lastDevicePlace, weather: here),
            now: now)
    }

    private static func clock(_ place: Place, fallbackZone: String?) -> ClockPlace? {
        guard let id = place.timeZoneID ?? fallbackZone, let zone = TimeZone(identifier: id) else { return nil }
        return ClockPlace(name: place.name, zone: zone)
    }

    private static func hereClock(_ here: HereSetting, device: Place?, weather: PlaceWeather?) -> ClockPlace {
        if case .fixed(let place) = here, let c = clock(place, fallbackZone: weather?.timeZoneID) { return c }
        let zone = TimeZone.current
        return ClockPlace(name: device?.name ?? TimeOffset.city(fromTimeZoneID: zone.identifier), zone: zone)
    }

    /// Sample data for previews and the app's widget gallery.
    public static let sample: WidgetSnapshot = {
        let now = Date()
        var settings = AppSettings.defaults(now: now)
        settings.home = Place(name: "Geneva", label: "Geneva, Switzerland", latitude: 46.2, longitude: 6.15, timeZoneID: "Europe/Zurich")
        let bangkok = Place(name: "Bangkok", label: "Bangkok, Thailand", latitude: 13.75, longitude: 100.5, timeZoneID: "Asia/Bangkok")
        settings.here = .fixed(bangkok)
        let budget = BudgetSummary(
            spent: 1240, limit: settings.budget.displayLimit, currency: "USD",
            periodStart: BudgetPeriod.currentStart(anchor: now, refreshHour: 0, span: .monthly, now: now),
            nextReset: BudgetPeriod.nextStart(anchor: now, refreshHour: 0, span: .monthly, now: now))
        return WidgetSnapshot(
            settings: settings, rates: .sample, pad: PadState(text: "100", currency: "USD", lastInput: 100),
            budget: budget,
            home: PlaceWeather(label: "Geneva", timeZoneID: "Europe/Zurich", currentTemp: 9, currentCode: 2, isDay: true,
                               today: DayForecast(code: 2, min: 4, max: 11, precipitationChance: 20),
                               tomorrow: DayForecast(code: 61, min: 5, max: 9, precipitationChance: 70), fetchedAt: now, unit: .celsius),
            here: PlaceWeather(label: "Bangkok", timeZoneID: "Asia/Bangkok", currentTemp: 31, currentCode: 1, isDay: true,
                               today: DayForecast(code: 1, min: 25, max: 33, precipitationChance: 10),
                               tomorrow: DayForecast(code: 80, min: 26, max: 32, precipitationChance: 60), fetchedAt: now, unit: .celsius),
            homeClock: ClockPlace(name: "Geneva", zone: TimeZone(identifier: "Europe/Zurich")!),
            hereClock: ClockPlace(name: "Bangkok", zone: TimeZone(identifier: "Asia/Bangkok")!),
            now: now)
    }()
}
