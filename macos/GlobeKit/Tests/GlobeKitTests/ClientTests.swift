import Foundation
import Testing
@testable import GlobeKit

private let geneva = Place(name: "Geneva", label: "Geneva, Switzerland", latitude: 46.2, longitude: 6.15, timeZoneID: nil)

@Test func decodesLatestRates() async throws {
    let http = StubHTTP(); http.on("https://openexchangerates.org/api/latest.json", .fixture("latest", 200))
    let now = Date(timeIntervalSince1970: 1_000)
    let rates = try await ExchangeRateClient(http: http).latest(apiKey: " key ", now: now)
    #expect(rates.usd["EUR"] == 0.865 && rates.usd["USD"] == 1 && rates.updatedAt == now)
    #expect(http.calls.first?.query?.contains("app_id=key") == true)
}

@Test(arguments: [(401, RatesError.invalidKey), (403, .keyNotAllowed), (429, .quota), (500, .service(500))])
func mapsHTTPErrors(code: Int, expected: RatesError) async {
    let http = StubHTTP(); http.on("https://openexchangerates.org", .body("{}", code))
    await #expect(throws: expected) { try await ExchangeRateClient(http: http).latest(apiKey: "k", now: .now) }
}

@Test func emptyRatesAndNetworkErrors() async {
    let http = StubHTTP(); http.on("https://openexchangerates.org", .body(#"{"rates":{}}"#, 200))
    await #expect(throws: RatesError.empty) { try await ExchangeRateClient(http: http).latest(apiKey: "k", now: .now) }
    await #expect(throws: RatesError.network) { try await ExchangeRateClient(http: StubHTTP()).latest(apiKey: "k", now: .now) }
}

@Test func decodesForecast() async throws {
    let http = StubHTTP(); http.on("https://api.open-meteo.com/v1/forecast", .fixture("forecast", 200))
    let w = try await OpenMeteoClient(http: http).forecast(for: geneva, unit: .fahrenheit, now: .now)
    #expect(w.currentTemp == 9.5 && w.currentCode == 61 && !w.isDay)
    #expect(w.today == DayForecast(code: 63, min: 8.3, max: 15.7, precipitationChance: 100))
    #expect(w.tomorrow?.max == 17.1)
    #expect(w.timeZoneID == "Europe/Zurich" && w.label == "Geneva" && w.unit == .fahrenheit)
    let query = http.calls.first?.query ?? ""
    #expect(query.contains("temperature_unit=fahrenheit") && query.contains("forecast_days=3") && query.contains("timezone=auto"))
}

@Test func searchBuildsLabels() async throws {
    let http = StubHTTP(); http.on("https://geocoding-api.open-meteo.com/v1/search", .fixture("search", 200))
    let places = try await OpenMeteoClient(http: http).search("Geneva", language: "en")
    #expect(places.count == 2)
    #expect(places[0].label == "Geneva, Canton of Geneva, Switzerland")
    #expect(places[0].timeZoneID == "Europe/Zurich" && places[0].name == "Geneva")
}

@Test func searchLabelsDropDuplicatesAndEmptyResults() async throws {
    let http = StubHTTP()
    http.on("https://geocoding-api.open-meteo.com/v1/search?name=Monaco",
            .body(#"{"results":[{"name":"Monaco","latitude":43.7,"longitude":7.4,"country":"Monaco","admin1":"Monaco"}]}"#, 200))
    http.on("https://geocoding-api.open-meteo.com/v1/search?name=Zzz", .body(#"{"generationtime_ms":0.1}"#, 200))
    let client = OpenMeteoClient(http: http)
    #expect(try await client.search("Monaco", language: "en").first?.label == "Monaco")
    #expect(try await client.search("Zzz", language: "en").isEmpty)
}
