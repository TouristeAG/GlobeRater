import Foundation

public enum WeatherSlot: String, Codable, CaseIterable, Sendable {
    case home, here
}

public struct DayForecast: Codable, Equatable, Sendable {
    public var code: Int?
    public var min: Double?
    public var max: Double?
    public var precipitationChance: Int?

    public init(code: Int?, min: Double?, max: Double?, precipitationChance: Int?) {
        self.code = code; self.min = min; self.max = max; self.precipitationChance = precipitationChance
    }
}

public struct PlaceWeather: Codable, Equatable, Sendable {
    public var label: String
    public var timeZoneID: String?
    public var currentTemp: Double?
    public var currentCode: Int?
    public var isDay: Bool
    public var today: DayForecast?
    public var tomorrow: DayForecast?
    public var fetchedAt: Date
    public var unit: TemperatureUnit

    public init(label: String, timeZoneID: String?, currentTemp: Double?, currentCode: Int?, isDay: Bool,
                today: DayForecast?, tomorrow: DayForecast?, fetchedAt: Date, unit: TemperatureUnit) {
        self.label = label; self.timeZoneID = timeZoneID; self.currentTemp = currentTemp; self.currentCode = currentCode
        self.isDay = isDay; self.today = today; self.tomorrow = tomorrow; self.fetchedAt = fetchedAt; self.unit = unit
    }
}

/// WMO weather codes → SF Symbol and description key (same groups as the Android app).
public enum WeatherCodes {
    public static func symbol(code: Int?, isDay: Bool) -> String {
        switch code {
        case 0: isDay ? "sun.max.fill" : "moon.stars.fill"
        case 1, 2: isDay ? "cloud.sun.fill" : "cloud.moon.fill"
        case 3: "cloud.fill"
        case 45, 48: "cloud.fog.fill"
        case 51, 53, 55, 56, 57: "cloud.drizzle.fill"
        case 61, 63, 65, 66, 67, 80, 81, 82: "cloud.rain.fill"
        case 71, 73, 75, 77, 85, 86: "cloud.snow.fill"
        case 95, 96, 99: "cloud.bolt.rain.fill"
        default: "cloud.fill"
        }
    }

    public static func descriptionKey(code: Int?) -> String {
        switch code {
        case 0: "wx_clear"
        case 1: "wx_mostly_clear"
        case 2: "wx_partly_cloudy"
        case 3: "wx_overcast"
        case 45, 48: "wx_fog"
        case 51, 53, 55: "wx_drizzle"
        case 56, 57: "wx_freezing_drizzle"
        case 61, 63, 65: "wx_rain"
        case 66, 67: "wx_freezing_rain"
        case 71, 73, 75, 77: "wx_snow"
        case 80, 81, 82: "wx_showers"
        case 85, 86: "wx_snow_showers"
        case 95, 96, 99: "wx_thunderstorm"
        default: "wx_unknown"
        }
    }

    public static func temperature(_ value: Double?) -> String {
        guard let value else { return "--°" }
        return "\(Int(value.rounded()))°"
    }
}

public enum TimeOffset {
    /// Minutes "here" is ahead of home (negative = behind).
    public static func minutes(home: TimeZone, here: TimeZone, at date: Date) -> Int {
        (here.secondsFromGMT(for: date) - home.secondsFromGMT(for: date)) / 60
    }

    public static func city(fromTimeZoneID id: String) -> String {
        (id.split(separator: "/").last.map(String.init) ?? id).replacingOccurrences(of: "_", with: " ")
    }

    /// Night outside 06:00–19:59 local time.
    public static func isNight(in zone: TimeZone, at date: Date) -> Bool {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone
        return !(6...19).contains(calendar.component(.hour, from: date))
    }
}
