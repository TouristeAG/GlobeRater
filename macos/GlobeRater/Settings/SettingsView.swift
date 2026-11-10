import GlobeKit
import SwiftUI

struct SettingsView: View {
    var body: some View {
        TabView {
            RatesSettings().tabItem { Label(L("rates_title"), systemImage: "dollarsign.arrow.circlepath") }
            BudgetSettingsView().tabItem { Label(L("budget_title"), systemImage: "chart.bar.fill") }
            TravelSettings().tabItem { Label(L("travel_title"), systemImage: "airplane") }
            GeneralSettings().tabItem { Label(L("mac_settings_general"), systemImage: "gearshape") }
        }
        .frame(width: 520)
        .fixedSize(horizontal: false, vertical: true)
    }
}

extension AppModel {
    /// Binding to one setting; every change is saved and shared with the widgets.
    func binding<T>(_ path: WritableKeyPath<AppSettings, T>) -> Binding<T> {
        Binding(get: { self.settings[keyPath: path] }, set: { value in self.update { $0[keyPath: path] = value } })
    }

    var allCurrencies: [String] {
        Set(Rates.sample.usd.keys).union(snapshot.rates.usd.keys).sorted()
    }
}

/// Picker over every known currency.
struct CurrencyPicker: View {
    let title: String
    @Binding var code: String
    let all: [String]

    var body: some View {
        Picker(selection: $code) {
            ForEach(all, id: \.self) { Text(verbatim: "\($0) \(Money.symbol(for: $0))").tag($0) }
        } label: { Text(verbatim: title) }
    }
}

/// Left-aligned explanation under a settings section.
struct SettingsFooter: View {
    let key: String
    var body: some View {
        Text(verbatim: L(key)).foregroundStyle(.secondary).frame(maxWidth: .infinity, alignment: .leading)
    }
}
