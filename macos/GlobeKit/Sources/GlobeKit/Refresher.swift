import Foundation

/// Decides when rates and weather are out of date, downloads them and stores them.
/// Called by the app (launch, hourly, ↻) and by the widgets' timelines.
public struct Refresher: Sendable {
    public static let weatherMaxAge: TimeInterval = 3 * 3600

    private let store: SharedStore
    private let ratesClient: ExchangeRateClient
    private let weatherClient: OpenMeteoClient
    private let devicePlace: @Sendable () async -> Place?

    public init(store: SharedStore, rates: ExchangeRateClient = ExchangeRateClient(),
                weather: OpenMeteoClient = OpenMeteoClient(),
                devicePlace: @escaping @Sendable () async -> Place?) {
        self.store = store
        self.ratesClient = rates
        self.weatherClient = weather
        self.devicePlace = devicePlace
    }

    public func ratesAreStale(now: Date) -> Bool {
        let settings = store.settings
        guard !settings.apiKey.isEmpty else { return false }
        guard let updated = store.rates.updatedAt else { return true }
        return now.timeIntervalSince(updated) >= Double(max(settings.rateIntervalDays, 1)) * 86_400
    }

    /// Returns true when new rates were stored. Without an API key nothing is downloaded.
    @discardableResult
    public func refreshRates(force: Bool, now: Date) async throws -> Bool {
        let key = store.settings.apiKey
        guard !key.isEmpty, force || ratesAreStale(now: now) else { return false }
        store.rates = try await ratesClient.latest(apiKey: key, now: now)
        return true
    }

    public func weatherIsStale(now: Date) -> Bool {
        let settings = store.settings
        let here: Place? = switch settings.here {
        case .fixed(let p): p
        case .device: store.lastDevicePlace
        }
        return [(WeatherSlot.home, settings.home), (.here, here)].contains { slot, place in
            guard let place else { return false }
            return isStale(store.weather(slot), for: place, unit: settings.units, now: now)
        }
    }

    /// Updates both places; a place that is no longer set loses its weather. On a network
    /// failure the previous weather is kept. Returns true if anything was updated.
    @discardableResult
    public func refreshWeather(force: Bool, now: Date) async -> Bool {
        let settings = store.settings
        let here: Place? = switch settings.here {
        case .fixed(let p): p
        case .device: await devicePlace()
        }
        var updated = false
        for (slot, place) in [(WeatherSlot.home, settings.home), (.here, here)] {
            guard let place else {
                if store.weather(slot) != nil { store.setWeather(nil, for: slot) }
                continue
            }
            guard force || isStale(store.weather(slot), for: place, unit: settings.units, now: now) else { continue }
            if let w = try? await weatherClient.forecast(for: place, unit: settings.units, now: now) {
                store.setWeather(w, for: slot)
                updated = true
            }
        }
        return updated
    }

    private func isStale(_ w: PlaceWeather?, for place: Place, unit: TemperatureUnit, now: Date) -> Bool {
        guard let w else { return true }
        return w.unit != unit || w.label != place.name || now.timeIntervalSince(w.fetchedAt) >= Self.weatherMaxAge
    }
}
