import Foundation
import Testing
@testable import GlobeKit

@Test func weatherSymbols() {
    #expect(WeatherCodes.symbol(code: 0, isDay: true) == "sun.max.fill")
    #expect(WeatherCodes.symbol(code: 0, isDay: false) == "moon.stars.fill")
    #expect(WeatherCodes.symbol(code: 2, isDay: false) == "cloud.moon.fill")
    #expect(WeatherCodes.symbol(code: 82, isDay: true) == "cloud.rain.fill")
    #expect(WeatherCodes.symbol(code: 86, isDay: true) == "cloud.snow.fill")
    #expect(WeatherCodes.symbol(code: 99, isDay: true) == "cloud.bolt.rain.fill")
    #expect(WeatherCodes.symbol(code: nil, isDay: true) == "cloud.fill")
}

@Test func weatherDescriptionsAndTemperatures() {
    #expect(WeatherCodes.descriptionKey(code: 1) == "wx_mostly_clear")
    #expect(WeatherCodes.descriptionKey(code: 66) == "wx_freezing_rain")
    #expect(WeatherCodes.descriptionKey(code: 42) == "wx_unknown")
    #expect(WeatherCodes.temperature(9.5) == "10°")
    #expect(WeatherCodes.temperature(-0.4) == "0°")
    #expect(WeatherCodes.temperature(nil) == "--°")
}

@Test func offsets() {
    let t = Date(timeIntervalSince1970: 1_768_478_400) // 2026-01-15 12:00 UTC
    let zurich = TimeZone(identifier: "Europe/Zurich")!, bangkok = TimeZone(identifier: "Asia/Bangkok")!
    let kolkata = TimeZone(identifier: "Asia/Kolkata")!
    #expect(TimeOffset.minutes(home: zurich, here: bangkok, at: t) == 360)
    #expect(TimeOffset.minutes(home: bangkok, here: zurich, at: t) == -360)
    #expect(TimeOffset.minutes(home: zurich, here: kolkata, at: t) == 270)
    #expect(TimeOffset.city(fromTimeZoneID: "America/New_York") == "New York")
    #expect(TimeOffset.isNight(in: zurich, at: t) == false)        // 13:00 in Zurich
    #expect(TimeOffset.isNight(in: bangkok, at: t) == false)       // 19:00 in Bangkok
    #expect(TimeOffset.isNight(in: TimeZone(identifier: "Pacific/Auckland")!, at: t) == true) // 01:00
}
