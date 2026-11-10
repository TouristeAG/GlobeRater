import GlobeKit
import SwiftUI

struct RatesSettings: View {
    @Environment(AppModel.self) private var model
    @State private var key = ""
    @State private var checking = false
    @State private var result: String?
    @State private var ok = false

    var body: some View {
        Form {
            Section {
                SecureField(text: $key, prompt: Text(verbatim: "0123abcd…")) { Text(verbatim: L("api_key_label")) }
                    .onSubmit(verify)
                HStack {
                    Link(L("api_key_get"), destination: URL(string: "https://openexchangerates.org/signup/free")!)
                    Spacer()
                    if checking { ProgressView().controlSize(.small) }
                    if let result {
                        Label(result, systemImage: ok ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                            .foregroundStyle(ok ? .green : .orange)
                    }
                    Button(L("mac_verify_key"), action: verify).disabled(key.trimmingCharacters(in: .whitespaces).isEmpty || checking)
                }
                Picker(L("update_every"), selection: model.binding(\.rateIntervalDays)) {
                    Text(verbatim: L("span_day")).tag(1)
                    Text(verbatim: L("span_3_days")).tag(3)
                    Text(verbatim: L("span_week")).tag(7)
                }
            } footer: {
                SettingsFooter(key: "rates_subtitle")
            }
            CurrencyListSection(path: \.inputCurrencies, max: AppSettings.maxInputCurrencies,
                                header: "keypad_currencies", footer: "keypad_currencies_hint")
            CurrencyListSection(path: \.outputCurrencies, max: AppSettings.maxOutputCurrencies,
                                header: "converted_to", footer: "converted_to_hint")
        }
        .formStyle(.grouped)
        .onAppear { key = model.settings.apiKey }
    }

    private func verify() {
        let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines)
        model.update { $0.apiKey = trimmed }
        guard !trimmed.isEmpty else { return }
        checking = true
        result = nil
        Task {
            let error = await model.verifyKey()
            checking = false
            ok = error == nil
            result = error?.message ?? L("mac_key_ok")
        }
    }
}

/// 1 to `max` currencies: a picker per row, − to remove one (never the last), + to add one.
struct CurrencyListSection: View {
    @Environment(AppModel.self) private var model
    let path: WritableKeyPath<AppSettings, [String]>
    let max: Int
    let header: String
    let footer: String

    var body: some View {
        let list = model.settings[keyPath: path]
        Section {
            ForEach(list.indices, id: \.self) { i in
                HStack {
                    CurrencyPicker(title: "\(i + 1)", code: currencyBinding(i), all: model.allCurrencies)
                    Button {
                        model.removeCurrency(path, at: i)
                    } label: {
                        Image(systemName: "minus.circle.fill").foregroundStyle(.secondary)
                    }
                    .buttonStyle(.borderless)
                    .disabled(list.count <= 1)
                    .help(L("mac_remove"))
                    .accessibilityLabel(Text(verbatim: "\(L("mac_remove")) \(list[i])"))
                }
                // Row ids must differ between the two lists, or the grouped Form mixes up their rows.
                .id("\(header)-\(i)")
            }
            if list.count < max {
                Button {
                    model.addCurrency(path)
                } label: {
                    Label(L("action_add"), systemImage: "plus.circle.fill")
                }
                .buttonStyle(.borderless)
                .id("\(header)-add")
            }
        } header: { Text(verbatim: L(header)) } footer: { SettingsFooter(key: footer) }
    }

    private func currencyBinding(_ i: Int) -> Binding<String> {
        Binding(
            get: { let list = model.settings[keyPath: path]; return i < list.count ? list[i] : "" },
            set: { code in model.setCurrency(path, at: i, to: code) })
    }
}
