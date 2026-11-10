import GlobeKit
import SwiftUI
import WidgetKit

/// Weather now, today and tomorrow, at home and here. Large adds tomorrow's details.
struct WeatherView: View {
    let snapshot: WidgetSnapshot
    let family: WidgetFamily
    let accent: AccentChoice

    private var rows: [(role: String, weather: PlaceWeather)] {
        [(L("place_home"), snapshot.home), (L("place_here"), snapshot.here)].compactMap { role, w in w.map { (role, $0) } }
    }

    var body: some View {
        if rows.isEmpty {
            Text(verbatim: L("travel_set_home_hint")).font(.callout).foregroundStyle(.secondary)
        } else {
            VStack(spacing: 8) {
                ForEach(rows, id: \.role) { row in
                    VStack(spacing: 6) {
                        now(role: row.role, w: row.weather)
                        if family == .systemLarge { tomorrow(row.weather) }
                    }
                    .padding(10)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(.quaternary))
                }
            }
        }
    }

    private func now(role: String, w: PlaceWeather) -> some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: w.label).font(.headline).lineLimit(1)
                Text(verbatim: L("place_role_label", role, L(WeatherCodes.descriptionKey(code: w.currentCode))))
                    .font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer(minLength: 4)
            Image(systemName: WeatherCodes.symbol(code: w.currentCode, isDay: w.isDay))
                .symbolRenderingMode(.multicolor).font(.title2)
            Text(verbatim: WeatherCodes.temperature(w.currentTemp))
                .font(.system(size: 30, weight: .semibold, design: .rounded)).monospacedDigit()
            VStack(alignment: .trailing, spacing: 2) {
                Text(verbatim: "↑\(WeatherCodes.temperature(w.today?.max)) ↓\(WeatherCodes.temperature(w.today?.min))")
                Text(verbatim: L("widget_tomorrow", WeatherCodes.temperature(w.tomorrow?.max))).foregroundStyle(.secondary)
            }
            .font(.caption.weight(.medium)).monospacedDigit().lineLimit(1)
        }
    }

    private func tomorrow(_ w: PlaceWeather) -> some View {
        HStack(spacing: 6) {
            Image(systemName: WeatherCodes.symbol(code: w.tomorrow?.code, isDay: true)).symbolRenderingMode(.multicolor)
            Text(verbatim: L("widget_tomorrow", L(WeatherCodes.descriptionKey(code: w.tomorrow?.code))))
            if let chance = w.tomorrow?.precipitationChance { Text(verbatim: L("widget_rain", chance)).foregroundStyle(.secondary) }
            Spacer(minLength: 0)
            Text(verbatim: "↑\(WeatherCodes.temperature(w.tomorrow?.max)) ↓\(WeatherCodes.temperature(w.tomorrow?.min))").monospacedDigit()
        }
        .font(.caption)
    }
}
