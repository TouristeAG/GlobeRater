import GlobeKit
import SwiftUI

/// "Home & here": both clocks with the weather, and the time difference.
struct TravelSection: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        let s = model.snapshot
        Section(L("section_home_here")) {
            TimelineView(.everyMinute) { context in
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 12) {
                        PlaceCard(role: L("place_home"), place: s.homeClock, weather: s.home, now: context.date)
                        PlaceCard(role: L("place_here"), place: s.hereClock, weather: s.here, now: context.date)
                    }
                    HStack {
                        if let home = s.homeClock {
                            Text(verbatim: offsetText(TimeOffset.minutes(home: home.zone, here: s.hereClock.zone, at: context.date)))
                                .foregroundStyle(.secondary)
                        } else {
                            Text(verbatim: L("travel_set_home_hint")).foregroundStyle(.secondary)
                            Spacer()
                            SettingsLink { Text(verbatim: L("travel_set_home")) }
                        }
                    }
                    .font(.callout)
                }
                .padding(.vertical, 4)
            }
        }
    }

    private func offsetText(_ minutes: Int) -> String {
        if minutes == 0 { return L("offset_same") }
        let a = abs(minutes)
        let amount = a % 60 == 0 ? L("offset_hours", a / 60) : L("offset_hours_minutes", a / 60, a % 60)
        return L(minutes > 0 ? "offset_ahead" : "offset_behind", amount)
    }
}

struct PlaceCard: View {
    let role: String
    let place: ClockPlace?
    let weather: PlaceWeather?
    let now: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(verbatim: L("place_role_label", role, place?.name ?? "—"))
                .font(.subheadline.weight(.semibold)).foregroundStyle(.secondary).lineLimit(1)
            Text(now, style: .time)
                .font(.system(size: 34, weight: .semibold, design: .rounded)).monospacedDigit()
                .environment(\.timeZone, place?.zone ?? .current)
                .opacity(place == nil ? 0.3 : 1)
            if let weather {
                HStack(spacing: 6) {
                    Image(systemName: WeatherCodes.symbol(code: weather.currentCode, isDay: weather.isDay))
                        .symbolRenderingMode(.multicolor)
                    Text(verbatim: WeatherCodes.temperature(weather.currentTemp)).font(.title3.weight(.semibold))
                    Text(verbatim: L(WeatherCodes.descriptionKey(code: weather.currentCode)))
                        .foregroundStyle(.secondary).lineLimit(1)
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(.quaternary.opacity(0.6)))
    }
}
