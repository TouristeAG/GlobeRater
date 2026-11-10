import GlobeKit
import WidgetKit

struct SnapshotEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot
    var accent: AccentChoice = .system
    var budgetStyle: BudgetStyle = .fill
}

private func makeRefresher() -> Refresher {
    Refresher(store: .shared, devicePlace: { SharedStore.shared.lastDevicePlace })
}

private func load(_ date: Date = .now) -> WidgetSnapshot { WidgetSnapshot.load(.shared, now: date) }

/// Converters: refresh stale rates, then redraw at the next budget reset or rates refresh.
struct ConverterProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> SnapshotEntry { SnapshotEntry(date: .now, snapshot: .sample) }

    func snapshot(for configuration: AccentConfiguration, in context: Context) async -> SnapshotEntry {
        SnapshotEntry(date: .now, snapshot: context.isPreview ? .sample : load(), accent: configuration.accent)
    }

    func timeline(for configuration: AccentConfiguration, in context: Context) async -> Timeline<SnapshotEntry> {
        let now = Date.now
        let refresher = makeRefresher()
        if refresher.ratesAreStale(now: now) { _ = try? await refresher.refreshRates(force: false, now: now) }
        let snapshot = load(now)
        let ratesDue = now.addingTimeInterval(Double(max(snapshot.settings.rateIntervalDays, 1)) * 86_400)
        return Timeline(entries: [SnapshotEntry(date: now, snapshot: snapshot, accent: configuration.accent)],
                        policy: .after(min(snapshot.budget.nextReset, ratesDue)))
    }
}

/// Budget: a second entry at the next reset so the bar empties right on time.
struct BudgetProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> SnapshotEntry { SnapshotEntry(date: .now, snapshot: .sample) }

    func snapshot(for configuration: BudgetConfiguration, in context: Context) async -> SnapshotEntry {
        SnapshotEntry(date: .now, snapshot: context.isPreview ? .sample : load(),
                      accent: configuration.accent, budgetStyle: configuration.style)
    }

    func timeline(for configuration: BudgetConfiguration, in context: Context) async -> Timeline<SnapshotEntry> {
        let now = Date.now
        let current = load(now)
        let reset = current.budget.nextReset
        return Timeline(entries: [
            SnapshotEntry(date: now, snapshot: current, accent: configuration.accent, budgetStyle: configuration.style),
            SnapshotEntry(date: reset, snapshot: load(reset), accent: configuration.accent, budgetStyle: configuration.style),
        ], policy: .atEnd)
    }
}

/// Clock: the time ticks by itself; one entry per hour only updates the day/night tiles.
struct ClockProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> SnapshotEntry { SnapshotEntry(date: .now, snapshot: .sample) }

    func snapshot(for configuration: AccentConfiguration, in context: Context) async -> SnapshotEntry {
        SnapshotEntry(date: .now, snapshot: context.isPreview ? .sample : load(), accent: configuration.accent)
    }

    func timeline(for configuration: AccentConfiguration, in context: Context) async -> Timeline<SnapshotEntry> {
        let now = Date.now
        let base = load(now)
        let hour = Calendar.current.dateInterval(of: .hour, for: now)?.start ?? now
        var entries = [SnapshotEntry(date: now, snapshot: base, accent: configuration.accent)]
        for h in 1...24 {
            let date = hour.addingTimeInterval(Double(h) * 3600)
            var snapshot = base
            snapshot.now = date
            entries.append(SnapshotEntry(date: date, snapshot: snapshot, accent: configuration.accent))
        }
        return Timeline(entries: entries, policy: .atEnd)
    }
}

/// Weather: refresh when older than 3 h.
struct WeatherProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> SnapshotEntry { SnapshotEntry(date: .now, snapshot: .sample) }

    func snapshot(for configuration: AccentConfiguration, in context: Context) async -> SnapshotEntry {
        SnapshotEntry(date: .now, snapshot: context.isPreview ? .sample : load(), accent: configuration.accent)
    }

    func timeline(for configuration: AccentConfiguration, in context: Context) async -> Timeline<SnapshotEntry> {
        let now = Date.now
        let refresher = makeRefresher()
        if refresher.weatherIsStale(now: now) { _ = await refresher.refreshWeather(force: false, now: now) }
        return Timeline(entries: [SnapshotEntry(date: now, snapshot: load(now), accent: configuration.accent)],
                        policy: .after(now.addingTimeInterval(Refresher.weatherMaxAge)))
    }
}
