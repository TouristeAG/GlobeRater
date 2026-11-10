import GlobeKit
import SwiftUI

struct BudgetSettingsView: View {
    @Environment(AppModel.self) private var model
    @State private var confirmReset = false

    private let spans: [(BudgetSpan, String)] = [(.daily, "span_day"), (.weekly, "span_week"), (.monthly, "span_month"), (.yearly, "span_year")]

    var body: some View {
        Form {
            Section {
                Toggle(L("budget_track"), isOn: model.binding(\.budget.enabled))
            } footer: {
                SettingsFooter(key: "budget_subtitle")
            }
            if model.settings.budget.enabled {
                Section {
                    TextField(value: model.binding(\.budget.amount), format: .number) { Text(verbatim: L("group_budget")) }
                    CurrencyPicker(title: L("budget_currency"), code: model.binding(\.budget.currency), all: model.allCurrencies)
                    Picker(L("budget_per"), selection: model.binding(\.budget.span)) {
                        ForEach(spans, id: \.0) { Text(verbatim: L($0.1)).tag($0.0) }
                    }
                    .pickerStyle(.segmented)
                    Picker(L("budget_widgets_per"), selection: model.binding(\.budget.displaySpan)) {
                        ForEach(spans, id: \.0) { Text(verbatim: L($0.1)).tag($0.0) }
                    }
                    .pickerStyle(.segmented)
                    LabeledContent(L("budget_spent_this_period")) {
                        Text(verbatim: Money.format(model.budget.limit, currency: model.budget.currency)).monospacedDigit()
                    }
                }
                Section {
                    DatePicker(L("budget_period_starts"), selection: model.binding(\.budget.anchor), displayedComponents: .date)
                    Picker(L("mac_reset_hour"), selection: model.binding(\.budget.refreshHour)) {
                        ForEach(0..<24, id: \.self) { h in Text(verbatim: String(format: "%02d:00", h)).tag(h) }
                    }
                    HStack {
                        Text(verbatim: L("budget_resets", Formats.shortDate(model.budget.nextReset))).foregroundStyle(.secondary)
                        Spacer()
                        Button(L("action_reset"), role: .destructive) { confirmReset = true }
                    }
                }
            }
        }
        .formStyle(.grouped)
        .confirmationDialog(L("mac_reset_budget_title"), isPresented: $confirmReset) {
            Button(L("action_reset"), role: .destructive) { model.resetBudgetNow() }
        } message: {
            Text(verbatim: L("mac_reset_budget_text"))
        }
    }
}
