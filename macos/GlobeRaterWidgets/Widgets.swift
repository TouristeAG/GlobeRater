import GlobeKit
import SwiftUI
import WidgetKit

private let todayURL = URL(string: "globerater://today")!
private let travelURL = URL(string: "globerater://travel")!

private struct ConverterEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SnapshotEntry
    let withBudget: Bool
    var body: some View {
        ConverterView(snapshot: entry.snapshot, family: family, withBudget: withBudget, accent: entry.accent, interactive: true)
    }
}

private struct BudgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SnapshotEntry
    var body: some View {
        BudgetView(snapshot: entry.snapshot, family: family, style: entry.budgetStyle, accent: entry.accent, interactive: true)
    }
}

private struct WeatherEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SnapshotEntry
    var body: some View { WeatherView(snapshot: entry.snapshot, family: family, accent: entry.accent) }
}

private extension View {
    func widgetChrome(_ url: URL) -> some View {
        containerBackground(.background, for: .widget).widgetURL(url)
    }
}

struct ConverterWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "converter", intent: AccentConfiguration.self, provider: ConverterProvider()) { entry in
            ConverterEntryView(entry: entry, withBudget: false).widgetChrome(todayURL)
        }
        .configurationDisplayName(Text(verbatim: L("group_converter")))
        .description(Text(verbatim: L("currency_converter_widget_description")))
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

struct ConverterBudgetWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "converterBudget", intent: AccentConfiguration.self, provider: ConverterProvider()) { entry in
            ConverterEntryView(entry: entry, withBudget: true).widgetChrome(todayURL)
        }
        .configurationDisplayName(Text(verbatim: L("mac_converter_budget")))
        .description(Text(verbatim: L("currency_converter_expanded_budget_widget_description")))
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

struct BudgetWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "budget", intent: BudgetConfiguration.self, provider: BudgetProvider()) { entry in
            BudgetEntryView(entry: entry).widgetChrome(todayURL)
        }
        .configurationDisplayName(Text(verbatim: L("group_budget")))
        .description(Text(verbatim: L("budget_tracker_widget_description")))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct TravelClockWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "travelClock", intent: AccentConfiguration.self, provider: ClockProvider()) { entry in
            ClockView(snapshot: entry.snapshot, accent: entry.accent).widgetChrome(travelURL)
        }
        .configurationDisplayName(Text(verbatim: L("w_clock")))
        .description(Text(verbatim: L("travel_clock_widget_description")))
        .supportedFamilies([.systemMedium])
    }
}

struct WeatherWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "weather", intent: AccentConfiguration.self, provider: WeatherProvider()) { entry in
            WeatherEntryView(entry: entry).widgetChrome(travelURL)
        }
        .configurationDisplayName(Text(verbatim: L("w_weather")))
        .description(Text(verbatim: L("w_weather_desc")))
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}
