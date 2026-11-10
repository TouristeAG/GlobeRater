import GlobeKit
import SwiftUI
import WidgetKit

/// Home time next to local time. `Text(_:style: .time)` keeps ticking without timeline reloads.
struct ClockView: View {
    let snapshot: WidgetSnapshot
    let accent: AccentChoice

    var body: some View {
        HStack(spacing: 8) {
            if let home = snapshot.homeClock {
                tile(role: L("place_home"), place: home)
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    Text(verbatim: L("place_home")).font(.caption.weight(.semibold))
                    Spacer(minLength: 0)
                    Text(verbatim: L("travel_set_home_hint")).font(.caption).foregroundStyle(.secondary)
                }
                .padding(10)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(.quaternary, lineWidth: 1.5))
            }
            tile(role: L("place_here"), place: snapshot.hereClock)
        }
    }

    private func tile(role: String, place: ClockPlace) -> some View {
        let night = TimeOffset.isNight(in: place.zone, at: snapshot.now)
        return VStack(alignment: .leading, spacing: 0) {
            Text(verbatim: L("place_role_label", role, place.name))
                .font(.caption.weight(.semibold)).lineLimit(1)
            Spacer(minLength: 0)
            Text(snapshot.now, style: .time)
                .font(.system(size: 34, weight: .semibold, design: .rounded))
                .monospacedDigit().lineLimit(1).minimumScaleFactor(0.5)
                .environment(\.timeZone, place.zone)
            Text(verbatim: Formats.weekdayDate(snapshot.now, zone: place.zone))
                .font(.caption).foregroundStyle(.secondary)
        }
        .padding(10)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(night ? AnyShapeStyle(accent.color.opacity(0.28)) : AnyShapeStyle(.quaternary))
                .widgetAccentable(night)
        )
    }
}
