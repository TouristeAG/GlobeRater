import AppKit
import GlobeKit
import Observation

/// State of the app window and the menu bar panel. Reloads whenever the store changes,
/// including changes made by the widgets in the other process.
@MainActor
@Observable
final class AppModel {
    let store: SharedStore
    private(set) var snapshot: WidgetSnapshot
    private(set) var lastRatesError: RatesError?
    private(set) var isRefreshing = false
    private var scheduler: NSBackgroundActivityScheduler?
    private let location = LocationProvider()
    private(set) var locationDenied = false

    var settings: AppSettings { snapshot.settings }
    var budget: BudgetSummary { snapshot.budget }
    var pad: PadState { snapshot.pad }

    init(store: SharedStore = .shared) {
        self.store = store
        self.snapshot = WidgetSnapshot.load(store, now: .now)
        observeChanges()
        scheduleRefresh()
        Task {
            await updateLocation()
            await refresh(force: false)
        }
    }

    /// Reads the Mac's position (only when "here" follows the Mac), at launch and when the menu bar panel opens.
    func updateLocation() async {
        guard settings.here == .device else { return }
        let place = await location.currentPlace()
        locationDenied = location.isDenied
        guard let place, place != store.lastDevicePlace else { return }
        store.lastDevicePlace = place
        await refresh(force: false)
    }

    func reload() { snapshot = WidgetSnapshot.load(store, now: .now) }

    // MARK: Keypad and budget

    func setAmount(typed: String) { PadActions.setTyped(typed, store: store); reload() }
    func selectCurrency(_ code: String) { PadActions.selectCurrency(code, store: store); reload() }
    func conversions() -> [Conversion] { snapshot.conversions(limit: 5) }

    @discardableResult
    func addPadToBudget() -> Bool {
        defer { reload() }
        return PadActions.addPadToBudget(store: store, now: .now)
    }

    @discardableResult
    func addExpense(typed: String, currency: String) -> Bool {
        defer { reload() }
        return PadActions.addTyped(typed, currency: currency, store: store, now: .now)
    }

    func resetBudgetNow() {
        try? store.ledger.resetNow(at: .now)
        store.changed()
        reload()
    }

    // MARK: Settings

    func update(_ change: (inout AppSettings) -> Void) {
        var s = store.settings
        change(&s)
        store.settings = s
        reload()
    }

    // MARK: Refresh

    /// The widgets and the app read the same last known position.
    var refresher: Refresher {
        let store = self.store
        return Refresher(store: store, devicePlace: { store.lastDevicePlace })
    }

    func refresh(force: Bool) async {
        guard !isRefreshing else { return }
        isRefreshing = true
        defer { isRefreshing = false; reload() }
        do {
            try await refresher.refreshRates(force: force, now: .now)
            lastRatesError = nil
        } catch let error as RatesError {
            lastRatesError = error
        } catch {
            lastRatesError = .network
        }
        await refresher.refreshWeather(force: force, now: .now)
    }

    /// Checks a key typed in the settings by downloading rates with it. Always asks the server,
    /// even while another refresh is running, so a bad key is never reported as valid.
    func verifyKey() async -> RatesError? {
        do {
            try await refresher.refreshRates(force: true, now: .now)
            lastRatesError = nil
        } catch let error as RatesError {
            lastRatesError = error
        } catch {
            lastRatesError = .network
        }
        reload()
        return lastRatesError
    }

    private func scheduleRefresh() {
        let scheduler = NSBackgroundActivityScheduler(identifier: "ch.tageoa.globerater.refresh")
        scheduler.interval = 3600
        scheduler.repeats = true
        scheduler.qualityOfService = .utility
        scheduler.schedule { [weak self] completion in
            Task { @MainActor in
                await self?.refresh(force: false)
                completion(.finished)
            }
        }
        self.scheduler = scheduler
    }

    private func observeChanges() {
        let observer = Unmanaged.passUnretained(self).toOpaque()
        CFNotificationCenterAddObserver(
            CFNotificationCenterGetDarwinNotifyCenter(), observer,
            { _, observer, _, _, _ in
                guard let observer else { return }
                let model = Unmanaged<AppModel>.fromOpaque(observer).takeUnretainedValue()
                Task { @MainActor in model.reload() }
            },
            SharedStore.changedNotification as CFString, nil, .deliverImmediately)
    }
}

extension RatesError {
    var message: String {
        switch self {
        case .invalidKey: L("err_invalid_key")
        case .keyNotAllowed: L("err_key_not_allowed")
        case .quota: L("err_quota")
        case .service(let code): L("err_service", code)
        case .empty: L("err_empty")
        case .network: L("mac_err_network")
        }
    }
}
