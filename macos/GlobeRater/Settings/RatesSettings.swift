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
            Section {
                ForEach(0..<3, id: \.self) { i in
                    CurrencyPicker(title: "\(i + 1)", code: currencyBinding(\.inputCurrencies, i), all: model.allCurrencies)
                        .id("input-\(i)")
                }
            } header: { Text(verbatim: L("keypad_currencies")) } footer: { SettingsFooter(key: "keypad_currencies_hint") }
            Section {
                ForEach(0..<5, id: \.self) { i in
                    CurrencyPicker(title: "\(i + 1)", code: currencyBinding(\.outputCurrencies, i), all: model.allCurrencies)
                        .id("output-\(i)")
                }
            } header: { Text(verbatim: L("converted_to")) } footer: { SettingsFooter(key: "converted_to_hint") }
        }
        .formStyle(.grouped)
        .onAppear { key = model.settings.apiKey }
    }

    private func currencyBinding(_ path: WritableKeyPath<AppSettings, [String]>, _ i: Int) -> Binding<String> {
        Binding(
            get: { let list = model.settings[keyPath: path]; return i < list.count ? list[i] : "" },
            set: { code in
                model.update { s in
                    var list = s[keyPath: path]
                    while list.count <= i { list.append(code) }
                    list[i] = code
                    s[keyPath: path] = list
                }
            })
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
