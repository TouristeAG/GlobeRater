import Foundation
import Testing
@testable import GlobeKit

private let now = Date(timeIntervalSince1970: 1_800_000_000)
private let geneva = Place(name: "Geneva", label: "Geneva, Switzerland", latitude: 46.2, longitude: 6.15, timeZoneID: "Europe/Zurich")

private func refresher(_ store: SharedStore, _ http: StubHTTP, device: Place? = nil) -> Refresher {
    Refresher(store: store, rates: ExchangeRateClient(http: http), weather: OpenMeteoClient(http: http), devicePlace: { device })
}

private func weather(fetchedAt: Date, unit: TemperatureUnit = .celsius) -> PlaceWeather {
    PlaceWeather(label: "Geneva", timeZoneID: "Europe/Zurich", currentTemp: 5, currentCode: 0, isDay: true,
                 today: nil, tomorrow: nil, fetchedAt: fetchedAt, unit: unit)
}

@Test func noKeyMeansNoNetwork() async throws {
    let store = makeStore(), http = StubHTTP()
    let r = refresher(store, http)
    #expect(!r.ratesAreStale(now: now))
    #expect(try await r.refreshRates(force: false, now: now) == false)
    #expect(http.calls.isEmpty)
}

@Test func staleAfterInterval() {
    let store = makeStore()
    store.settings.apiKey = "k"
    let r = refresher(store, StubHTTP())
    #expect(r.ratesAreStale(now: now))   // sample rates only
    store.rates = Rates(usd: ["EUR": 0.9], updatedAt: now.addingTimeInterval(-25 * 3600))
    #expect(r.ratesAreStale(now: now))
    store.rates = Rates(usd: ["EUR": 0.9], updatedAt: now.addingTimeInterval(-23 * 3600))
    #expect(!r.ratesAreStale(now: now))
}

@Test func refreshRatesStoresThem() async throws {
    let store = makeStore(), http = StubHTTP()
    store.settings.apiKey = "k"
    http.on("https://openexchangerates.org", .fixture("latest", 200))
    #expect(try await refresher(store, http).refreshRates(force: false, now: now))
    #expect(store.rates.usd["THB"] == 32.4 && store.rates.updatedAt == now)
}

@Test func weatherStaleness() {
    let store = makeStore()
    let r = refresher(store, StubHTTP())
    #expect(!r.weatherIsStale(now: now))          // home unset, here = device but no position yet
    store.settings.home = geneva
    #expect(r.weatherIsStale(now: now))           // missing
    store.setWeather(weather(fetchedAt: now.addingTimeInterval(-3600)), for: .home)
    #expect(!r.weatherIsStale(now: now))
    store.setWeather(weather(fetchedAt: now.addingTimeInterval(-4 * 3600)), for: .home)
    #expect(r.weatherIsStale(now: now))           // older than 3 h
}

@Test func unitChangeMakesWeatherStale() {
    let store = makeStore()
    store.settings.home = geneva
    store.setWeather(weather(fetchedAt: now), for: .home)
    store.settings.units = .fahrenheit
    #expect(refresher(store, StubHTTP()).weatherIsStale(now: now))
}

@Test func refreshWeatherFillsBothSlots() async {
    let store = makeStore(), http = StubHTTP()
    http.on("https://api.open-meteo.com", .fixture("forecast", 200))
    store.settings.home = geneva
    #expect(await refresher(store, http, device: geneva).refreshWeather(force: false, now: now))
    #expect(store.weather(.home)?.currentTemp == 9.5)
    #expect(store.weather(.here)?.label == "Geneva")
}

@Test func networkFailureKeepsOldWeather() async {
    let store = makeStore()
    store.settings.home = geneva
    let old = weather(fetchedAt: now.addingTimeInterval(-5 * 3600))
    store.setWeather(old, for: .home)
    #expect(await refresher(store, StubHTTP()).refreshWeather(force: false, now: now) == false)
    #expect(store.weather(.home) == old)
}

@Test func removedPlaceClearsItsWeather() async {
    let store = makeStore()
    store.setWeather(weather(fetchedAt: now), for: .home)
    _ = await refresher(store, StubHTTP()).refreshWeather(force: true, now: now)
    #expect(store.weather(.home) == nil)
}

@Test func lastDevicePlaceIsShared() {
    let store = makeStore()
    store.lastDevicePlace = geneva
    #expect(store.lastDevicePlace == geneva)
}

@Test func forcedRefreshAlwaysAsksTheServer() async throws {
    let store = makeStore(), http = StubHTTP()
    store.settings.apiKey = "bad"
    store.rates = Rates(usd: ["EUR": 0.9], updatedAt: now)          // fresh, not stale
    http.on("https://openexchangerates.org", .body("{}", 401))
    await #expect(throws: RatesError.invalidKey) { try await refresher(store, http).refreshRates(force: true, now: now) }
}
