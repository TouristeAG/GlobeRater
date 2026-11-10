import Foundation

public struct OpenMeteoError: Error, Equatable, Sendable { public let status: Int }

/// Open-Meteo forecast and geocoding. Free, no key; only the coordinates of chosen places are sent.
public struct OpenMeteoClient: Sendable {
    private let http: HTTPClient

    public init(http: HTTPClient = URLSession.shared) { self.http = http }

    private struct Forecast: Decodable {
        struct Current: Decodable { var temperature_2m: Double?; var weather_code: Int?; var is_day: Int? }
        struct Daily: Decodable {
            var time: [String]?
            var weather_code: [Int?]?
            var temperature_2m_max: [Double?]?
            var temperature_2m_min: [Double?]?
            var precipitation_probability_max: [Double?]?
        }
        var timezone: String?
        var current: Current?
        var daily: Daily?
    }

    private struct Search: Decodable {
        struct Result: Decodable {
            var name: String?; var latitude: Double?; var longitude: Double?
            var country: String?; var admin1: String?; var timezone: String?
        }
        var results: [Result]?
    }

    public func forecast(for place: Place, unit: TemperatureUnit, now: Date) async throws -> PlaceWeather {
        var c = URLComponents(string: "https://api.open-meteo.com/v1/forecast")!
        c.queryItems = [
            .init(name: "latitude", value: String(place.latitude)),
            .init(name: "longitude", value: String(place.longitude)),
            .init(name: "current", value: "temperature_2m,weather_code,is_day"),
            .init(name: "daily", value: "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"),
            .init(name: "forecast_days", value: "3"),
            .init(name: "temperature_unit", value: unit.rawValue),
            .init(name: "timezone", value: "auto"),
        ]
        let (data, code) = try await http.get(c.url!)
        guard code == 200 else { throw OpenMeteoError(status: code) }
        let f = try JSONDecoder().decode(Forecast.self, from: data)
        func day(_ i: Int) -> DayForecast? {
            guard let d = f.daily, let time = d.time, i < time.count else { return nil }
            return DayForecast(
                code: d.weather_code?[safe: i] ?? nil,
                min: d.temperature_2m_min?[safe: i] ?? nil,
                max: d.temperature_2m_max?[safe: i] ?? nil,
                precipitationChance: (d.precipitation_probability_max?[safe: i] ?? nil).map { min(max(Int($0), 0), 100) })
        }
        return PlaceWeather(
            label: place.name, timeZoneID: f.timezone ?? place.timeZoneID,
            currentTemp: f.current?.temperature_2m, currentCode: f.current?.weather_code,
            isDay: f.current?.is_day != 0, today: day(0), tomorrow: day(1), fetchedAt: now, unit: unit)
    }

    public func search(_ query: String, language: String) async throws -> [Place] {
        var c = URLComponents(string: "https://geocoding-api.open-meteo.com/v1/search")!
        c.queryItems = [
            .init(name: "name", value: query.trimmingCharacters(in: .whitespacesAndNewlines)),
            .init(name: "count", value: "5"),
            .init(name: "language", value: language),
        ]
        let (data, code) = try await http.get(c.url!)
        guard code == 200 else { throw OpenMeteoError(status: code) }
        return (try JSONDecoder().decode(Search.self, from: data).results ?? []).compactMap { r in
            guard let lat = r.latitude, let lon = r.longitude, let name = r.name else { return nil }
            var parts: [String] = []
            for p in [r.name, r.admin1, r.country].compactMap({ $0 }) where !parts.contains(p) { parts.append(p) }
            return Place(name: name, label: parts.joined(separator: ", "), latitude: lat, longitude: lon, timeZoneID: r.timezone)
        }
    }
}

extension Array {
    subscript(safe index: Int) -> Element? { indices.contains(index) ? self[index] : nil }
}
