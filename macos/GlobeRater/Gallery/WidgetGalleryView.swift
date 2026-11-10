import GlobeKit
import SwiftUI
import WidgetKit

/// All widgets drawn with sample data at their real macOS sizes, plus how to add them.
struct WidgetGalleryView: View {
    var snapshot: WidgetSnapshot = .sample

    private struct Item: Identifiable {
        let id: String
        let title: String
        let description: String
        let family: WidgetFamily
        let view: AnyView
    }

    private var groups: [(String, [Item])] {
        let s = snapshot
        return [
            (L("group_converter"), [
                Item(id: "c-m", title: L("group_converter"), description: L("w_compact_desc"), family: .systemMedium,
                     view: AnyView(ConverterView(snapshot: s, family: .systemMedium, withBudget: false, accent: .system, interactive: false))),
                Item(id: "c-l", title: L("group_converter"), description: L("w_tall_desc"), family: .systemLarge,
                     view: AnyView(ConverterView(snapshot: s, family: .systemLarge, withBudget: false, accent: .system, interactive: false))),
                Item(id: "cb-m", title: L("mac_converter_budget"), description: L("w_compact_budget_desc"), family: .systemMedium,
                     view: AnyView(ConverterView(snapshot: s, family: .systemMedium, withBudget: true, accent: .system, interactive: false))),
                Item(id: "cb-l", title: L("mac_converter_budget"), description: L("w_tall_budget_desc"), family: .systemLarge,
                     view: AnyView(ConverterView(snapshot: s, family: .systemLarge, withBudget: true, accent: .system, interactive: false))),
            ]),
            (L("group_budget"), [
                Item(id: "b-s", title: L("group_budget"), description: L("w_vertical_desc"), family: .systemSmall,
                     view: AnyView(BudgetView(snapshot: s, family: .systemSmall, style: .fill, accent: .system, interactive: false))),
                Item(id: "b-r", title: L("group_budget"), description: L("mac_ring_desc"), family: .systemSmall,
                     view: AnyView(BudgetView(snapshot: s, family: .systemSmall, style: .ring, accent: .system, interactive: false))),
                Item(id: "b-m", title: L("group_budget"), description: L("w_horizontal_desc"), family: .systemMedium,
                     view: AnyView(BudgetView(snapshot: s, family: .systemMedium, style: .fill, accent: .system, interactive: false))),
            ]),
            (L("group_travel"), [
                Item(id: "t-c", title: L("w_clock"), description: L("w_clock_desc"), family: .systemMedium,
                     view: AnyView(ClockView(snapshot: s, accent: .system))),
                Item(id: "t-w", title: L("w_weather"), description: L("w_weather_desc"), family: .systemMedium,
                     view: AnyView(WeatherView(snapshot: s, family: .systemMedium, accent: .system))),
                Item(id: "t-wl", title: L("w_weather"), description: L("w_weather_desc"), family: .systemLarge,
                     view: AnyView(WeatherView(snapshot: s, family: .systemLarge, accent: .system))),
            ]),
        ]
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                ForEach(groups, id: \.0) { title, items in
                    VStack(alignment: .leading, spacing: 12) {
                        Text(verbatim: title).font(.title2.weight(.semibold))
                        FlowLayout(spacing: 20) {
                            ForEach(items) { card($0) }
                        }
                    }
                }
                Label {
                    Text(verbatim: L("mac_widgets_howto"))
                } icon: {
                    Image(systemName: "info.circle")
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: 10).fill(.quaternary.opacity(0.5)))
            }
            .padding(24)
        }
    }

    private func card(_ item: Item) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            item.view
                .padding(16)
                .frame(width: Self.size(item.family).width, height: Self.size(item.family).height)
                .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(.background.secondary))
                .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(.separator))
            Text(verbatim: item.title).font(.headline)
            Text(verbatim: item.description).font(.caption).foregroundStyle(.secondary)
                .frame(width: Self.size(item.family).width, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    static func size(_ family: WidgetFamily) -> CGSize {
        switch family {
        case .systemSmall: CGSize(width: 170, height: 170)
        case .systemLarge: CGSize(width: 364, height: 382)
        default: CGSize(width: 364, height: 170)
        }
    }
}
